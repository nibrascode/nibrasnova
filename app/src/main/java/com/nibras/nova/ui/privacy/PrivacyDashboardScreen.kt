package com.nibras.nova.ui.privacy

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibras.nova.core.security.DnsOverHttpsProvider
import kotlinx.coroutines.launch

@Composable
fun PrivacyDashboardScreen(
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val pinGuard = com.nibras.nova.ui.parental.rememberPinGuard()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showEncryptedBackupDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var newWhitelistDomain by remember { mutableStateOf("") }
    var proxyDraft by remember(state.settings.customProxyHostPort) {
        mutableStateOf(state.settings.customProxyHostPort)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L.s(R.string.t_back))
                }
                Text(
                    text = L.s(R.string.t_privacy_doh_anti_fingerprint_backup),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
            // 1. Live Telemetry Counters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "  " + L.s(R.string.t_live_shield_telemetry_local),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricPill(L.s(R.string.t_ads_trackers), state.metrics.adsAndTrackersBlockedCount)
                            MetricPill(L.s(R.string.t_clean_urls), state.metrics.trackingParamsStrippedCount)
                            MetricPill(L.s(R.string.t_redirects), state.metrics.redirectChainsBlockedCount)
                            MetricPill("HTTPS", state.metrics.httpsUpgradesCount)
                        }
                    }
                }
            }

            // 2. Action Buttons: Clear Browsing Data & AES-256-GCM Encrypted Backup
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Text(" " + L.s(R.string.t_clear_data_2))
                    }
                    OutlinedButton(
                        onClick = { showEncryptedBackupDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null)
                        Text(" " + L.s(R.string.t_aes_256_backup))
                    }
                }
            }

            // 3. Active Security, Anti-Fingerprinting, Redirect Guard & Biometric Vault Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = L.s(R.string.t_shields_anti_fingerprinting_zero_trust),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_canvas_webgl_audio_anti_fingerprinting),
                            subtitle = L.s(R.string.t_inject_cryptographic_lsb_noise_into_canv),
                            checked = state.settings.enableAntiFingerprinting,
                            onCheckedChange = viewModel::setAntiFingerprinting
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_built_in_ad_tracker_blocker),
                            subtitle = L.s(R.string.t_block_known_advertising_and_analytics_ne),
                            checked = state.settings.enableAdAndTrackerBlocker,
                            onCheckedChange = viewModel::setAdAndTrackerBlocker
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_strip_marketing_tracking_url_parameters),
                            subtitle = L.s(R.string.t_automatically_remove_utm_fbclid_gclid_an),
                            checked = state.settings.stripTrackingUrlParameters,
                            onCheckedChange = viewModel::setStripTrackingParams
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_redirect_chain_clickjacking_guard),
                            subtitle = "Block rapid >3-hop cross-domain ad redirect chains automatically",
                            checked = state.settings.enableRedirectChainGuard,
                            onCheckedChange = viewModel::setRedirectChainGuard
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_biometric_lock_for_private_tabs_vault),
                            subtitle = L.s(R.string.t_require_fingerprint_face_pin_before_view),
                            checked = state.settings.biometricLockIncognitoTabs,
                            onCheckedChange = viewModel::setBiometricLockIncognito
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_https_only_mode),
                            subtitle = L.s(R.string.t_automatically_upgrade_http_navigations_t),
                            checked = state.settings.httpsOnlyMode,
                            onCheckedChange = viewModel::setHttpsOnly
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_android_safe_browsing),
                            subtitle = L.s(R.string.t_verify_urls_against_play_protect_system),
                            checked = state.settings.enableSafeBrowsing,
                            onCheckedChange = viewModel::setSafeBrowsing
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_block_third_party_cookies),
                            subtitle = L.s(R.string.t_disallow_cross_domain_tracking_cookies_v),
                            checked = state.settings.blockThirdPartyCookies,
                            onCheckedChange = viewModel::setBlockThirdPartyCookies
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_javascript_execution),
                            subtitle = L.s(R.string.t_disable_on_untrusted_sites_to_reduce_att),
                            checked = state.settings.enableJavaScript,
                            onCheckedChange = viewModel::setJavaScriptEnabled
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_dom_storage_localstorage),
                            subtitle = L.s(R.string.t_allow_websites_to_store_local_key_value),
                            checked = state.settings.enableDomStorage,
                            onCheckedChange = viewModel::setDomStorageEnabled
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_send_do_not_track_dnt_1),
                            subtitle = L.s(R.string.t_include_dnt_privacy_header_on_outgoing_r),
                            checked = state.settings.sendDoNotTrackHeader,
                            onCheckedChange = viewModel::setDoNotTrack
                        )

                        ToggleSettingRow(
                            title = L.s(R.string.t_block_screenshots_in_private_tabs),
                            subtitle = L.s(R.string.t_enforce_windowmanager_flag_secure_during),
                            checked = state.settings.blockScreenshotsInIncognito,
                            onCheckedChange = viewModel::setBlockIncognitoScreenshots
                        )
                    }
                }
            }

            // 4. DNS-over-HTTPS (DoH) & Custom Proxy Controller (Stage 11.5)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = L.s(R.string.t_dns_over_https_doh_proxy_controller),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        DnsOverHttpsProvider.entries.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDnsOverHttpsProvider(provider) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.settings.dnsOverHttpsProvider == provider,
                                    onClick = { viewModel.setDnsOverHttpsProvider(provider) }
                                )
                                Column {
                                    Text(provider.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text(provider.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = proxyDraft,
                                onValueChange = { proxyDraft = it },
                                label = { Text(L.s(R.string.t_custom_proxy_e_g_127_0_0_1_8080)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { viewModel.applyCustomProxy(proxyDraft) }) {
                                Text(L.s(R.string.t_apply))
                            }
                        }
                    }
                }
            }

            // 5. Domain Cookie Whitelist Manager
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = L.s(R.string.t_cookie_whitelist_preserve_logins_when_sh),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newWhitelistDomain,
                                onValueChange = { newWhitelistDomain = it },
                                placeholder = { Text(L.s(R.string.t_e_g_github_com)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newWhitelistDomain.isNotBlank()) {
                                        viewModel.addWhitelistedCookieDomain(newWhitelistDomain)
                                        newWhitelistDomain = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = L.s(R.string.t_add_domain))
                            }
                        }

                        state.cookieWhitelist.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry.domain, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { viewModel.removeWhitelistedCookieDomain(entry.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_remove_whitelisted_domain))
                                }
                            }
                        }
                    }
                }
            }

            if (state.sitePermissions.isNotEmpty()) {
                item {
                    Text(
                        text = L.s(R.string.t_saved_website_permissions),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(state.sitePermissions, key = { it.id }) { perm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(perm.origin, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text(
                                    "${perm.permissionType.displayName}: ${perm.decision.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { viewModel.removeSitePermission(perm.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = L.s(R.string.t_revoke_permission))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDataDialog) {
        var clearHistory by remember { mutableStateOf(true) }
        var clearCookies by remember { mutableStateOf(true) }
        var clearCache by remember { mutableStateOf(true) }
        var clearPermissions by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text(L.s(R.string.t_clear_browsing_data)) },
            text = {
                Column {
                    CheckRow(L.s(R.string.t_browsing_history_favicons), clearHistory) { clearHistory = it }
                    CheckRow(L.s(R.string.t_cookies_dom_storage_preserves_whiteliste), clearCookies) { clearCookies = it }
                    CheckRow(L.s(R.string.t_cached_images_files), clearCache) { clearCache = it }
                    CheckRow(L.s(R.string.t_saved_site_hardware_permissions), clearPermissions) { clearPermissions = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    pinGuard { viewModel.executeDataWipe(
                        clearHistory = clearHistory,
                        clearCookies = clearCookies,
                        clearCache = clearCache,
                        clearPermissions = clearPermissions
                    ) {
                        showClearDataDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(L.s(R.string.t_selected_browsing_data_cleared))
                        }
                    } }
                }) {
                    Text(L.s(R.string.t_clear_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        )
    }

    // Stage 12.4: AES-256-GCM Encrypted Backup & Restore Dialog
    if (showEncryptedBackupDialog || pendingRestoreUri != null) {
        var passphrase by remember { mutableStateOf("") }
        val isRestoring = pendingRestoreUri != null
        AlertDialog(
            onDismissRequest = {
                showEncryptedBackupDialog = false
                pendingRestoreUri = null
            },
            title = {
                Text(if (isRestoring) L.s(R.string.t_decrypt_restore_novabackup) else L.s(R.string.t_aes_256_gcm_encrypted_backup))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isRestoring) {
                            L.s(R.string.t_enter_the_password_used_to_encrypt_this)
                        } else {
                            L.s(R.string.t_encrypts_all_bookmarks_quick_shortcuts_u)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(L.s(R.string.t_encryption_passphrase_min_4_chars)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isRestoring) {
                        OutlinedButton(
                            onClick = {
                                showEncryptedBackupDialog = false
                                restoreFileLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(L.s(R.string.t_or_select_existing_novabackup_file_to_re))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (passphrase.length >= 4) {
                        if (isRestoring && pendingRestoreUri != null) {
                            viewModel.restoreEncryptedNovaBackup(context, pendingRestoreUri!!, passphrase)
                        } else {
                            viewModel.exportEncryptedNovaBackup(context, passphrase)
                        }
                        showEncryptedBackupDialog = false
                        pendingRestoreUri = null
                    }
                }) {
                    Text(if (isRestoring) L.s(R.string.t_decrypt_restore) else L.s(R.string.t_encrypt_export))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptedBackupDialog = false
                    pendingRestoreUri = null
                }) {
                    Text(L.s(R.string.t_cancel))
                }
            }
        )
    }
}

@Composable
private fun MetricPill(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleSettingRow(
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

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
