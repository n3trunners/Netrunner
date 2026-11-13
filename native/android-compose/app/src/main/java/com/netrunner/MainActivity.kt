package com.netrunner

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.netrunner.core.RustDaemonBridge
import com.netrunner.model.ActiveTab
import com.netrunner.model.GitHubRepo
import com.netrunner.model.GitHubStats
import com.netrunner.model.GitHubUser
import com.netrunner.model.SettingsModalType
import com.netrunner.model.UserCustomIntegration
import com.netrunner.model.WorkspaceProject
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.BrowseScreen
import com.netrunner.ui.DashboardScreen
import com.netrunner.ui.GitHubAuthDialog
import com.netrunner.ui.HybridArchitectureDialog
import com.netrunner.ui.NewProjectDialog
import com.netrunner.ui.ProfileScreen
import com.netrunner.ui.SettingsDetailDialog
import com.netrunner.ui.SettingsScreen
import com.netrunner.ui.WorkspaceTerminalScreen
import com.netrunner.ui.components.BottomDock
import com.netrunner.ui.components.ToastBanner
import com.netrunner.ui.theme.NetRunnerTheme
import com.netrunner.ui.theme.NetRunnerWallpaperBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private var deepLinkTokenState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RustDaemonBridge.boot(filesDir)
        handleAuthIntent(intent)

        val prefs = getSharedPreferences("netrunner_prefs", Context.MODE_PRIVATE)

        setContent {
            val scope = rememberCoroutineScope()

            // Theme & navigation state
            var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
            var activeTab by remember { mutableStateOf(ActiveTab.HOME) }
            var isSettingsOpen by remember { mutableStateOf(false) }
            var activeSettingsModal by remember { mutableStateOf<SettingsModalType?>(null) }
            var isNewProjectOpen by remember { mutableStateOf(false) }
            var isGitHubAuthOpen by remember { mutableStateOf(false) }
            var isHybridDialogOpen by remember { mutableStateOf(false) }
            var selectedProject by remember { mutableStateOf<WorkspaceProject?>(null) }
            var toastMessage by remember { mutableStateOf<String?>(null) }
            var selectedLanguage by remember {
                mutableStateOf(prefs.getString("selected_language", "English (US)") ?: "English (US)")
            }

            // BYOK token state
            var userApiToken by remember {
                mutableStateOf(prefs.getString("user_api_token", "") ?: "")
            }
            var selectedModel by remember {
                mutableStateOf(prefs.getString("gemini_model", "gemini-4-hydrogen") ?: "gemini-4-hydrogen")
            }

            // GitHub account state
            var currentUser by remember {
                mutableStateOf(
                    prefs.getString("github_user", null)?.let { raw ->
                        runCatching { GitHubUser.fromJson(JSONObject(raw)) }.getOrNull()
                    },
                )
            }
            var userStats by remember {
                mutableStateOf(
                    prefs.getString("github_stats", null)?.let { raw ->
                        runCatching { GitHubStats.fromJson(JSONObject(raw)) }.getOrNull()
                    },
                )
            }
            var userRepos by remember {
                mutableStateOf(GitHubRepo.listFromJson(prefs.getString("github_repos", null)))
            }
            var githubToken by remember {
                mutableStateOf(prefs.getString("github_token", null))
            }
            var isRefreshingStats by remember { mutableStateOf(false) }

            // Projects & custom integrations
            val projects = remember {
                mutableStateListOf<WorkspaceProject>().apply {
                    addAll(WorkspaceProject.listFromJson(prefs.getString("workspace_projects", null)))
                }
            }
            val connectors = remember {
                mutableStateListOf<UserCustomIntegration>().apply {
                    addAll(UserCustomIntegration.listFromJson(prefs.getString("user_connectors", null)))
                }
            }
            val agents = remember {
                mutableStateListOf<UserCustomIntegration>().apply {
                    addAll(UserCustomIntegration.listFromJson(prefs.getString("user_agents", null)))
                }
            }
            val skills = remember {
                mutableStateListOf<UserCustomIntegration>().apply {
                    addAll(UserCustomIntegration.listFromJson(prefs.getString("user_skills", null)))
                }
            }

            fun showToast(msg: String) {
                toastMessage = msg
            }

            LaunchedEffect(toastMessage) {
                if (toastMessage != null) {
                    delay(3200)
                    toastMessage = null
                }
            }

            fun persistProjects() {
                prefs.edit().putString("workspace_projects", WorkspaceProject.listToJson(projects)).apply()
            }

            fun applyGitHubAuth(
                user: GitHubUser,
                stats: GitHubStats,
                repos: List<GitHubRepo>,
                token: String?,
            ) {
                currentUser = user
                userStats = stats
                userRepos = repos
                if (!token.isNullOrBlank()) {
                    githubToken = token
                    prefs.edit().putString("github_token", token).apply()
                }
                prefs.edit()
                    .putString("github_user", user.toJson().toString())
                    .putString("github_stats", stats.toJson().toString())
                    .putString("github_repos", GitHubRepo.listToJson(repos))
                    .apply()
            }

            // Live sync GitHub profile & repos on launch if signed in
            LaunchedEffect(Unit) {
                val user = currentUser
                if (user != null) {
                    GitHubApiClient.syncLiveUser(user.login, githubToken).onSuccess { res ->
                        applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                    }
                }
            }

            // Handle deep link token netrunner://auth?token=...
            LaunchedEffect(deepLinkTokenState) {
                val token = deepLinkTokenState ?: return@LaunchedEffect
                deepLinkTokenState = null
                GitHubApiClient.authenticate(token).onSuccess { res ->
                    applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                    showToast("Signed in as @${res.user.login}!")
                }.onFailure { err ->
                    showToast(err.message ?: "GitHub sign-in failed")
                }
            }

            BackHandler(enabled = selectedProject != null || isSettingsOpen || activeTab != ActiveTab.HOME) {
                when {
                    selectedProject != null -> selectedProject = null
                    isSettingsOpen -> isSettingsOpen = false
                    activeTab != ActiveTab.HOME -> activeTab = ActiveTab.HOME
                }
            }

            NetRunnerTheme(darkTheme = isDark) {
                NetRunnerWallpaperBox {
                    val activeProj = selectedProject
                    if (activeProj != null) {
                        WorkspaceTerminalScreen(
                            project = activeProj,
                            userApiToken = userApiToken,
                            modelId = selectedModel,
                            onOpenTokenSettings = { activeSettingsModal = SettingsModalType.MODELS },
                            onBack = { selectedProject = null },
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val routeKey = when {
                                isSettingsOpen -> "settings"
                                activeTab == ActiveTab.BROWSE -> "browse"
                                activeTab == ActiveTab.PROFILE -> "profile"
                                else -> "home"
                            }

                            Crossfade(
                                targetState = routeKey,
                                animationSpec = tween(220),
                                label = "mainRouteCrossfade",
                            ) { key ->
                                when (key) {
                                    "settings" -> SettingsScreen(
                                        currentUser = currentUser,
                                        selectedLanguage = selectedLanguage,
                                        onBack = { isSettingsOpen = false },
                                        onOpenModal = { activeSettingsModal = it },
                                        onToggleTheme = {
                                            isDark = !isDark
                                            prefs.edit().putBoolean("dark_mode", isDark).apply()
                                            showToast("Switched to ${if (isDark) "Dark" else "Light"} appearance")
                                        },
                                        onOpenGitHubAuth = { isGitHubAuthOpen = true },
                                    )

                                    "browse" -> BrowseScreen(
                                        onBackToHome = { activeTab = ActiveTab.HOME },
                                        onShowToast = ::showToast,
                                    )

                                    "profile" -> ProfileScreen(
                                        currentUser = currentUser,
                                        userStats = userStats,
                                        userRepos = userRepos,
                                        isRefreshing = isRefreshingStats,
                                        onBackToHome = { activeTab = ActiveTab.HOME },
                                        onOpenSettings = { isSettingsOpen = true },
                                        onOpenGitHubAuth = { isGitHubAuthOpen = true },
                                        onSignOut = {
                                            currentUser = null
                                            userStats = null
                                            userRepos = emptyList()
                                            githubToken = null
                                            prefs.edit()
                                                .remove("github_user")
                                                .remove("github_stats")
                                                .remove("github_repos")
                                                .remove("github_token")
                                                .apply()
                                            showToast("Signed out of NetRunner")
                                        },
                                        onRefreshStats = {
                                            val login = currentUser?.login ?: return@ProfileScreen
                                            isRefreshingStats = true
                                            scope.launch {
                                                GitHubApiClient.syncLiveUser(login, githubToken)
                                                    .onSuccess { res ->
                                                        applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                                                        showToast("Live GitHub statistics synchronized!")
                                                    }
                                                    .onFailure {
                                                        showToast("Could not refresh stats from GitHub API")
                                                    }
                                                isRefreshingStats = false
                                            }
                                        },
                                    )

                                    else -> DashboardScreen(
                                        projects = projects,
                                        hasApiToken = userApiToken.isNotBlank(),
                                        selectedModel = selectedModel,
                                        currentUser = currentUser,
                                        userRepos = userRepos,
                                        onOpenNewProjectModal = { isNewProjectOpen = true },
                                        onOpenSettings = { isSettingsOpen = true },
                                        onOpenTokenSettings = { activeSettingsModal = SettingsModalType.MODELS },
                                        onOpenHybridStudio = { isHybridDialogOpen = true },
                                        onOpenGitHubAuth = { isGitHubAuthOpen = true },
                                        onImportGitHubRepo = { repo ->
                                            if (projects.any { it.name == repo.name }) {
                                                showToast("Repository \"${repo.name}\" is already in your workspace.")
                                            } else {
                                                projects.add(
                                                    0,
                                                    WorkspaceProject(
                                                        id = "repo-${repo.id}",
                                                        name = repo.name,
                                                        description = repo.description ?: "Imported from GitHub",
                                                        category = "Fullstack",
                                                        architecture = "fullstack",
                                                        updatedAt = "Imported",
                                                        filesCount = 12,
                                                        repoUrl = repo.htmlUrl,
                                                    ),
                                                )
                                                persistProjects()
                                                showToast("Imported \"${repo.name}\" into workspace!")
                                            }
                                        },
                                        onSelectProject = { selectedProject = it },
                                        onDeleteProject = { id ->
                                            val removed = projects.firstOrNull { it.id == id }
                                            projects.removeAll { it.id == id }
                                            persistProjects()
                                            if (removed != null) showToast("Deleted ${removed.name}")
                                        },
                                        onResetProjects = {
                                            projects.clear()
                                            persistProjects()
                                            showToast("Cleared workspace projects to empty state")
                                        },
                                    )
                                }
                            }

                            // Floating Bottom Navigation Dock (hidden inside Settings, matching website)
                            if (!isSettingsOpen) {
                                BottomDock(
                                    activeTab = activeTab,
                                    onChangeTab = { activeTab = it },
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                )
                            }
                        }
                    }

                    // Toast Notification
                    ToastBanner(
                        message = toastMessage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding(),
                    )

                    // Modals
                    if (isGitHubAuthOpen) {
                        GitHubAuthDialog(
                            onDismiss = { isGitHubAuthOpen = false },
                            onLoginSuccess = { res ->
                                applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                            },
                            onShowToast = ::showToast,
                        )
                    }

                    if (isNewProjectOpen) {
                        NewProjectDialog(
                            onDismiss = { isNewProjectOpen = false },
                            onCreateProject = { name, desc, cat ->
                                val isHybrid = cat == "Hybrid"
                                val created = WorkspaceProject(
                                    id = "proj-${System.currentTimeMillis()}",
                                    name = name,
                                    description = desc.ifBlank {
                                        if (isHybrid) "Kotlin + Jetpack Compose UI with Rust Core Daemon"
                                        else "NetRunner workspace"
                                    },
                                    category = cat,
                                    architecture = if (isHybrid) "hybrid-kotlin-rust" else "fullstack",
                                    updatedAt = "Just now",
                                    filesCount = (4..11).random(),
                                )
                                projects.add(0, created)
                                persistProjects()
                                showToast("Project \"$name\" created!")
                            },
                        )
                    }

                    if (isHybridDialogOpen) {
                        HybridArchitectureDialog(
                            onDismiss = { isHybridDialogOpen = false },
                            onCreateHybridProject = {
                                val created = WorkspaceProject(
                                    id = "hybrid-${System.currentTimeMillis()}",
                                    name = "netrunner-hybrid-core",
                                    description = "Kotlin + Jetpack Compose Android UI with Rust Core Daemon",
                                    category = "Hybrid",
                                    architecture = "hybrid-kotlin-rust",
                                    updatedAt = "Just now",
                                    filesCount = 13,
                                )
                                projects.add(0, created)
                                persistProjects()
                                showToast("Created \"netrunner-hybrid-core\" workspace!")
                            },
                        )
                    }

                    val modalType = activeSettingsModal
                    if (modalType != null) {
                        SettingsDetailDialog(
                            modalType = modalType,
                            currentUser = currentUser,
                            userApiToken = userApiToken,
                            selectedModel = selectedModel,
                            selectedLanguage = selectedLanguage,
                            connectors = connectors,
                            agents = agents,
                            skills = skills,
                            onDismiss = { activeSettingsModal = null },
                            onSaveToken = { token, model ->
                                userApiToken = token
                                selectedModel = model
                                prefs.edit()
                                    .putString("user_api_token", userApiToken)
                                    .putString("gemini_model", selectedModel)
                                    .apply()
                                showToast(
                                    if (token.isNotBlank()) "Personal token & $model saved locally"
                                    else "Model set to $model",
                                )
                            },
                            onClearToken = {
                                userApiToken = ""
                                prefs.edit().remove("user_api_token").apply()
                                showToast("Personal API token removed")
                            },
                            onAddIntegration = { type, name, detail ->
                                val item = UserCustomIntegration(
                                    id = "custom-${System.currentTimeMillis()}",
                                    name = name,
                                    detail = detail,
                                )
                                when (type) {
                                    SettingsModalType.CONNECTORS -> {
                                        connectors.add(item)
                                        prefs.edit().putString("user_connectors", UserCustomIntegration.listToJson(connectors)).apply()
                                        showToast("Connector \"$name\" registered")
                                    }
                                    SettingsModalType.AGENTS -> {
                                        agents.add(item)
                                        prefs.edit().putString("user_agents", UserCustomIntegration.listToJson(agents)).apply()
                                        showToast("Agent \"$name\" initialized")
                                    }
                                    SettingsModalType.SKILLS -> {
                                        skills.add(item)
                                        prefs.edit().putString("user_skills", UserCustomIntegration.listToJson(skills)).apply()
                                        showToast("Skill \"$name\" installed")
                                    }
                                    else -> Unit
                                }
                            },
                            onDeleteIntegration = { type, id ->
                                when (type) {
                                    SettingsModalType.CONNECTORS -> {
                                        connectors.removeAll { it.id == id }
                                        prefs.edit().putString("user_connectors", UserCustomIntegration.listToJson(connectors)).apply()
                                    }
                                    SettingsModalType.AGENTS -> {
                                        agents.removeAll { it.id == id }
                                        prefs.edit().putString("user_agents", UserCustomIntegration.listToJson(agents)).apply()
                                    }
                                    SettingsModalType.SKILLS -> {
                                        skills.removeAll { it.id == id }
                                        prefs.edit().putString("user_skills", UserCustomIntegration.listToJson(skills)).apply()
                                    }
                                    else -> Unit
                                }
                                showToast("Removed item")
                            },
                            onSelectLanguage = { lang ->
                                selectedLanguage = lang
                                prefs.edit().putString("selected_language", lang).apply()
                            },
                            onOpenGitHubAuth = { isGitHubAuthOpen = true },
                            onSignOut = {
                                currentUser = null
                                userStats = null
                                userRepos = emptyList()
                                githubToken = null
                                prefs.edit()
                                    .remove("github_user")
                                    .remove("github_stats")
                                    .remove("github_repos")
                                    .remove("github_token")
                                    .apply()
                                showToast("Signed out of NetRunner")
                            },
                            onShowToast = ::showToast,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "netrunner" && data.host == "auth") {
            val token = data.getQueryParameter("token") ?: data.getQueryParameter("username")
            if (!token.isNullOrBlank()) {
                deepLinkTokenState = token
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            RustDaemonBridge.trimCaches()
        }
    }
}
