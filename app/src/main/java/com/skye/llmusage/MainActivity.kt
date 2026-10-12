package com.skye.llmusage

import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skye.llmusage.data.AppStyle
import com.skye.llmusage.data.Provider
import com.skye.llmusage.data.ThemeMode
import com.skye.llmusage.ui.detail.DetailScreen
import com.skye.llmusage.ui.detail.DetailViewModel
import com.skye.llmusage.ui.detail.TrendScreen
import com.skye.llmusage.ui.detail.TrendViewModel
import com.skye.llmusage.ui.edit.EditScreen
import com.skye.llmusage.ui.edit.EditViewModel
import com.skye.llmusage.ui.glass.GlassConfig
import com.skye.llmusage.ui.glass.GlassController
import com.skye.llmusage.ui.glass.GlassPanel
import com.skye.llmusage.ui.glass.LocalGlassController
import com.skye.llmusage.ui.home.HomeScreen
import com.skye.llmusage.ui.home.HomeViewModel
import com.skye.llmusage.ui.settings.SettingsScreen
import com.skye.llmusage.ui.settings.SettingsViewModel
import com.skye.llmusage.ui.theme.LocalGlassEnabled
import com.skye.llmusage.ui.theme.LlmUsageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as LlmUsageApp
        val glass = GlassController(this)

        // 窗口分层(自底向上):渐变采样源 → 常规玻璃层 → 应用内容 → 悬浮玻璃层。
        // 玻璃面板与内容由 GlassPanel 按占位坐标同步,内容本身仍在 Compose 树中,
        // 点击/布局/内边距行为与纯 Compose 实现完全一致。
        val matchParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        val composeView = ComposeView(this)
        val root = FrameLayout(this)
        root.addView(glass.backgroundSource, matchParams)
        root.addView(glass.backHost, matchParams)
        root.addView(composeView, matchParams)
        root.addView(glass.frontHost, matchParams)
        setContentView(root)

        composeView.setContent {
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val mode = settings?.themeMode ?: ThemeMode.SYSTEM
            val style = settings?.appStyle ?: AppStyle.CLASSIC
            // 折射效果依赖 API 33+ 的 RuntimeShader;旧设备静默回退经典外观
            val glassActive = style == AppStyle.LIQUID_GLASS &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect { glass.setEnabled(glassActive, dark) }

            CompositionLocalProvider(
                LocalGlassEnabled provides glassActive,
                LocalGlassController provides glass,
            ) {
                LlmUsageTheme(
                    themeMode = mode,
                    dynamicColor = settings?.dynamicColor ?: true,
                    appStyle = style,
                ) {
                    AppNavHost()
                }
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
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val TABS = listOf(
    TabItem(Routes.HOME, R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    TabItem(Routes.TREND, R.string.tab_trend, Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    TabItem(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

/** 顶层路由集合:判定底部栏可见性;只分配一次,不随 AppNavHost 重组重建 */
private val TAB_ROUTES = TABS.map { it.route }.toSet()

/** 顶层结构:三 Tab 底部导航(主页 / 趋势 / 设置)+ 全屏子页(添加/详情/编辑) */
@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val navigateToTab: (String) -> Unit = { route ->
                    navController.navigate(route) {
                        // 单实例 + 状态保持 + 返回栈回起点
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                GlassPanel(
                    key = "bottom-nav",
                    config = GlassConfig.bar(),
                    modifier = Modifier.fillMaxWidth(),
                    classic = {
                        NavigationBar {
                            TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
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
                    },
                    glass = { GlassBottomBar(currentRoute, navigateToTab) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 8 } },
        ) {
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
                    appStyle = settings.appStyle,
                    onThemeChange = vm::setThemeMode,
                    onDynamicColorChange = vm::setDynamicColor,
                    onAppStyleChange = vm::setAppStyle,
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
    }
}

/** 玻璃模式底部导航:图标 + 文字的胶囊玻璃条,内容自行处理导航栏内边距 */
@Composable
private fun GlassBottomBar(currentRoute: String?, onTabSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TABS.forEach { tab ->
                val selected = currentRoute == tab.route
                val tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onTabSelect(tab.route) }
                        .padding(horizontal = 22.dp, vertical = 4.dp),
                ) {
                    Icon(
                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null,
                        tint = tint,
                    )
                    Text(
                        stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                    )
                }
            }
        }
    }
}
