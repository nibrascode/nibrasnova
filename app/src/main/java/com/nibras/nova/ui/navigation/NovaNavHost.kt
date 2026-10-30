package com.nibras.nova.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nibras.nova.HardwareKeyboardShortcut
import com.nibras.nova.ui.bookmarks.BookmarksScreen
import com.nibras.nova.ui.browser.BrowserScreen
import com.nibras.nova.ui.browser.BrowserViewModel
import com.nibras.nova.ui.downloads.DownloadsScreen
import com.nibras.nova.ui.history.HistoryScreen
import com.nibras.nova.ui.home.HomeScreen
import com.nibras.nova.ui.privacy.PrivacyDashboardScreen
import com.nibras.nova.ui.reader.ReaderModeScreen
import com.nibras.nova.ui.settings.AboutScreen
import com.nibras.nova.ui.settings.GeneralSettingsScreen
import com.nibras.nova.ui.settings.SearchEngineSettingsScreen
import com.nibras.nova.ui.settings.ThemeSettingsScreen
import com.nibras.nova.ui.tabs.TabOverviewScreen
import com.nibras.nova.ui.tools.DigitalWellbeingScreen
import com.nibras.nova.ui.tools.PageMonitorScreen
import com.nibras.nova.ui.tools.PrivateVaultScreen
import com.nibras.nova.ui.tools.QrScannerScreen
import com.nibras.nova.ui.tools.RssReaderScreen
import com.nibras.nova.ui.tools.UserScriptsScreen
import com.nibras.nova.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun NovaNavHost(
    isFirstLaunch: Boolean,
    incomingUrlFlow: SharedFlow<String>,
    keyboardShortcutFlow: SharedFlow<HardwareKeyboardShortcut>
) {
    val navController = rememberNavController()
    val sharedBrowserViewModel: BrowserViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        incomingUrlFlow.collect { externalUrl ->
            if (externalUrl.isNotBlank()) {
                sharedBrowserViewModel.submitOmnibarInput(externalUrl)
                navController.navigate(NovaRoutes.BROWSER) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isFirstLaunch) NovaRoutes.WELCOME else NovaRoutes.HOME
    ) {
        composable(NovaRoutes.WELCOME) {
            WelcomeScreen(
                onOnboardingComplete = {
                    navController.navigate(NovaRoutes.HOME) {
                        popUpTo(NovaRoutes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NovaRoutes.HOME) {
            HomeScreen(
                onOpenBrowser = { navController.navigate(NovaRoutes.BROWSER) { launchSingleTop = true } },
                onOpenTabs = { navController.navigate(NovaRoutes.TAB_OVERVIEW) },
                onOpenBookmarks = { navController.navigate(NovaRoutes.BOOKMARKS) },
                onOpenHistory = { navController.navigate(NovaRoutes.HISTORY) },
                onOpenDownloads = { navController.navigate(NovaRoutes.DOWNLOADS) },
                onOpenPrivacyDashboard = { navController.navigate(NovaRoutes.PRIVACY_DASHBOARD) },
                onOpenQrScanner = { navController.navigate(NovaRoutes.QR_SCANNER) },
                onOpenSettings = { navController.navigate(NovaRoutes.SETTINGS_GENERAL) }
            )
        }

        composable(NovaRoutes.BROWSER) {
            BrowserScreen(
                viewModel = sharedBrowserViewModel,
                keyboardShortcutFlow = keyboardShortcutFlow,
                onNavigateToHome = {
                    navController.navigate(NovaRoutes.HOME) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTabs = { navController.navigate(NovaRoutes.TAB_OVERVIEW) },
                onNavigateToPrivateVault = { navController.navigate(NovaRoutes.PRIVATE_VAULT) },
                onNavigateToBookmarks = { navController.navigate(NovaRoutes.BOOKMARKS) },
                onNavigateToHistory = { navController.navigate(NovaRoutes.HISTORY) },
                onNavigateToDownloads = { navController.navigate(NovaRoutes.DOWNLOADS) },
                onNavigateToRssReader = { navController.navigate(NovaRoutes.RSS_READER) },
                onNavigateToPageMonitor = { navController.navigate(NovaRoutes.PAGE_MONITOR) },
                onNavigateToDigitalWellbeing = { navController.navigate(NovaRoutes.DIGITAL_WELLBEING) },
                onNavigateToPrivacy = { navController.navigate(NovaRoutes.PRIVACY_DASHBOARD) },
                onNavigateToReaderMode = { navController.navigate(NovaRoutes.READER_MODE) },
                onNavigateToQrScanner = { navController.navigate(NovaRoutes.QR_SCANNER) },
                onNavigateToUserScripts = { navController.navigate(NovaRoutes.USER_SCRIPTS) },
                onNavigateToSettings = { navController.navigate(NovaRoutes.SETTINGS_GENERAL) }
            )
        }

        composable(NovaRoutes.TAB_OVERVIEW) {
            TabOverviewScreen(
                onBack = { navController.popBackStack() },
                onOpenHome = {
                    navController.navigate(NovaRoutes.HOME) {
                        launchSingleTop = true
                    }
                },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.PRIVATE_VAULT) {
            PrivateVaultScreen(
                onBack = { navController.popBackStack() },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.PAGE_MONITOR) {
            PageMonitorScreen(
                onBack = { navController.popBackStack() },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.DIGITAL_WELLBEING) {
            DigitalWellbeingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.BOOKMARKS) {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.DOWNLOADS) {
            DownloadsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.PRIVACY_DASHBOARD) {
            PrivacyDashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.READER_MODE) {
            ReaderModeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.QR_SCANNER) {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onQrCodeScanned = { scannedValue ->
                    sharedBrowserViewModel.submitOmnibarInput(scannedValue)
                    navController.navigate(NovaRoutes.BROWSER) {
                        popUpTo(NovaRoutes.QR_SCANNER) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.USER_SCRIPTS) {
            UserScriptsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.RSS_READER) {
            RssReaderScreen(
                onBack = { navController.popBackStack() },
                onOpenBrowser = {
                    navController.navigate(NovaRoutes.BROWSER) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(NovaRoutes.SETTINGS_GENERAL) {
            GeneralSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSearchEngineSettings = { navController.navigate(NovaRoutes.SETTINGS_SEARCH_ENGINE) },
                onOpenThemeSettings = { navController.navigate(NovaRoutes.SETTINGS_THEME) },
                onOpenPrivacyDashboard = { navController.navigate(NovaRoutes.PRIVACY_DASHBOARD) },
                onOpenUserScriptsManager = { navController.navigate(NovaRoutes.USER_SCRIPTS) },
                onOpenRssReader = { navController.navigate(NovaRoutes.RSS_READER) },
                onOpenAboutScreen = { navController.navigate(NovaRoutes.ABOUT_NOVA) }
            )
        }

        composable(NovaRoutes.SETTINGS_SEARCH_ENGINE) {
            SearchEngineSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.SETTINGS_THEME) {
            ThemeSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(NovaRoutes.ABOUT_NOVA) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
