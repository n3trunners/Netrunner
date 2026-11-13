package com.netrunner

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.netrunner.ui.NewProjectDialog
import com.netrunner.ui.ProfileScreen
import com.netrunner.ui.SettingsDetailDialog
import com.netrunner.ui.SettingsScreen
import com.netrunner.ui.WorkspaceTerminalScreen
import com.netrunner.ui.components.BottomDock
import com.netrunner.ui.components.ToastBanner
import com.netrunner.ui.theme.NetRunnerEase
import com.netrunner.ui.theme.NetRunnerTheme
import com.netrunner.ui.theme.NetRunnerWallpaperBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private var deepLinkTokenState by mutableStateOf<String?>(null)
    private var deepLinkCodeState by mutableStateOf<String?>(null)
    private var deepLinkErrorState by mutableStateOf<String?>(null)

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
            var isAuthorizingGitHub by remember { mutableStateOf(false) }
            var githubAuthError by remember { mutableStateOf<String?>(null) }
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
                isAuthorizingGitHub = true
                githubAuthError = null
                GitHubApiClient.authenticateWithToken(token).onSuccess { res ->
                    applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                    isAuthorizingGitHub = false
                    isGitHubAuthOpen = false
                    showToast("Signed in as @${res.user.login}!")
                }.onFailure { err ->
                    isAuthorizingGitHub = false
                    val msg = err.message ?: "GitHub sign-in failed"
                    githubAuthError = msg
                    showToast(msg)
                }
            }

            // Handle deep link authorization code netrunner://auth?code=...
            LaunchedEffect(deepLinkCodeState) {
                val code = deepLinkCodeState ?: return@LaunchedEffect
                deepLinkCodeState = null
                isAuthorizingGitHub = true
                githubAuthError = null
                GitHubApiClient.exchangeCodeAndAuthenticate(code).onSuccess { res ->
                    applyGitHubAuth(res.user, res.stats, res.repos, res.token)
                    isAuthorizingGitHub = false
                    isGitHubAuthOpen = false
                    showToast("Signed in as @${res.user.login}!")
                }.onFailure { err ->
                    isAuthorizingGitHub = false
                    val msg = err.message ?: "GitHub authorization failed"
                    githubAuthError = msg
                    showToast(msg)
                }
            }

            // Handle deep link error netrunner://auth?error=...
            LaunchedEffect(deepLinkErrorState) {
                val errMsg = deepLinkErrorState ?: return@LaunchedEffect
                deepLinkErrorState = null
                isAuthorizingGitHub = false
                githubAuthError = errMsg
                showToast(errMsg)
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
                    AnimatedContent(
                        targetState = selectedProject,
                        transitionSpec = {
                            if (targetState != null) {
                                (slideInHorizontally(tween(280, easing = NetRunnerEase)) { it / 6 } +
                                    fadeIn(tween(240)))
                                    .togetherWith(
                                        scaleOut(targetScale = 0.97f, animationSpec = tween(220)) + fadeOut(tween(180)),
                                    )
                            } else {
                                (scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = NetRunnerEase)) +
                                    fadeIn(tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(240, easing = NetRunnerEase)) { it / 6 } +
                                            fadeOut(tween(180)),
                                    )
                            }
                        },
                        label = "workspaceTerminalTransition",
                    ) { activeProj ->
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

                                AnimatedContent(
                                    targetState = routeKey,
                                    transitionSpec = {
                                        when {
                                            targetState == "settings" -> {
                                                (slideInHorizontally(tween(280, easing = NetRunnerEase)) { it / 5 } +
                                                    fadeIn(tween(240, easing = NetRunnerEase)))
                                                    .togetherWith(
                                                        slideOutHorizontally(tween(240, easing = NetRunnerEase)) { -it / 8 } +
                                                            fadeOut(tween(180)),
                                                    )
                                            }
                                            initialState == "settings" -> {
                                                (slideInHorizontally(tween(280, easing = NetRunnerEase)) { -it / 8 } +
                                                    fadeIn(tween(240, easing = NetRunnerEase)))
                                                    .togetherWith(
                                                        slideOutHorizontally(tween(240, easing = NetRunnerEase)) { it / 5 } +
                                                            fadeOut(tween(180)),
                                                    )
                                            }
                                            else -> {
                                                (scaleIn(initialScale = 0.975f, animationSpec = tween(240, easing = NetRunnerEase)) +
                                                    fadeIn(tween(220, easing = NetRunnerEase)))
                                                    .togetherWith(
                                                        scaleOut(targetScale = 0.985f, animationSpec = tween(180)) +
                                                            fadeOut(tween(160)),
                                                    )
                                            }
                                        }
                                    },
                                    label = "mainRouteAnimatedContent",
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
                                            onOpenGitHubAuth = {
                                                githubAuthError = null
                                                isGitHubAuthOpen = true
                                            },
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
                                            onOpenGitHubAuth = {
                                                githubAuthError = null
                                                isGitHubAuthOpen = true
                                            },
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
                                            currentUser = currentUser,
                                            userRepos = userRepos,
                                            onOpenNewProjectModal = { isNewProjectOpen = true },
                                            onOpenSettings = { isSettingsOpen = true },
                                            onOpenGitHubAuth = {
                                                githubAuthError = null
                                                isGitHubAuthOpen = true
                                            },
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

                                // Floating Bottom Navigation Dock (slides smoothly in/out when Settings opens)
                                AnimatedVisibility(
                                    visible = !isSettingsOpen,
                                    enter = slideInVertically(
                                        animationSpec = spring(stiffness = 380f, dampingRatio = 0.8f),
                                    ) { it } + fadeIn(tween(200)),
                                    exit = slideOutVertically(
                                        animationSpec = tween(200, easing = NetRunnerEase),
                                    ) { it } + fadeOut(tween(150)),
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                ) {
                                    BottomDock(
                                        activeTab = activeTab,
                                        onChangeTab = { activeTab = it },
                                    )
                                }
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
                            onDismiss = {
                                isGitHubAuthOpen = false
                                githubAuthError = null
                            },
                            isAuthorizing = isAuthorizingGitHub,
                            externalError = githubAuthError,
                            onClearExternalError = { githubAuthError = null },
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
                                    if (token.isNotBlank()) "Google AI Studio key saved locally"
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
                            onOpenGitHubAuth = {
                                githubAuthError = null
                                isGitHubAuthOpen = true
                            },
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
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "netrunner" && data.host == "auth") {
            val error = data.getQueryParameter("error_description") ?: data.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                deepLinkErrorState = error
                return
            }
            val token = data.getQueryParameter("token")
            if (!token.isNullOrBlank()) {
                deepLinkTokenState = token
                return
            }
            val code = data.getQueryParameter("code")
            if (!code.isNullOrBlank()) {
                deepLinkCodeState = code
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
