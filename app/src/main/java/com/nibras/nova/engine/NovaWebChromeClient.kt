package com.nibras.nova.engine

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.nibras.nova.domain.model.ConsoleLogLevel
import com.nibras.nova.domain.model.DevConsoleEntry
import com.nibras.nova.domain.model.SitePermissionType

data class WebsitePermissionPrompt(
    val origin: String,
    val permissionTypes: List<SitePermissionType>,
    val onGrant: () -> Unit,
    val onDeny: () -> Unit
)

interface ChromeSessionCallback {
    fun onProgressChanged(tabId: String, progress: Int)
    fun onTitleUpdated(tabId: String, title: String)
    fun onFaviconUpdated(tabId: String, icon: Bitmap)
    fun onConsoleMessageLogged(tabId: String, entry: DevConsoleEntry)
    fun onEnterFullScreenVideo(customView: View, exitCallback: WebChromeClient.CustomViewCallback)
    fun onExitFullScreenVideo()
    fun onFileUploadRequested(
        filePathCallback: ValueCallback<Array<Uri>>,
        acceptTypes: Array<String>,
        allowMultiple: Boolean
    ): Boolean
    fun onWebsitePermissionRequested(prompt: WebsitePermissionPrompt)
    fun onOpenNewTabFromUserGesture(targetUrl: String)
}

class NovaWebChromeClient(
    private val tabId: String,
    private val callback: ChromeSessionCallback
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        callback.onProgressChanged(tabId, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        if (!title.isNullOrBlank()) {
            callback.onTitleUpdated(tabId, title)
        }
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        super.onReceivedIcon(view, icon)
        if (icon != null) {
            callback.onFaviconUpdated(tabId, icon)
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            val level = when (consoleMessage.messageLevel()) {
                ConsoleMessage.MessageLevel.ERROR -> ConsoleLogLevel.ERROR
                ConsoleMessage.MessageLevel.WARNING -> ConsoleLogLevel.WARN
                ConsoleMessage.MessageLevel.TIP, ConsoleMessage.MessageLevel.LOG -> ConsoleLogLevel.INFO
                else -> ConsoleLogLevel.LOG
            }
            callback.onConsoleMessageLogged(
                tabId = tabId,
                entry = DevConsoleEntry(
                    level = level,
                    message = consoleMessage.message().orEmpty(),
                    sourceId = consoleMessage.sourceId().orEmpty(),
                    lineNumber = consoleMessage.lineNumber()
                )
            )
        }
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onShowCustomView(view: View?, customViewCallback: CustomViewCallback?) {
        if (view != null && customViewCallback != null) {
            callback.onEnterFullScreenVideo(view, customViewCallback)
        } else {
            super.onShowCustomView(view, customViewCallback)
        }
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        callback.onExitFullScreenVideo()
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (filePathCallback == null) return false
        val acceptTypes = fileChooserParams?.acceptTypes
            ?.filter { it.isNotBlank() }
            ?.toTypedArray()
            ?: arrayOf("*/*")
        val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
        return callback.onFileUploadRequested(filePathCallback, acceptTypes, allowMultiple)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        val origin = request.origin?.toString() ?: "Unknown Website"
        val mapped = request.resources.mapNotNull { res ->
            when (res) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> SitePermissionType.CAMERA
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> SitePermissionType.MICROPHONE
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> SitePermissionType.PROTECTED_MEDIA_DRM
                else -> null
            }
        }
        if (mapped.isEmpty()) {
            request.deny()
            return
        }
        callback.onWebsitePermissionRequested(
            WebsitePermissionPrompt(
                origin = origin,
                permissionTypes = mapped,
                onGrant = { request.grant(request.resources) },
                onDeny = { request.deny() }
            )
        )
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        geoCallback: GeolocationPermissions.Callback?
    ) {
        if (origin == null || geoCallback == null) return
        callback.onWebsitePermissionRequested(
            WebsitePermissionPrompt(
                origin = origin,
                permissionTypes = listOf(SitePermissionType.LOCATION),
                onGrant = { geoCallback.invoke(origin, true, false) },
                onDeny = { geoCallback.invoke(origin, false, false) }
            )
        )
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        if (!isUserGesture || view == null) return false
        val hitTestResult = view.hitTestResult
        val dataUrl = hitTestResult.extra
        if (!dataUrl.isNullOrBlank()) {
            callback.onOpenNewTabFromUserGesture(dataUrl)
            return true
        }
        return false
    }
}
