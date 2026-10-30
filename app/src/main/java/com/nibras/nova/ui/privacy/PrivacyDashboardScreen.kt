package com.nibras.nova.ui.privacy

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Privacy, DoH, Anti-Fingerprint & Backup",
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
                                text = "  Live Shield Telemetry (Local)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricPill("Ads/Trackers", state.metrics.adsAndTrackersBlockedCount)
                            MetricPill("Clean URLs", state.metrics.trackingParamsStrippedCount)
                            MetricPill("Redirects", state.metrics.redirectChainsBlockedCount)
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
                        Text(" Clear Data")
                    }
                    OutlinedButton(
                        onClick = { showEncryptedBackupDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null)
                        Text(" AES-256 Backup")
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
                            text = "Shields, Anti-Fingerprinting & Zero-Trust",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        ToggleSettingRow(
                            title = "Canvas, WebGL & Audio Anti-Fingerprinting",
                            subtitle = "Inject cryptographic LSB noise into Canvas/WebGL & mask hardware concurrency",
                            checked = state.settings.enableAntiFingerprinting,
                            onCheckedChange = viewModel::setAntiFingerprinting
                        )

                        ToggleSettingRow(
                            title = "Built-in Ad & Tracker Blocker",
                            subtitle = "Block known advertising and analytics network requests",
                            checked = state.settings.enableAdAndTrackerBlocker,
                            onCheckedChange = viewModel::setAdAndTrackerBlocker
                        )

                        ToggleSettingRow(
                            title = "Strip Marketing & Tracking URL Parameters",
                            subtitle = "Automatically remove utm_*, fbclid, gclid, and mc_cid from URLs",
                            checked = state.settings.stripTrackingUrlParameters,
                            onCheckedChange = viewModel::setStripTrackingParams
                        )

                        ToggleSettingRow(
                            title = "Redirect Chain & Clickjacking Guard",
                            subtitle = "Block rapid >3-hop cross-domain ad redirect chains automatically",
                            checked = state.settings.enableRedirectChainGuard,
                            onCheckedChange = viewModel::setRedirectChainGuard
                        )

                        ToggleSettingRow(
                            title = "Biometric Lock for Private Tabs Vault",
                            subtitle = "Require Fingerprint / Face / PIN before viewing open Private Tabs",
                            checked = state.settings.biometricLockIncognitoTabs,
                            onCheckedChange = viewModel::setBiometricLockIncognito
                        )

                        ToggleSettingRow(
                            title = "HTTPS-Only Mode",
                            subtitle = "Automatically upgrade HTTP navigations to encrypted HTTPS",
                            checked = state.settings.httpsOnlyMode,
                            onCheckedChange = viewModel::setHttpsOnly
                        )

                        ToggleSettingRow(
                            title = "Android Safe Browsing",
                            subtitle = "Verify URLs against Play Protect / System WebView anti-phishing provider",
                            checked = state.settings.enableSafeBrowsing,
                            onCheckedChange = viewModel::setSafeBrowsing
                        )

                        ToggleSettingRow(
                            title = "Block Third-Party Cookies",
                            subtitle = "Disallow cross-domain tracking cookies via CookieManager",
                            checked = state.settings.blockThirdPartyCookies,
                            onCheckedChange = viewModel::setBlockThirdPartyCookies
                        )

                        ToggleSettingRow(
                            title = "JavaScript Execution",
                            subtitle = "Disable on untrusted sites to reduce attack surface",
                            checked = state.settings.enableJavaScript,
                            onCheckedChange = viewModel::setJavaScriptEnabled
                        )

                        ToggleSettingRow(
                            title = "DOM Storage (localStorage)",
                            subtitle = "Allow websites to store local key-value state",
                            checked = state.settings.enableDomStorage,
                            onCheckedChange = viewModel::setDomStorageEnabled
                        )

                        ToggleSettingRow(
                            title = "Send Do Not Track (DNT: 1)",
                            subtitle = "Include DNT privacy header on outgoing requests",
                            checked = state.settings.sendDoNotTrackHeader,
                            onCheckedChange = viewModel::setDoNotTrack
                        )

                        ToggleSettingRow(
                            title = "Block Screenshots in Private Tabs",
                            subtitle = "Enforce WindowManager FLAG_SECURE during Incognito sessions",
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
                            text = "DNS-over-HTTPS (DoH) & Proxy Controller",
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
                                label = { Text("Custom Proxy (e.g. 127.0.0.1:8080)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { viewModel.applyCustomProxy(proxyDraft) }) {
                                Text("Apply")
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
                            text = "Cookie Whitelist (Preserve Logins When Shredding Data)",
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
                                placeholder = { Text("e.g. github.com") },
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
                                Icon(Icons.Default.Add, contentDescription = "Add Domain")
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
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Whitelisted Domain")
                                }
                            }
                        }
                    }
                }
            }

            if (state.sitePermissions.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Website Permissions",
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
                                Icon(Icons.Default.Delete, contentDescription = "Revoke Permission")
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
            title = { Text("Clear Browsing Data") },
            text = {
                Column {
                    CheckRow("Browsing History & Favicons", clearHistory) { clearHistory = it }
                    CheckRow("Cookies & DOM Storage (Preserves Whitelisted Sites)", clearCookies) { clearCookies = it }
                    CheckRow("Cached Images & Files", clearCache) { clearCache = it }
                    CheckRow("Saved Site Hardware Permissions", clearPermissions) { clearPermissions = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.executeDataWipe(
                        clearHistory = clearHistory,
                        clearCookies = clearCookies,
                        clearCache = clearCache,
                        clearPermissions = clearPermissions
                    ) {
                        showClearDataDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Selected browsing data cleared.")
                        }
                    }
                }) {
                    Text("Clear Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
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
                Text(if (isRestoring) "Decrypt & Restore .novabackup" else "AES-256-GCM Encrypted Backup")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isRestoring) {
                            "Enter the password used to encrypt this .novabackup archive."
                        } else {
                            "Encrypts all Bookmarks, Quick Shortcuts, UserScripts/CSS, Cookie Whitelist, and RSS feeds using PBKDF2-HMAC-SHA256 + AES-256-GCM."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Encryption Passphrase (min 4 chars)") },
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
                            Text("Or Select Existing .novabackup File to Restore")
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
                    Text(if (isRestoring) "Decrypt & Restore" else "Encrypt & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptedBackupDialog = false
                    pendingRestoreUri = null
                }) {
                    Text("Cancel")
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
