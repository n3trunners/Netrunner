package com.netrunner.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

enum class ActiveTab {
    HOME,
    BROWSE,
    PROFILE,
}

enum class SettingsModalType(val title: String) {
    CONNECTORS("Connectors"),
    MODELS("Models & API Token"),
    AGENTS("Agents"),
    SKILLS("Skills"),
    PROFILE_DETAIL("Profile Detail"),
    LANGUAGE("Language"),
    NOTIFICATIONS("Notifications"),
    EDITOR("Editor Preferences"),
    DATA_STORAGE("Data & Storage"),
    SECURITY("Security"),
    ABOUT("About NetRunner"),
}

@Immutable
data class WorkspaceProject(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "Hybrid",
    val architecture: String = "hybrid-kotlin-rust",
    val updatedAt: String = "Just now",
    val status: String = "active",
    val filesCount: Int = 6,
    val repoUrl: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description)
        .put("category", category)
        .put("architecture", architecture)
        .put("updatedAt", updatedAt)
        .put("status", status)
        .put("filesCount", filesCount)
        .apply { if (repoUrl != null) put("repoUrl", repoUrl) }

    companion object {
        fun fromJson(obj: JSONObject): WorkspaceProject = WorkspaceProject(
            id = obj.optString("id", "proj-${System.currentTimeMillis()}"),
            name = obj.optString("name", "untitled"),
            description = obj.optString("description", "NetRunner workspace"),
            category = obj.optString("category", "Hybrid"),
            architecture = obj.optString("architecture", "hybrid-kotlin-rust"),
            updatedAt = obj.optString("updatedAt", "Just now"),
            status = obj.optString("status", "active"),
            filesCount = obj.optInt("filesCount", 6),
            repoUrl = obj.optString("repoUrl").takeIf { it.isNotBlank() },
        )

        fun listToJson(list: List<WorkspaceProject>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(raw: String?): List<WorkspaceProject> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }
    }
}

@Immutable
data class GitHubUser(
    val login: String,
    val name: String?,
    val avatarUrl: String,
    val htmlUrl: String,
    val bio: String?,
    val company: String? = null,
    val location: String? = null,
    val blog: String? = null,
    val twitterUsername: String? = null,
    val publicRepos: Int = 0,
    val publicGists: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("login", login)
        .put("name", name ?: "")
        .put("avatar_url", avatarUrl)
        .put("html_url", htmlUrl)
        .put("bio", bio ?: "")
        .put("company", company ?: "")
        .put("location", location ?: "")
        .put("blog", blog ?: "")
        .put("twitter_username", twitterUsername ?: "")
        .put("public_repos", publicRepos)
        .put("public_gists", publicGists)
        .put("followers", followers)
        .put("following", following)

    companion object {
        fun fromJson(obj: JSONObject): GitHubUser {
            val login = obj.optString("login", "").ifBlank { "developer" }
            return GitHubUser(
                login = login,
                name = obj.optString("name").takeIf { it.isNotBlank() && it != "null" },
                avatarUrl = obj.optString("avatar_url").ifBlank { "https://github.com/$login.png" },
                htmlUrl = obj.optString("html_url").ifBlank { "https://github.com/$login" },
                bio = obj.optString("bio").takeIf { it.isNotBlank() && it != "null" } ?: "NetRunner Developer",
                company = obj.optString("company").takeIf { it.isNotBlank() && it != "null" },
                location = obj.optString("location").takeIf { it.isNotBlank() && it != "null" },
                blog = obj.optString("blog").takeIf { it.isNotBlank() && it != "null" },
                twitterUsername = obj.optString("twitter_username").takeIf { it.isNotBlank() && it != "null" },
                publicRepos = obj.optInt("public_repos", 0),
                publicGists = obj.optInt("public_gists", 0),
                followers = obj.optInt("followers", 0),
                following = obj.optInt("following", 0),
            )
        }
    }
}

@Immutable
data class LanguageStat(
    val language: String,
    val count: Int,
)

@Immutable
data class GitHubStats(
    val totalStars: Int,
    val totalForks: Int,
    val publicRepos: Int,
    val followers: Int,
    val topLanguages: List<LanguageStat>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("totalStars", totalStars)
        .put("totalForks", totalForks)
        .put("publicRepos", publicRepos)
        .put("followers", followers)
        .put(
            "topLanguages",
            JSONArray().apply {
                topLanguages.forEach {
                    put(JSONObject().put("language", it.language).put("count", it.count))
                }
            },
        )

    companion object {
        fun fromJson(obj: JSONObject): GitHubStats {
            val langsArr = obj.optJSONArray("topLanguages") ?: JSONArray()
            val langs = (0 until langsArr.length()).map {
                val item = langsArr.getJSONObject(it)
                LanguageStat(item.optString("language"), item.optInt("count"))
            }
            return GitHubStats(
                totalStars = obj.optInt("totalStars", 0),
                totalForks = obj.optInt("totalForks", 0),
                publicRepos = obj.optInt("publicRepos", 0),
                followers = obj.optInt("followers", 0),
                topLanguages = langs,
            )
        }
    }
}

@Immutable
data class GitHubRepo(
    val id: Long,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val stars: Int,
    val forks: Int,
    val language: String?,
    val updatedAt: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description ?: "")
        .put("html_url", htmlUrl)
        .put("stars", stars)
        .put("forks", forks)
        .put("language", language ?: "")
        .put("updated_at", updatedAt)

    companion object {
        fun fromApiJson(obj: JSONObject): GitHubRepo = GitHubRepo(
            id = obj.optLong("id", 0L),
            name = obj.optString("name", ""),
            description = obj.optString("description").takeIf { it.isNotBlank() && it != "null" },
            htmlUrl = obj.optString("html_url", ""),
            stars = obj.optInt("stargazers_count", obj.optInt("stars", 0)),
            forks = obj.optInt("forks_count", obj.optInt("forks", 0)),
            language = obj.optString("language").takeIf { it.isNotBlank() && it != "null" },
            updatedAt = obj.optString("updated_at", ""),
        )

        fun listToJson(list: List<GitHubRepo>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(raw: String?): List<GitHubRepo> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { fromApiJson(arr.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }
    }
}

@Immutable
data class UserCustomIntegration(
    val id: String,
    val name: String,
    val detail: String,
    val endpointOrKey: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("detail", detail)
        .apply { if (endpointOrKey != null) put("endpointOrKey", endpointOrKey) }

    companion object {
        fun listToJson(list: List<UserCustomIntegration>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(raw: String?): List<UserCustomIntegration> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map {
                    val o = arr.getJSONObject(it)
                    UserCustomIntegration(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        detail = o.optString("detail", "Custom"),
                        endpointOrKey = o.optString("endpointOrKey").takeIf { s -> s.isNotBlank() },
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}

@Immutable
data class SavedApiKeyEntry(
    val id: String,
    val provider: String = "gemini-api",
    val providerLabel: String = "Gemini API",
    val apiKey: String,
    val createdAt: String = "Just now",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("provider", provider)
        .put("providerLabel", providerLabel)
        .put("apiKey", apiKey)
        .put("createdAt", createdAt)

    companion object {
        fun listToJson(list: List<SavedApiKeyEntry>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(raw: String?): List<SavedApiKeyEntry> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).mapNotNull { idx ->
                    val o = arr.getJSONObject(idx)
                    val key = o.optString("apiKey").trim()
                    if (key.isEmpty()) return@mapNotNull null
                    SavedApiKeyEntry(
                        id = o.optString("id").ifBlank { "key-$idx-${System.currentTimeMillis()}" },
                        provider = o.optString("provider", "gemini-api"),
                        providerLabel = o.optString("providerLabel", "Gemini API"),
                        apiKey = key,
                        createdAt = o.optString("createdAt", "Just now"),
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}

