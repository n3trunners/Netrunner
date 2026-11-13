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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.netrunner.core.RustDaemonBridge
import com.netrunner.model.ActiveTab
import com.netrunner.model.GitHubRepo
import com.netrunner.model.GitHubStats
import com.netrunner.model.GitHubUser
import com.netrunner.model.SavedApiKeyEntry
import com.netrunner.model.SettingsModalType
import com.netrunner.model.UserCustomIntegration
import com.netrunner.model.WorkspaceProject
import com.netrunner.network.GitHubApiClient
import com.netrunner.ui.BrowseScreen
import com.netrunner.ui.ConfirmDeleteOverlay
import com.netrunner.ui.DashboardScreen
import com.netrunner.ui.GitHubAuthOverlay
import com.netrunner.ui.NewProjectOverlay
import com.netrunner.ui.ProfileScreen
import com.netrunner.ui.SettingsDetailOverlay
import com.netrunner.ui.SettingsScreen
import com.netrunner.ui.SettingsSubPageScreen
import com.netrunner.ui.WorkspaceTerminalScreen
import com.netrunner.ui.components.BottomDock
import com.netrunner.ui.components.ToastBanner
import com.netrunner.ui.theme.NetRunnerEase
import com.netrunner.ui.theme.NetRunnerTheme
import com.netrunner.ui.theme.NetRunnerWallpaperBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class PendingDeleteAction(
    val title: String,
    val description: String,
    val confirmLabel: String = "Delete",
    val onConfirm: () -> Unit,
)

class MainActivity : ComponentActivity() {
    private var deepLinkTokenState by mutableStateOf<String?>(null)
    private var deepLinkCodeState by mutableStateOf<String?>(null)
    private var deepLinkErrorState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAuthIntent(intent)

        val prefs = getSharedPreferences("netrunner_prefs", Context.MODE_PRIVATE)

        setContent {
            val scope = rememberCoroutineScope()

            // Theme & navigation state
            var isDark by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
            var activeTab by remember { mutableStateOf(ActiveTab.HOME) }
            var isSettingsOpen by remember { mutableStateOf(false) }
            var activeSettingsSubPage by remember { mutableStateOf<SettingsModalType?>(null) }
            var activeSettingsModal by remember { mutableStateOf<SettingsModalType?>(null) }
            var isNewProjectOpen by remember { mutableStateOf(false) }
            var isGitHubAuthOpen by remember { mutableStateOf(false) }
            var isAuthorizingGitHub by remember { mutableStateOf(false) }
            var githubAuthError by remember { mutableStateOf<String?>(null) }
            var pendingDeleteAction by remember { mutableStateOf<PendingDeleteAction?>(null) }
            var selectedProject by remember { mutableStateOf<WorkspaceProject?>(null) }
            var toastMessage by remember { mutableStateOf<String?>(null) }
            var selectedLanguage by remember {
                mutableStateOf(prefs.getString("selected_language", "English (US)") ?: "English (US)")
            }

            // Boot native daemon on IO dispatcher so frame-1 UI renders immediately
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    RustDaemonBridge.boot(filesDir)
                }
            }

            // BYOK token state + ordered saved keys list
            val savedApiKeys = remember {
                mutableStateListOf<SavedApiKeyEntry>().apply {
                    val parsed = SavedApiKeyEntry.listFromJson(prefs.getString("saved_api_keys", null))
                    if (parsed.isNotEmpty()) {
                        addAll(parsed)
                    } else {
                        val legacyKey = prefs.getString("user_api_token", "")?.trim().orEmpty()
                        if (legacyKey.isNotEmpty()) {
                            val migrated = SavedApiKeyEntry(
                                id = "key-migrated-${System.currentTimeMillis()}",
                                provider = "gemini-api",
                                providerLabel = "Gemini API",
                                apiKey = legacyKey,
                                createdAt = "Saved",
                            )
                            add(migrated)
                            prefs.edit().putString("saved_api_keys", SavedApiKeyEntry.listToJson(this)).apply()
                        }
                    }
                }
            }
            var userApiToken by remember {
                mutableStateOf(savedApiKeys.firstOrNull()?.apiKey ?: prefs.getString("user_api_token", "") ?: "")
            }
            var selectedModel by remember {
                mutableStateOf(prefs.getString("gemini_model", "gemini-4-hydrogen") ?: "gemini-4-hydrogen")
            }

            fun persistApiKeys() {
                val primaryKey = savedApiKeys.firstOrNull()?.apiKey.orEmpty()
                userApiToken = primaryKey
                prefs.edit()
                    .putString("saved_api_keys", SavedApiKeyEntry.listToJson(savedApiKeys))
                    .putString("user_api_token", primaryKey)
                    .apply()
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

            BackHandler(
                enabled = pendingDeleteAction != null ||
                    isNewProjectOpen ||
                    isGitHubAuthOpen ||
                    activeSettingsModal != null ||
                    selectedProject != null ||
                    activeSettingsSubPage != null ||
                    isSettingsOpen ||
                    activeTab != ActiveTab.HOME,
            ) {
                when {
                    pendingDeleteAction != null -> pendingDeleteAction = null
                    isNewProjectOpen -> isNewProjectOpen = false
                    isGitHubAuthOpen -> if (!isAuthorizingGitHub) {
                        isGitHubAuthOpen = false
                        githubAuthError = null
                    }
                    activeSettingsModal != null -> activeSettingsModal = null
                    selectedProject != null -> selectedProject = null
                    activeSettingsSubPage != null -> activeSettingsSubPage = null
                    isSettingsOpen -> isSettingsOpen = false
                    activeTab != ActiveTab.HOME -> activeTab = ActiveTab.HOME
                }
            }

            val isAnyOverlayOpen = isGitHubAuthOpen ||
                isNewProjectOpen ||
                activeSettingsModal != null ||
                pendingDeleteAction != null
            val bgBlur by animateDpAsState(
                targetValue = if (isAnyOverlayOpen) 16.dp else 0.dp,
                animationSpec = tween(220, easing = NetRunnerEase),
                label = "overlayBgBlur",
            )

            NetRunnerTheme(darkTheme = isDark) {
                NetRunnerWallpaperBox(backgroundBlur = bgBlur) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (bgBlur > 0.dp) Modifier.blur(bgBlur) else Modifier),
                    ) {
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
                                onOpenTokenSettings = {
                                    selectedProject = null
                                    isSettingsOpen = true
                                    activeSettingsSubPage = SettingsModalType.MODELS
                                },
                                onBack = { selectedProject = null },
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val subPage = activeSettingsSubPage
                                val routeKey = when {
                                    subPage != null -> "subpage:${subPage.name}"
                                    isSettingsOpen -> "settings"
                                    activeTab == ActiveTab.BROWSE -> "browse"
                                    activeTab == ActiveTab.PROFILE -> "profile"
                                    else -> "home"
                                }

                                AnimatedContent(
                                    targetState = routeKey,
                                    transitionSpec = {
                                        when {
                                            targetState.startsWith("subpage:") -> {
                                                (slideInHorizontally(tween(280, easing = NetRunnerEase)) { it / 4 } +
                                                    fadeIn(tween(240, easing = NetRunnerEase)))
                                                    .togetherWith(
                                                        slideOutHorizontally(tween(240, easing = NetRunnerEase)) { -it / 8 } +
                                                            fadeOut(tween(180)),
                                                    )
                                            }
                                            initialState.startsWith("subpage:") -> {
                                                (slideInHorizontally(tween(280, easing = NetRunnerEase)) { -it / 8 } +
                                                    fadeIn(tween(240, easing = NetRunnerEase)))
                                                    .togetherWith(
                                                        slideOutHorizontally(tween(240, easing = NetRunnerEase)) { it / 4 } +
                                                            fadeOut(tween(180)),
                                                    )
                                            }
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
                                    when {
                                        key.startsWith("subpage:") -> {
                                            val pageType = SettingsModalType.entries.firstOrNull {
                                                it.name == key.removePrefix("subpage:")
                                            } ?: SettingsModalType.MODELS
                                            SettingsSubPageScreen(
                                                pageType = pageType,
                                                savedKeys = savedApiKeys,
                                                connectors = connectors,
                                                agents = agents,
                                                skills = skills,
                                                onBack = { activeSettingsSubPage = null },
                                                onSaveNewApiKey = { rawKey ->
                                                    val entry = SavedApiKeyEntry(
                                                        id = "key-${System.currentTimeMillis()}",
                                                        provider = "gemini-api",
                                                        providerLabel = "Gemini API",
                                                        apiKey = rawKey.trim(),
                                                        createdAt = "Saved",
                                                    )
                                                    savedApiKeys.add(entry)
                                                    persistApiKeys()
                                                    showToast("Gemini API key saved")
                                                },
                                                onUpdateApiKey = { id, newKey ->
                                                    val index = savedApiKeys.indexOfFirst { it.id == id }
                                                    if (index >= 0) {
                                                        savedApiKeys[index] = savedApiKeys[index].copy(apiKey = newKey.trim())
                                                        persistApiKeys()
                                                        showToast("Gemini API key updated")
                                                    }
                                                },
                                                onDeleteApiKey = { id ->
                                                    savedApiKeys.removeAll { it.id == id }
                                                    persistApiKeys()
                                                    showToast("API key deleted")
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
                                                onRequestDeleteConfirm = { title, description, onConfirm ->
                                                    pendingDeleteAction = PendingDeleteAction(
                                                        title = title,
                                                        description = description,
                                                        onConfirm = onConfirm,
                                                    )
                                                },
                                                onShowToast = ::showToast,
                                            )
                                        }

                                        key == "settings" -> SettingsScreen(
                                            currentUser = currentUser,
                                            selectedLanguage = selectedLanguage,
                                            onBack = { isSettingsOpen = false },
                                            onOpenModal = { modal ->
                                                when (modal) {
                                                    SettingsModalType.CONNECTORS,
                                                    SettingsModalType.MODELS,
                                                    SettingsModalType.AGENTS,
                                                    SettingsModalType.SKILLS -> activeSettingsSubPage = modal
                                                    else -> activeSettingsModal = modal
                                                }
                                            },
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

                                        key == "browse" -> BrowseScreen(
                                            onBackToHome = { activeTab = ActiveTab.HOME },
                                            onShowToast = ::showToast,
                                        )

                                        key == "profile" -> ProfileScreen(
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
                                                val target = projects.firstOrNull { it.id == id }
                                                if (target != null) {
                                                    pendingDeleteAction = PendingDeleteAction(
                                                        title = "Delete ${target.name}?",
                                                        description = "Remove this project from your workspace? This cannot be undone.",
                                                        onConfirm = {
                                                            projects.removeAll { it.id == id }
                                                            persistProjects()
                                                            showToast("Deleted ${target.name}")
                                                        },
                                                    )
                                                }
                                            },
                                            onResetProjects = {
                                                pendingDeleteAction = PendingDeleteAction(
                                                    title = "Reset Workspace?",
                                                    description = "Delete all ${projects.size} project(s) and reset to the empty state?",
                                                    confirmLabel = "Reset",
                                                    onConfirm = {
                                                        projects.clear()
                                                        persistProjects()
                                                        showToast("Cleared workspace projects to empty state")
                                                    },
                                                )
                                            },
                                        )
                                    }
                                }

                                // Floating Bottom Navigation Dock (slides smoothly in/out when Settings opens)
                                AnimatedVisibility(
                                    visible = !isSettingsOpen && activeSettingsSubPage == null,
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
                    }

                    // Toast Notification
                    ToastBanner(
                        message = toastMessage,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding(),
                    )

                    // In-window Frosted Glassmorphism Overlays
                    GitHubAuthOverlay(
                        visible = isGitHubAuthOpen,
                        onDismiss = {
                            isGitHubAuthOpen = false
                            githubAuthError = null
                        },
                        isAuthorizing = isAuthorizingGitHub,
                        externalError = githubAuthError,
                        onClearExternalError = { githubAuthError = null },
                    )

                    NewProjectOverlay(
                        visible = isNewProjectOpen,
                        onDismiss = { isNewProjectOpen = false },
                        onCreateProject = { name, desc ->
                            val created = WorkspaceProject(
                                id = "proj-${System.currentTimeMillis()}",
                                name = name,
                                description = desc.ifBlank { "NetRunner workspace" },
                                category = "Fullstack",
                                architecture = "fullstack",
                                updatedAt = "Just now",
                                filesCount = (4..11).random(),
                            )
                            projects.add(0, created)
                            persistProjects()
                            selectedProject = null
                            activeSettingsSubPage = null
                            isSettingsOpen = false
                            activeTab = ActiveTab.HOME
                            showToast("Project \"$name\" created!")
                        },
                    )

                    SettingsDetailOverlay(
                        modalType = activeSettingsModal,
                        currentUser = currentUser,
                        selectedLanguage = selectedLanguage,
                        onDismiss = { activeSettingsModal = null },
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

                    ConfirmDeleteOverlay(
                        visible = pendingDeleteAction != null,
                        title = pendingDeleteAction?.title ?: "Confirm Delete",
                        description = pendingDeleteAction?.description ?: "",
                        confirmLabel = pendingDeleteAction?.confirmLabel ?: "Delete",
                        onCancel = { pendingDeleteAction = null },
                        onConfirm = {
                            val action = pendingDeleteAction
                            pendingDeleteAction = null
                            action?.onConfirm?.invoke()
                        },
                    )
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
