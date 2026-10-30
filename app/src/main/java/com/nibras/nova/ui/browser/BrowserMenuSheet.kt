package com.nibras.nova.ui.browser

import com.nibras.nova.R
import com.nibras.nova.core.common.L

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
            MenuRowItem(Icons.Default.Bolt, L.s(R.string.t_nova_omni_command_palette_ctrl_k_2)) {
                onDismiss()
                onOpenOmniCommandPalette()
            }
            MenuRowItem(Icons.Default.AddBox, L.s(R.string.t_new_tab)) {
                onDismiss()
                onNewNormalTab()
            }
            MenuRowItem(Icons.Default.VisibilityOff, L.s(R.string.t_new_private_tab_2)) {
                onDismiss()
                onNewIncognitoTab()
            }
            MenuRowItem(Icons.Default.Lock, L.s(R.string.t_biometric_private_media_bookmark_vault)) {
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
                    Text(L.s(R.string.t_site_text_zoom, siteTextZoomPercent), style = MaterialTheme.typography.bodyLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onAdjustSiteZoom(-15) }) { Text("-") }
                    OutlinedButton(onClick = { onAdjustSiteZoom(15) }) { Text("+") }
                }
            }

            MenuRowItem(
                Icons.Default.Splitscreen,
                if (isSplitScreenActive) L.s(R.string.t_exit_dual_split_screen_webview) else L.s(R.string.t_dual_split_screen_webview_mode)
            ) {
                onDismiss()
                onToggleSplitScreenMode()
            }
            MenuRowItem(
                Icons.Default.SwipeDown,
                if (isAutoScrollActive) L.s(R.string.t_stop_hands_free_auto_scroll) else L.s(R.string.t_hands_free_auto_scroll_teleprompter)
            ) {
                onDismiss()
                onToggleAutoScrollTeleprompter()
            }
            MenuRowItem(Icons.Default.PictureInPictureAlt, L.s(R.string.t_floating_picture_in_picture_pip_mode)) {
                onDismiss()
                onEnterPipMode()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.Bookmarks, L.s(R.string.t_bookmarks_html_import_export)) {
                onDismiss()
                onOpenBookmarks()
            }
            MenuRowItem(Icons.Default.History, L.s(R.string.t_browsing_history)) {
                onDismiss()
                onOpenHistory()
            }
            MenuRowItem(Icons.Default.Download, L.s(R.string.t_download_center_sha_256_verifier)) {
                onDismiss()
                onOpenDownloads()
            }
            MenuRowItem(Icons.Default.RssFeed, L.s(R.string.t_nova_rss_atom_news_reader)) {
                onDismiss()
                onOpenRssReader()
            }
            MenuRowItem(Icons.Default.TrackChanges, L.s(R.string.t_web_page_change_monitor_price_news_watch)) {
                onDismiss()
                onOpenPageMonitor()
            }
            MenuRowItem(Icons.Default.Timer, L.s(R.string.t_digital_wellbeing_focus_site_timer)) {
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
                Text(L.s(R.string.t_desktop_site_mode), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = isDesktopMode,
                    onCheckedChange = {
                        onToggleDesktopMode(it)
                        onDismiss()
                    }
                )
            }

            MenuRowItem(Icons.Default.MenuBook, L.s(R.string.t_reader_mode_on_device_summarizer)) {
                onDismiss()
                onOpenReaderMode()
            }
            MenuRowItem(
                Icons.Default.RecordVoiceOver,
                if (isTtsSpeaking) L.s(R.string.t_stop_audio_voice_reader_tts) else L.s(R.string.t_listen_to_page_aloud_tts_voice_reader)
            ) {
                onDismiss()
                onToggleTtsReadAloud()
            }
            MenuRowItem(Icons.Default.EditNote, L.s(R.string.t_web_annotator_sticky_highlights_2)) {
                onDismiss()
                onOpenWebNotesAnnotator()
            }
            MenuRowItem(Icons.Default.Collections, L.s(R.string.t_page_media_image_batch_grabber)) {
                onDismiss()
                onOpenMediaGrabber()
            }
            MenuRowItem(Icons.Default.Archive, L.s(R.string.t_save_full_offline_web_archive_mht)) {
                onDismiss()
                onSaveFullMhtmlWebArchive()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.BugReport, L.s(R.string.t_web_developer_console_inspector_f12)) {
                onDismiss()
                onOpenDevToolsConsole()
            }
            MenuRowItem(Icons.Default.Colorize, L.s(R.string.t_eyedropper_color_picker_font_inspector)) {
                onDismiss()
                onOpenColorFontInspector()
            }
            MenuRowItem(Icons.Default.Code, L.s(R.string.t_userscripts_js_custom_css_manager)) {
                onDismiss()
                onOpenUserScriptsManager()
            }
            MenuRowItem(Icons.Default.DataObject, L.s(R.string.t_format_page_as_json_xml_tree)) {
                onDismiss()
                onOpenJsonXmlFormatter()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(Icons.Default.ContentCopy, L.s(R.string.t_copy_sanitized_link_60s_clipboard_auto_s)) {
                onDismiss()
                onCopyCleanUrlWithClipboardGuard()
            }
            MenuRowItem(Icons.Default.FindInPage, L.s(R.string.t_find_in_page)) {
                onDismiss()
                onFindInPage()
            }
            MenuRowItem(Icons.Default.Translate, L.s(R.string.t_translate_page_selection)) {
                onDismiss()
                onTranslatePage()
            }
            MenuRowItem(Icons.Default.CameraAlt, L.s(R.string.t_full_page_scrolling_screenshot_capture)) {
                onDismiss()
                onCaptureFullScrollingScreenshot()
            }
            MenuRowItem(Icons.Default.Print, L.s(R.string.t_print_save_page_as_pdf)) {
                onDismiss()
                onPrintPage()
            }
            MenuRowItem(Icons.Default.QrCode2, L.s(R.string.t_generate_qr_code_for_this_url)) {
                onDismiss()
                onGeneratePageQrCode()
            }
            MenuRowItem(Icons.Default.AddToHomeScreen, L.s(R.string.t_add_website_shortcut_to_home_screen)) {
                onDismiss()
                onAddHomeScreenShortcut()
            }
            MenuRowItem(Icons.Default.Share, L.s(R.string.t_share_webpage)) {
                onDismiss()
                onSharePage()
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MenuRowItem(
                icon = Icons.Default.LocalFireDepartment,
                label = L.s(R.string.t_emergency_panic_button_wipe_exit),
                tint = NovaDangerRed
            ) {
                onDismiss()
                onTriggerEmergencyPanicShred()
            }
            MenuRowItem(Icons.Default.Security, L.s(R.string.t_privacy_doh_adblock_backup)) {
                onDismiss()
                onOpenPrivacyDashboard()
            }
            MenuRowItem(Icons.Default.Settings, L.s(R.string.t_browser_settings)) {
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
