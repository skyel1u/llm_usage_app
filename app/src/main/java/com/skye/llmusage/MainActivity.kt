package com.skye.llmusage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skye.llmusage.data.Provider
import com.skye.llmusage.data.ThemeMode
import com.skye.llmusage.ui.detail.DetailScreen
import com.skye.llmusage.ui.detail.DetailViewModel
import com.skye.llmusage.ui.detail.TrendScreen
import com.skye.llmusage.ui.detail.TrendViewModel
import com.skye.llmusage.ui.edit.EditScreen
import com.skye.llmusage.ui.edit.EditViewModel
import com.skye.llmusage.ui.home.HomeScreen
import com.skye.llmusage.ui.home.HomeViewModel
import com.skye.llmusage.ui.settings.SettingsScreen
import com.skye.llmusage.ui.settings.SettingsViewModel
import com.skye.llmusage.ui.theme.LlmUsageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as LlmUsageApp
        setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            LlmUsageTheme(
                themeMode = settings?.themeMode ?: ThemeMode.SYSTEM,
                dynamicColor = settings?.dynamicColor ?: true,
            ) {
                AppNavHost()
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val TREND = "trend"
    const val SETTINGS = "settings"
    const val ADD = "add"
    const val DETAIL = "detail/{accountId}"
    const val EDIT = "edit/{accountId}"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long) = "edit/$id"
}

private data class TabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val TABS = listOf(
    TabItem(Routes.HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    TabItem(Routes.TREND, R.string.tab_trend, Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    TabItem(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/** 顶层路由集合:判定底部栏可见性;只分配一次,不随 AppNavHost 重组重建 */
private val TAB_ROUTES = TABS.map { it.route }.toSet()

/** 顶层结构:三 Tab Scaffold 底部导航 + 全屏子页;转场用 Expressive 动效方案的弹簧 */
@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES
    // 方案对象可在组合中读取,弹簧 spec 在转场 lambda 外解析(泛型需显式指定)
    val motion = MaterialTheme.motionScheme
    val fastSpatial = motion.fastSpatialSpec<Float>()
    val fastEffects = motion.fastEffectsSpec<Float>()
    val slideSpec = motion.fastSpatialSpec<IntOffset>()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // NavigationBarItem 是 RowScope 扩展,必须内联在 NavigationBar 内容里
                NavigationBar {
                    TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(navController, tab.route) },
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            enterTransition = {
                fadeIn(fastSpatial) + slideInHorizontally(slideSpec) { it / 8 }
            },
            exitTransition = { fadeOut(fastEffects) },
            popEnterTransition = { fadeIn(fastSpatial) },
            popExitTransition = {
                fadeOut(fastEffects) + slideOutHorizontally(slideSpec) { it / 8 }
            },
        ) { appGraph(navController) }
    }
}

/** 单实例 + 状态保持 + 返回栈回起点 */
private fun navigateToTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavGraphBuilder.appGraph(navController: NavHostController) {
    composable(Routes.HOME) {
        val vm: HomeViewModel = viewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            onRefresh = vm::refresh,
            onOpenAccount = { navController.navigate(Routes.detail(it)) },
            onAddAccount = { navController.navigate(Routes.ADD) },
        )
    }

    composable(Routes.TREND) {
        val vm: TrendViewModel = viewModel()
        val accounts by vm.accounts.collectAsStateWithLifecycle()
        val selectedId by vm.selectedAccountId.collectAsStateWithLifecycle()
        val history by vm.history.collectAsStateWithLifecycle()
        val refreshing by vm.refreshing.collectAsStateWithLifecycle()
        TrendScreen(
            accounts = accounts,
            selectedAccountId = selectedId,
            history = history,
            refreshing = refreshing,
            onSelectAccount = vm::select,
            onRefresh = vm::refresh,
            onEdit = { navController.navigate(Routes.edit(it)) },
        )
    }

    composable(Routes.SETTINGS) {
        val vm: SettingsViewModel = viewModel()
        val settings by vm.settings.collectAsStateWithLifecycle()
        SettingsScreen(
            themeMode = settings.themeMode,
            dynamicColor = settings.dynamicColor,
            onThemeChange = vm::setThemeMode,
            onDynamicColorChange = vm::setDynamicColor,
            onClearHistory = vm::clearHistory,
        )
    }

    composable(Routes.ADD) {
        val vm: EditViewModel = viewModel()
        val duplicate by vm.duplicateName.collectAsStateWithLifecycle()
        EditScreen(
            initialAccountId = null,
            initialName = null,
            initialProvider = null,
            initialKey = null,
            duplicateName = duplicate,
            onDismissDuplicate = vm::dismissDuplicate,
            onConfirmDuplicate = vm::confirmDuplicate,
            onBack = { navController.popBackStack() },
            onSave = { id, name, provider, apiKey ->
                vm.save(id, name, provider, apiKey) { navController.popBackStack() }
            },
        )
    }

    composable(Routes.DETAIL) { backStack ->
        val accountId = backStack.arguments?.getString("accountId")?.toLongOrNull() ?: return@composable
        val vm: DetailViewModel = viewModel()
        val account by vm.account(accountId).collectAsStateWithLifecycle(initialValue = null)
        val history by vm.history(accountId).collectAsStateWithLifecycle(initialValue = emptyList())
        val refreshing by vm.refreshing.collectAsStateWithLifecycle()
        DetailScreen(
            account = account,
            history = history,
            refreshing = refreshing,
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(Routes.edit(it)) },
            onDelete = {
                vm.delete(accountId) { navController.popBackStack() }
            },
            onRefresh = { vm.refresh(accountId) },
        )
    }

    composable(Routes.EDIT) { backStack ->
        val accountId = backStack.arguments?.getString("accountId")?.toLongOrNull() ?: return@composable
        val vm: EditViewModel = viewModel()
        val duplicate by vm.duplicateName.collectAsStateWithLifecycle()
        // 编辑模式需先读取账户;用仓储一次性取值
        val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as LlmUsageApp
        var loaded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Pair<String, Provider>?>(null) }
        var loadedKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
        androidx.compose.runtime.LaunchedEffect(accountId) {
            app.repository.getAccount(accountId)?.let {
                loaded = it.name to Provider.of(it.provider)
                loadedKey = it.apiKey
            }
        }
        loaded?.let { (name, provider) ->
            EditScreen(
                initialAccountId = accountId,
                initialName = name,
                initialProvider = provider,
                initialKey = loadedKey,
                duplicateName = duplicate,
                onDismissDuplicate = vm::dismissDuplicate,
                onConfirmDuplicate = vm::confirmDuplicate,
                onBack = { navController.popBackStack() },
                onSave = { id, n, p, k ->
                    vm.save(id, n, p, k) { navController.popBackStack() }
                },
            )
        }
    }
}
