package com.nibras.nova.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nibras.nova.ui.theme.NovaDangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    isDesktopMode: Boolean,
    isSplitScreenActive: Boolean,
    isAutoScrollActive: Boolean,
    isTtsSpeaking: Boolean,
    siteTextZoomPercent: Int,
    onDismiss: () -> Unit,
    onOpenOmniCommandPalette: () -> Unit,
    onNewNormalTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onToggleSplitScreenMode: () -> Unit,
    onToggleAutoScrollTeleprompter: () -> Unit,
    onEnterPipMode: () -> Unit,
    onAdjustSiteZoom: (Int) -> Unit,
    onCopyCleanUrlWithClipboardGuard: () -> Unit,
    onOpenPrivateVault: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenRssReader: () -> Unit,
    onOpenPageMonitor: () -> Unit,
    onOpenDigitalWellbeing: () -> Unit,
    onOpenReaderMode: () -> Unit,
    onToggleTtsReadAloud: () -> Unit,
    onOpenWebNotesAnnotator: () -> Unit,
    onOpenMediaGrabber: () -> Unit,
    onOpenColorFontInspector: () -> Unit,
    onOpenJsonXmlFormatter: () -> Unit,
    onSaveFullMhtmlWebArchive: () -> Unit,
    onOpenDevToolsConsole: () -> Unit,
    onOpenUserScriptsManager: () -> Unit,
    onToggleDesktopMode: (Boolean) -> Unit,
    onFindInPage: () -> Unit,
    onTranslatePage: () -> Unit,
    onCaptureFullScrollingScreenshot: () -> Unit,
    onPrintPage: () -> Unit,
    onGeneratePageQrCode: () -> Unit,
    onAddHomeScreenShortcut: () -> Unit,
    onSharePage: () -> Unit,
    onTriggerEmergencyPanicShred: () -> Unit,
    onOpenPrivacyDashboard: () -> Unit,
    onOpenSettings: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            MenuRowItem(Icons.Default.Bolt, "Nova Omni-Command Palette (Ctrl+K)") {
                onDismiss()
                onOpenOmniCommandPalette()
            }
            MenuRowItem(Icons.Default.AddBox, "New Tab") {
                onDismiss()
                onNewNormalTab()
            }
            MenuRowItem(Icons.Default.VisibilityOff, "New Private Tab") {
                onDismiss()
                onNewIncognitoTab()
            }
            MenuRowItem(Icons.Default.Lock, "Biometric Private Media & Bookmark Vault") {
                onDismiss()
                onOpenPrivateVault()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Site Text Zoom ($siteTextZoomPercent%)", style = MaterialTheme.typography.bodyLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdjustSiteZoom(-15) }) { Text("-") }
                    OutlinedButton(onClick = { onAdjustSiteZoom(15) }) { Text("+") }
                }
            }

            MenuRowItem(
                Icons.Default.Splitscreen,
                if (isSplitScreenActive) "Exit Dual Split-Screen WebView" else "Dual Split-Screen WebView Mode"
            ) {
                onDismiss()
                onToggleSplitScreenMode()
            }
            MenuRowItem(
                Icons.Default.SwipeDown,
                if (isAutoScrollActive) "Stop Hands-Free Auto-Scroll" else "Hands-Free Auto-Scroll (Teleprompter)"
            ) {
                onDismiss()
                onToggleAutoScrollTeleprompter()
            }
            MenuRowItem(Icons.Default.PictureInPictureAlt, "Floating Picture-in-Picture (PiP) Mode") {
                onDismiss()
                onEnterPipMode()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.Bookmarks, "Bookmarks (HTML Import / Export)") {
                onDismiss()
                onOpenBookmarks()
            }
            MenuRowItem(Icons.Default.History, "Browsing History") {
                onDismiss()
                onOpenHistory()
            }
            MenuRowItem(Icons.Default.Download, "Download Center & SHA-256 Verifier") {
                onDismiss()
                onOpenDownloads()
            }
            MenuRowItem(Icons.Default.RssFeed, "Nova RSS & Atom News Reader") {
                onDismiss()
                onOpenRssReader()
            }
            MenuRowItem(Icons.Default.TrackChanges, "Web Page Change Monitor (Price/News Watcher)") {
                onDismiss()
                onOpenPageMonitor()
            }
            MenuRowItem(Icons.Default.Timer, "Digital Wellbeing & Focus Site Timer") {
                onDismiss()
                onOpenDigitalWellbeing()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggleDesktopMode(!isDesktopMode)
                        onDismiss()
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DesktopWindows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Desktop Site Mode", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = isDesktopMode,
                    onCheckedChange = {
                        onToggleDesktopMode(it)
                        onDismiss()
                    }
                )
            }

            MenuRowItem(Icons.Default.MenuBook, "Reader Mode & On-Device Summarizer") {
                onDismiss()
                onOpenReaderMode()
            }
            MenuRowItem(
                Icons.Default.RecordVoiceOver,
                if (isTtsSpeaking) "Stop Audio Voice Reader (TTS)" else "Listen to Page Aloud (TTS Voice Reader)"
            ) {
                onDismiss()
                onToggleTtsReadAloud()
            }
            MenuRowItem(Icons.Default.EditNote, "Web Annotator & Sticky Highlights") {
                onDismiss()
                onOpenWebNotesAnnotator()
            }
            MenuRowItem(Icons.Default.Collections, "Page Media & Image Batch Grabber") {
                onDismiss()
                onOpenMediaGrabber()
            }
            MenuRowItem(Icons.Default.Archive, "Save Full Offline Web Archive (.mht)") {
                onDismiss()
                onSaveFullMhtmlWebArchive()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.BugReport, "Web Developer Console & Inspector (F12)") {
                onDismiss()
                onOpenDevToolsConsole()
            }
            MenuRowItem(Icons.Default.Colorize, "Eyedropper Color Picker & Font Inspector") {
                onDismiss()
                onOpenColorFontInspector()
            }
            MenuRowItem(Icons.Default.Code, "UserScripts (JS) & Custom CSS Manager") {
                onDismiss()
                onOpenUserScriptsManager()
            }
            MenuRowItem(Icons.Default.DataObject, "Format Page as JSON / XML Tree") {
                onDismiss()
                onOpenJsonXmlFormatter()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.ContentCopy, "Copy Sanitized Link (60s Clipboard Auto-Scrub)") {
                onDismiss()
                onCopyCleanUrlWithClipboardGuard()
            }
            MenuRowItem(Icons.Default.FindInPage, "Find in Page") {
                onDismiss()
                onFindInPage()
            }
            MenuRowItem(Icons.Default.Translate, "Translate Page / Selection") {
                onDismiss()
                onTranslatePage()
            }
            MenuRowItem(Icons.Default.CameraAlt, "Full-Page Scrolling Screenshot Capture") {
                onDismiss()
                onCaptureFullScrollingScreenshot()
            }
            MenuRowItem(Icons.Default.Print, "Print / Save Page as PDF") {
                onDismiss()
                onPrintPage()
            }
            MenuRowItem(Icons.Default.QrCode2, "Generate QR Code for This URL") {
                onDismiss()
                onGeneratePageQrCode()
            }
            MenuRowItem(Icons.Default.AddToHomeScreen, "Add Website Shortcut to Home Screen") {
                onDismiss()
                onAddHomeScreenShortcut()
            }
            MenuRowItem(Icons.Default.Share, "Share Webpage") {
                onDismiss()
                onSharePage()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(
                icon = Icons.Default.LocalFireDepartment,
                label = "Emergency Panic Button (Wipe & Exit)",
                tint = NovaDangerRed
            ) {
                onDismiss()
                onTriggerEmergencyPanicShred()
            }
            MenuRowItem(Icons.Default.Security, "Privacy, DoH, AdBlock & Backup") {
                onDismiss()
                onOpenPrivacyDashboard()
            }
            MenuRowItem(Icons.Default.Settings, "Browser Settings") {
                onDismiss()
                onOpenSettings()
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
