package com.netrunner.network

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import java.util.concurrent.ConcurrentHashMap

data class GitHubAuthResult(
    val user: GitHubUser,
    val stats: GitHubStats,
    val repos: List<GitHubRepo>,
    val token: String?,
)

object GitHubApiClient {
    private val avatarCache = ConcurrentHashMap<String, ImageBitmap>()

    private fun looksLikeToken(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("ghp_") ||
            trimmed.startsWith("github_pat_") ||
            trimmed.startsWith("gho_") ||
            trimmed.startsWith("ghu_") ||
            trimmed.startsWith("ghs_")
    }

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

    suspend fun authenticate(credentialOrUsername: String): Result<GitHubAuthResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleaned = credentialOrUsername.trim().removePrefix("@")
                require(cleaned.isNotEmpty()) { "Enter a GitHub username or personal access token." }

                val isToken = looksLikeToken(cleaned)
                val token = if (isToken) cleaned else null

                val userEndpoint = if (isToken) {
                    "https://api.github.com/user"
                } else {
                    "https://api.github.com/users/${URLEncoder.encode(cleaned, "UTF-8")}"
                }

                val (userCode, userBody) = httpGet(userEndpoint, token)
                if (userCode !in 200..299) {
                    val msg = runCatching { JSONObject(userBody).optString("message") }.getOrNull()
                    throw IllegalStateException(
                        when (userCode) {
                            401 -> "GitHub rejected this token. Check scopes or generate a fresh token."
                            404 -> "GitHub user \"$cleaned\" not found."
                            403 -> "GitHub API rate limit reached. Sign in with a GitHub token to continue."
                            else -> msg?.takeIf { it.isNotBlank() } ?: "GitHub returned HTTP $userCode"
                        }
                    )
                }

                val userJson = JSONObject(userBody)
                val user = GitHubUser.fromJson(userJson)

                val reposEndpoint = if (isToken) {
                    "https://api.github.com/user/repos?sort=updated&per_page=30&affiliation=owner,collaborator"
                } else {
                    "https://api.github.com/users/${URLEncoder.encode(user.login, "UTF-8")}/repos?sort=updated&per_page=30"
                }

                val (reposCode, reposBody) = httpGet(reposEndpoint, token)
                val repos = if (reposCode in 200..299) {
                    val arr = JSONArray(reposBody)
                    (0 until arr.length()).map { GitHubRepo.fromApiJson(arr.getJSONObject(it)) }
                } else {
                    emptyList()
                }

                val stats = buildStats(user, repos)
                GitHubAuthResult(user = user, stats = stats, repos = repos, token = token)
            }
        }

    suspend fun syncLiveUser(login: String, token: String?): Result<GitHubAuthResult> =
        withContext(Dispatchers.IO) {
            if (!token.isNullOrBlank()) {
                authenticate(token)
            } else {
                authenticate(login)
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
