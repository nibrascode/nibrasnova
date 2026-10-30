package com.nibras.nova.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.R
import com.nibras.nova.domain.model.ExternalLinkPolicy
import com.nibras.nova.domain.model.UserAgentProfile
import com.nibras.nova.ui.tools.LanSyncDialog

/** One settings row: short title + ⓘ explanation. */
private sealed class SettingRow(@StringRes val title: Int, @StringRes val info: Int) {
    class Nav(title: Int, info: Int, val value: String? = null, val onClick: () -> Unit) : SettingRow(title, info)
    class Toggle(title: Int, info: Int, val checked: Boolean, val onChange: (Boolean) -> Unit) : SettingRow(title, info)
    class Choice(title: Int, info: Int, val options: List<Pair<String, Boolean>>, val onSelect: (Int) -> Unit) : SettingRow(title, info)
    class TextInput(title: Int, info: Int, val value: String, val onChange: (String) -> Unit) : SettingRow(title, info)
}

private class SettingGroup(@StringRes val title: Int, val rows: List<SettingRow>)

fun externalLinkPolicyLabelRes(policy: ExternalLinkPolicy): Int = when (policy) {
    ExternalLinkPolicy.ASK_EVERY_TIME -> R.string.link_ask
    ExternalLinkPolicy.ALWAYS_OPEN_EXTERNAL -> R.string.link_always
    ExternalLinkPolicy.STAY_IN_BROWSER -> R.string.link_stay
}

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
    var infoDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val languageUnsupported = stringResource(R.string.set_language_unsupported)

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    val linkPolicyLabels = ExternalLinkPolicy.entries.map { stringResource(externalLinkPolicyLabelRes(it)) }

    val groups = listOf(
        SettingGroup(
            R.string.grp_general, listOf(
                SettingRow.Nav(R.string.set_language, R.string.set_language_info) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    } else {
                        Toast.makeText(context, languageUnsupported, Toast.LENGTH_LONG).show()
                    }
                },
                SettingRow.Nav(R.string.set_search, R.string.set_search_info, settings.searchEngine.displayName, onOpenSearchEngineSettings),
                SettingRow.Nav(R.string.set_theme, R.string.set_theme_info, onClick = onOpenThemeSettings)
            )
        ),
        SettingGroup(
            R.string.grp_home, listOf(
                SettingRow.TextInput(R.string.set_homepage, R.string.set_homepage_info, customHomeDraft) {
                    customHomeDraft = it
                    viewModel.updateCustomHomepageUrl(it)
                },
                SettingRow.Toggle(R.string.set_home_shortcuts, R.string.set_home_shortcuts_info, settings.showQuickShortcutsOnHome) {
                    viewModel.updateHomepageVisibility(it, settings.showRecentHistoryOnHome)
                },
                SettingRow.Toggle(R.string.set_home_recent, R.string.set_home_recent_info, settings.showRecentHistoryOnHome) {
                    viewModel.updateHomepageVisibility(settings.showQuickShortcutsOnHome, it)
                }
            )
        ),
        SettingGroup(
            R.string.grp_browsing, listOf(
                SettingRow.Toggle(R.string.set_restore_tabs, R.string.set_restore_tabs_info, settings.restoreTabsOnStartup, viewModel::updateRestoreTabsOnStartup),
                SettingRow.Toggle(R.string.set_bottom_bar, R.string.set_bottom_bar_info, settings.bottomAddressBarReachability, viewModel::updateBottomAddressBarReachability),
                SettingRow.Toggle(R.string.set_tab_strip, R.string.set_tab_strip_info, settings.enableDesktopTabStrip, viewModel::updateEnableDesktopTabStrip),
                SettingRow.Toggle(R.string.set_desktop_mode, R.string.set_desktop_mode_info, settings.defaultDesktopMode, viewModel::updateDefaultDesktopMode),
                SettingRow.Toggle(R.string.set_data_saver, R.string.set_data_saver_info, settings.enableDataSaverMode, viewModel::updateDataSaverMode),
                SettingRow.Toggle(R.string.set_hibernation, R.string.set_hibernation_info, settings.enableAutoTabHibernation, viewModel::updateAutoTabHibernation),
                SettingRow.Toggle(R.string.set_bg_audio, R.string.set_bg_audio_info, settings.allowBackgroundAudioPlayback, viewModel::updateAllowBackgroundAudioPlayback),
                SettingRow.Choice(
                    R.string.set_user_agent, R.string.set_user_agent_info,
                    UserAgentProfile.entries.map { it.displayName to (settings.userAgentProfile == it) }
                ) { viewModel.updateUserAgentProfile(UserAgentProfile.entries[it]) }
            )
        ),
        SettingGroup(
            R.string.grp_privacy, listOf(
                SettingRow.Nav(R.string.set_privacy_center, R.string.set_privacy_center_info, onClick = onOpenPrivacyDashboard),
                SettingRow.Choice(
                    R.string.set_external_links, R.string.set_external_links_info,
                    ExternalLinkPolicy.entries.mapIndexed { i, p -> linkPolicyLabels[i] to (settings.externalLinkPolicy == p) }
                ) { viewModel.updateExternalLinkPolicy(ExternalLinkPolicy.entries[it]) }
            )
        ),
        SettingGroup(
            R.string.grp_tools, listOf(
                SettingRow.Nav(R.string.set_lan_sync, R.string.set_lan_sync_info, onClick = viewModel::startLanSyncServer),
                SettingRow.Nav(R.string.set_rss, R.string.set_rss_info, onClick = onOpenRssReader),
                SettingRow.Nav(R.string.set_userscripts, R.string.set_userscripts_info, onClick = onOpenUserScriptsManager)
            )
        ),
        SettingGroup(
            R.string.grp_about, listOf(
                SettingRow.Nav(R.string.set_about, R.string.set_about_info, onClick = onOpenAboutScreen),
                SettingRow.Nav(R.string.set_features, R.string.set_features_info) { openUrl(NovaLinks.FEATURES) },
                SettingRow.Nav(R.string.set_privacy_policy, R.string.set_privacy_policy_info) { openUrl(NovaLinks.PRIVACY_POLICY) },
                SettingRow.Nav(R.string.set_website, R.string.set_website_info) { openUrl(NovaLinks.WEBSITE) }
            )
        )
    )

    // Resolve titles for filtering (stringResource must be called in composition)
    val titleTexts = groups.flatMap { g -> g.rows.map { it.title } }.distinct()
        .associateWith { stringResource(it) }
    val infoTexts = groups.flatMap { g -> g.rows.map { it.info } }.distinct()
        .associateWith { stringResource(it) }
    val q = filterQuery.trim()
    val visibleGroups = groups.map { g ->
        g to g.rows.filter { r ->
            q.isEmpty() || titleTexts[r.title].orEmpty().contains(q, true) || infoTexts[r.info].orEmpty().contains(q, true)
        }
    }.filter { it.second.isNotEmpty() }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.settings_search_hint)) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_info_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (visibleGroups.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.settings_no_results),
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            visibleGroups.forEach { (group, rows) ->
                item(key = group.title) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(group.title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            rows.forEachIndexed { index, row ->
                                if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                                SettingRowView(row) { infoDialog = row.title to row.info }
                            }
                        }
                    }
                }
            }
        }
    }

    infoDialog?.let { (title, info) ->
        AlertDialog(
            onDismissRequest = { infoDialog = null },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(info)) },
            confirmButton = { TextButton(onClick = { infoDialog = null }) { Text(stringResource(R.string.common_ok)) } }
        )
    }

    lanEndpoint?.let { endpoint ->
        LanSyncDialog(endpoint = endpoint, onStopAndDismiss = viewModel::stopLanSyncServer)
    }
}

@Composable
private fun InfoButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.common_info),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingRowView(row: SettingRow, onInfo: () -> Unit) {
    val title = stringResource(row.title)
    when (row) {
        is SettingRow.Nav -> Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = row.onClick).padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoButton(onInfo)
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                row.value?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is SettingRow.Toggle -> Row(
            modifier = Modifier.fillMaxWidth().clickable { row.onChange(!row.checked) }.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoButton(onInfo)
            Text(
                title,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Switch(checked = row.checked, onCheckedChange = row.onChange)
        }
        is SettingRow.Choice -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InfoButton(onInfo)
                Text(title, modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            }
            row.options.forEachIndexed { i, (label, selected) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { row.onSelect(i) }.padding(start = 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { row.onSelect(i) })
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is SettingRow.TextInput -> Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoButton(onInfo)
            OutlinedTextField(
                value = row.value,
                onValueChange = row.onChange,
                label = { Text(title) },
                singleLine = true,
                modifier = Modifier.weight(1f).padding(start = 6.dp, end = 6.dp)
            )
        }
    }
}
