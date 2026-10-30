package com.nibras.nova.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.UserAgentProfile
import com.nibras.nova.ui.tools.LanSyncDialog

@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    onOpenSearchEngineSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onOpenUserScriptsManager: () -> Unit,
    onOpenRssReader: () -> Unit,
    onOpenAboutScreen: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lanEndpoint by viewModel.activeLanSyncEndpoint.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var filterQuery by remember { mutableStateOf("") }
    var customHomeDraft by remember(settings.customHomepageUrl) { mutableStateOf(settings.customHomepageUrl) }

    fun matchesFilter(vararg keywords: String): Boolean {
        if (filterQuery.isBlank()) return true
        return keywords.any { it.contains(filterQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Nibras Nova Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search settings (e.g., language, lan sync, user-agent, doh)…") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stage 14.3: Local Wi-Fi / LAN Peer-to-Peer QR Sync Card
            if (matchesFilter("sync", "wifi", "lan", "qr", "transfer", "peer", "share")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.QrCode2,
                        title = "Local Wi-Fi / LAN Peer-to-Peer QR Sync",
                        subtitle = "Transfer open tabs & bookmarks directly to another phone or PC on your Wi-Fi without cloud servers",
                        onClick = viewModel::startLanSyncServer
                    )
                }
            }

            // Stage 14.1: Multi-Language Application Locale Switcher (AZ, TR, EN, RU, AR)
            if (matchesFilter("language", "locale", "az", "tr", "ru", "ar", "english", "dil")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Language,
                        title = "App Language (AZ • TR • EN • RU • AR)",
                        subtitle = "Switch between Azerbaijani, Turkish, English, Russian, and Arabic (RTL supported)",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    )
                }
            }

            if (matchesFilter("search", "engine", "google", "duckduckgo", "brave", "bing", "startpage")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Search,
                        title = "Search Engine (${settings.searchEngine.displayName})",
                        subtitle = "Configure default search provider, custom URL template, and local suggestions",
                        onClick = onOpenSearchEngineSettings
                    )
                }
            }

            if (matchesFilter("theme", "dark", "light", "wallpaper", "color", "turquoise", "appearance")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Palette,
                        title = "Theme & Homepage Wallpaper (${settings.themeMode.label})",
                        subtitle = "Customize Dark/Light appearance, accent intensity, and homepage background",
                        onClick = onOpenThemeSettings
                    )
                }
            }

            if (matchesFilter("privacy", "security", "adblock", "doh", "proxy", "backup", "fingerprint")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Security,
                        title = "Privacy, Anti-Fingerprint, DoH & Encrypted Backup",
                        subtitle = "Canvas/WebGL shield, DNS-over-HTTPS, Custom Proxy, Cookie Whitelist, and AES-256 Backup",
                        onClick = onOpenPrivacyDashboard
                    )
                }
            }

            if (matchesFilter("script", "userscript", "css", "tampermonkey", "extension", "code")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Code,
                        title = "UserScripts (JS) & Custom CSS Manager",
                        subtitle = "Inject custom JavaScript or CSS stylesheets per domain like desktop extensions",
                        onClick = onOpenUserScriptsManager
                    )
                }
            }

            if (matchesFilter("rss", "atom", "feed", "news", "blog")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.RssFeed,
                        title = "Nova RSS & Atom News Reader",
                        subtitle = "Manage ad-free XML news and blog subscriptions directly inside Nibras Nova",
                        onClick = onOpenRssReader
                    )
                }
            }

            if (matchesFilter("user-agent", "profile", "windows", "macos", "safari", "ios", "iphone", "linux", "firefox")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Multi-OS User-Agent & Device Simulator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            UserAgentProfile.entries.forEach { profile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateUserAgentProfile(profile) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.userAgentProfile == profile,
                                        onClick = { viewModel.updateUserAgentProfile(profile) }
                                    )
                                    Text(profile.displayName, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (matchesFilter("home", "homepage", "shortcuts", "tabs", "startup", "desktop", "bottom", "audio", "data saver", "hibernation")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Ergonomics, Data Saver & Memory",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = customHomeDraft,
                                onValueChange = {
                                    customHomeDraft = it
                                    viewModel.updateCustomHomepageUrl(it)
                                },
                                label = { Text("Homepage Address (use nova://home for Nibras Homepage)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            SettingsSwitchItem(
                                title = "Smart Data Saver & Autoplay Limiter",
                                subtitle = "Block heavy network images and media autoplay on metered connections",
                                checked = settings.enableDataSaverMode,
                                onCheckedChange = viewModel::updateDataSaverMode
                            )

                            SettingsSwitchItem(
                                title = "Automatic Tab Hibernation (Memory Saver)",
                                subtitle = "Hibernate inactive background tabs beyond the 4-tab LRU pool to save RAM",
                                checked = settings.enableAutoTabHibernation,
                                onCheckedChange = viewModel::updateAutoTabHibernation
                            )

                            SettingsSwitchItem(
                                title = "Bottom Address Bar (One-Handed Mode)",
                                subtitle = "Dock the OmniAddressBar at the bottom above navigation controls",
                                checked = settings.bottomAddressBarReachability,
                                onCheckedChange = viewModel::updateBottomAddressBarReachability
                            )

                            SettingsSwitchItem(
                                title = "Desktop Horizontal Tab Strip",
                                subtitle = "Show a desktop-style top tab bar for instant 1-tap tab switching",
                                checked = settings.enableDesktopTabStrip,
                                onCheckedChange = viewModel::updateEnableDesktopTabStrip
                            )

                            SettingsSwitchItem(
                                title = "Allow Background Audio & Media Playback",
                                subtitle = "Keep web audio/podcasts playing when switching tabs or turning screen off",
                                checked = settings.allowBackgroundAudioPlayback,
                                onCheckedChange = viewModel::updateAllowBackgroundAudioPlayback
                            )

                            SettingsSwitchItem(
                                title = "Restore Open Tabs on Startup",
                                subtitle = "Reopen your normal tabs automatically when launching Nibras Nova",
                                checked = settings.restoreTabsOnStartup,
                                onCheckedChange = viewModel::updateRestoreTabsOnStartup
                            )

                            SettingsSwitchItem(
                                title = "Request Desktop Site by Default",
                                subtitle = "Open new tabs with desktop user-agent and wide viewport",
                                checked = settings.defaultDesktopMode,
                                onCheckedChange = viewModel::updateDefaultDesktopMode
                            )

                            SettingsSwitchItem(
                                title = "Show Quick-Access Shortcuts on Homepage",
                                subtitle = "Display customizable pinned websites grid",
                                checked = settings.showQuickShortcutsOnHome,
                                onCheckedChange = {
                                    viewModel.updateHomepageVisibility(it, settings.showRecentHistoryOnHome)
                                }
                            )

                            SettingsSwitchItem(
                                title = "Show Recently Visited Strip on Homepage",
                                subtitle = "Display recent non-private history items on the homepage",
                                checked = settings.showRecentHistoryOnHome,
                                onCheckedChange = {
                                    viewModel.updateHomepageVisibility(settings.showQuickShortcutsOnHome, it)
                                }
                            )
                        }
                    }
                }
            }

            if (matchesFilter("external", "app", "links", "intent")) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "External Application Link Policy",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExternalLinkPolicy.entries.forEach { policy ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updateExternalLinkPolicy(policy) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = settings.externalLinkPolicy == policy,
                                        onClick = { viewModel.updateExternalLinkPolicy(policy) }
                                    )
                                    Text(policy.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            if (matchesFilter("about", "version", "nibras", "webview", "license")) {
                item {
                    SettingsNavCard(
                        icon = Icons.Default.Info,
                        title = "About Nibras Nova",
                        subtitle = "Version 2.0.0-nova-flagship • Nibras Code Ecosystem & Chromium Engine Diagnostics",
                        onClick = onOpenAboutScreen
                    )
                }
            }
        }
    }

    lanEndpoint?.let { endpoint ->
        LanSyncDialog(
            endpoint = endpoint,
            onStopAndDismiss = viewModel::stopLanSyncServer
        )
    }
}

@Composable
private fun SettingsNavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
