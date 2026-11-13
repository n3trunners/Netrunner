package com.netrunner.network

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.netrunner.app.BuildConfig
import com.netrunner.model.GitHubRepo
import com.netrunner.model.GitHubStats
import com.netrunner.model.GitHubUser
import com.netrunner.model.LanguageStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class GitHubAuthResult(
    val user: GitHubUser,
    val stats: GitHubStats,
    val repos: List<GitHubRepo>,
    val token: String?,
)

object GitHubApiClient {
    private val avatarCache = ConcurrentHashMap<String, ImageBitmap>()

    private fun httpGet(urlString: String, token: String? = null): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "NetRunner-Android")
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer ${token.trim()}")
            }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            code to body
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPostJson(urlString: String, jsonBody: JSONObject): Pair<Int, String> {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 12_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NetRunner-Android")
        }
        return try {
            conn.outputStream.bufferedWriter().use { it.write(jsonBody.toString()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            code to body
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Resolves the GitHub OAuth authorize URL for Android:
     * 1) If BuildConfig.GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET are bundled, uses direct mobile deep-link OAuth (netrunner://auth).
     * 2) If BuildConfig.GITHUB_CLIENT_ID is bundled without secret, routes callback through OAUTH_BASE_URL/auth/callback with android state.
     * 3) Otherwise fetches the configured OAuth URL from the NetRunner server (/api/auth/github/url?platform=android).
     */
    suspend fun resolveAuthorizeUrl(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = BuildConfig.GITHUB_CLIENT_ID.trim()
            val clientSecret = BuildConfig.GITHUB_CLIENT_SECRET.trim()
            val baseUrl = BuildConfig.OAUTH_BASE_URL.trim().trimEnd('/')
            val nonce = UUID.randomUUID().toString().replace("-", "").take(16)

            if (clientId.isNotEmpty()) {
                val redirectUri = if (clientSecret.isNotEmpty()) {
                    "netrunner://auth"
                } else {
                    "$baseUrl/auth/callback"
                }
                val state = "android:$nonce"
                val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
                val encodedScope = URLEncoder.encode("read:user,repo", "UTF-8")
                val encodedState = URLEncoder.encode(state, "UTF-8")
                return@runCatching "https://github.com/login/oauth/authorize?client_id=$clientId&redirect_uri=$encodedRedirect&scope=$encodedScope&allow_signup=true&state=$encodedState"
            }

            require(baseUrl.isNotEmpty()) {
                "GitHub OAuth Client ID is not configured. Provide GITHUB_CLIENT_ID in build config or server environment."
            }

            val (code, body) = httpGet("$baseUrl/api/auth/github/url?platform=android")
            if (code !in 200..299) {
                throw IllegalStateException("Auth server returned HTTP $code")
            }
            val json = JSONObject(body)
            if (!json.optBoolean("configured", false)) {
                throw IllegalStateException(
                    json.optString("message").ifBlank {
                        "GitHub OAuth Client ID is not configured yet."
                    }
                )
            }
            val url = json.optString("url")
            require(url.isNotBlank()) { "Auth server returned an empty GitHub OAuth URL." }
            url
        }
    }

    /**
     * Exchanges an OAuth authorization code (received at netrunner://auth?code=...) for a GitHub access token
     * and signs the user in.
     */
    suspend fun exchangeCodeAndAuthenticate(code: String): Result<GitHubAuthResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clientId = BuildConfig.GITHUB_CLIENT_ID.trim()
                val clientSecret = BuildConfig.GITHUB_CLIENT_SECRET.trim()
                val baseUrl = BuildConfig.OAUTH_BASE_URL.trim().trimEnd('/')

                val accessToken = if (clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
                    val payload = JSONObject()
                        .put("client_id", clientId)
                        .put("client_secret", clientSecret)
                        .put("code", code)
                        .put("redirect_uri", "netrunner://auth")
                    val (status, body) = httpPostJson("https://github.com/login/oauth/access_token", payload)
                    if (status !in 200..299) {
                        throw IllegalStateException("GitHub token exchange failed (HTTP $status)")
                    }
                    val json = JSONObject(body)
                    if (json.has("error")) {
                        throw IllegalStateException(json.optString("error_description", json.optString("error")))
                    }
                    json.optString("access_token").takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("GitHub did not return an access token.")
                } else {
                    val payload = JSONObject()
                        .put("code", code)
                        .put("redirectUri", "netrunner://auth")
                    val (status, body) = httpPostJson("$baseUrl/api/auth/github/exchange", payload)
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (status !in 200..299) {
                        throw IllegalStateException(json?.optString("error")?.ifBlank { null } ?: "Token exchange failed (HTTP $status)")
                    }
                    json?.optString("access_token")?.takeIf { it.isNotBlank() }
                        ?: throw IllegalStateException("Server did not return an access token.")
                }

                authenticateWithToken(accessToken).getOrThrow()
            }
        }

    suspend fun authenticateWithToken(accessToken: String): Result<GitHubAuthResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanToken = accessToken.trim()
                require(cleanToken.isNotEmpty()) { "Missing GitHub access token." }

                val (userCode, userBody) = httpGet("https://api.github.com/user", cleanToken)
                if (userCode !in 200..299) {
                    val msg = runCatching { JSONObject(userBody).optString("message") }.getOrNull()
                    throw IllegalStateException(
                        when (userCode) {
                            401 -> "GitHub authorization expired. Please sign in again."
                            403 -> "GitHub API rate limit reached. Please try again shortly."
                            else -> msg?.takeIf { it.isNotBlank() } ?: "GitHub returned HTTP $userCode"
                        }
                    )
                }

                val userJson = JSONObject(userBody)
                val user = GitHubUser.fromJson(userJson)

                val reposEndpoint =
                    "https://api.github.com/user/repos?sort=updated&per_page=30&affiliation=owner,collaborator"
                val (reposCode, reposBody) = httpGet(reposEndpoint, cleanToken)
                val repos = if (reposCode in 200..299) {
                    val arr = JSONArray(reposBody)
                    (0 until arr.length()).map { GitHubRepo.fromApiJson(arr.getJSONObject(it)) }
                } else {
                    emptyList()
                }

                val stats = buildStats(user, repos)
                GitHubAuthResult(user = user, stats = stats, repos = repos, token = cleanToken)
            }
        }

    suspend fun syncLiveUser(login: String, token: String?): Result<GitHubAuthResult> =
        withContext(Dispatchers.IO) {
            if (!token.isNullOrBlank()) {
                authenticateWithToken(token)
            } else {
                runCatching {
                    val cleanLogin = login.trim().removePrefix("@")
                    require(cleanLogin.isNotEmpty()) { "Missing GitHub login." }
                    val (userCode, userBody) = httpGet("https://api.github.com/users/${URLEncoder.encode(cleanLogin, "UTF-8")}")
                    if (userCode !in 200..299) {
                        throw IllegalStateException("GitHub returned HTTP $userCode")
                    }
                    val user = GitHubUser.fromJson(JSONObject(userBody))
                    val (reposCode, reposBody) = httpGet(
                        "https://api.github.com/users/${URLEncoder.encode(user.login, "UTF-8")}/repos?sort=updated&per_page=30"
                    )
                    val repos = if (reposCode in 200..299) {
                        val arr = JSONArray(reposBody)
                        (0 until arr.length()).map { GitHubRepo.fromApiJson(arr.getJSONObject(it)) }
                    } else {
                        emptyList()
                    }
                    GitHubAuthResult(user = user, stats = buildStats(user, repos), repos = repos, token = null)
                }
            }
        }

    private fun buildStats(user: GitHubUser, repos: List<GitHubRepo>): GitHubStats {
        val totalStars = repos.sumOf { it.stars }
        val totalForks = repos.sumOf { it.forks }
        val langCounts = mutableMapOf<String, Int>()
        repos.forEach { repo ->
            val lang = repo.language
            if (!lang.isNullOrBlank()) {
                langCounts[lang] = (langCounts[lang] ?: 0) + 1
            }
        }
        val topLanguages = langCounts.entries
            .sortedByDescending { it.value }
            .map { LanguageStat(it.key, it.value) }

        return GitHubStats(
            totalStars = totalStars,
            totalForks = totalForks,
            publicRepos = if (user.publicRepos > 0) user.publicRepos else repos.size,
            followers = user.followers,
            topLanguages = topLanguages,
        )
    }

    suspend fun loadAvatar(url: String): ImageBitmap? {
        if (url.isBlank()) return null
        avatarCache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "NetRunner-Android")
                }
                try {
                    if (conn.responseCode in 200..299) {
                        val bmp = BitmapFactory.decodeStream(conn.inputStream)
                        bmp?.asImageBitmap()?.also { avatarCache[url] = it }
                    } else null
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }
}
