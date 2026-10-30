package com.nibras.nova.core.util

import com.nibras.nova.core.common.DispatcherProvider
import com.nibras.nova.domain.model.BookmarkItem
import com.nibras.nova.domain.model.TabItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

data class LanSyncEndpoint(
    val url: String,
    val ipAddress: String,
    val port: Int
)

/**
 * Stage 14.3: Zero-Cloud Peer-to-Peer Local Wi-Fi / LAN Transfer Server.
 * Starts an ephemeral local HTTP ServerSocket on a random high port and serves an interactive
 * HTML/JSON dashboard of open tabs and bookmarks so any phone or desktop on the same Wi-Fi
 * can scan the QR code and open/import them immediately without any external server.
 */
@Singleton
class LocalLanSyncServer @Inject constructor(
    private val dispatchers: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun startSharingSession(
        tabs: List<TabItem>,
        bookmarks: List<BookmarkItem>
    ): Result<LanSyncEndpoint> {
        stopSharingSession()
        return try {
            val ip = detectLocalWifiIpv4Address() ?: "127.0.0.1"
            val socket = ServerSocket(0)
            val port = socket.localPort
            serverSocket = socket

            val htmlPayload = buildInteractiveSyncHtml(tabs, bookmarks)

            serverJob = scope.launch {
                while (isActive && !socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    launch {
                        runCatching {
                            client.use { c ->
                                val writer = BufferedWriter(OutputStreamWriter(c.getOutputStream(), Charsets.UTF_8))
                                val bytes = htmlPayload.toByteArray(Charsets.UTF_8)
                                writer.write("HTTP/1.1 200 OK\r\n")
                                writer.write("Content-Type: text/html; charset=utf-8\r\n")
                                writer.write("Content-Length: ${bytes.size}\r\n")
                                writer.write("Connection: close\r\n\r\n")
                                writer.write(htmlPayload)
                                writer.flush()
                            }
                        }
                    }
                }
            }

            val endpointUrl = "http://$ip:$port"
            Result.success(LanSyncEndpoint(url = endpointUrl, ipAddress = ip, port = port))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopSharingSession() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun detectLocalWifiIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddr = addr.hostAddress.orEmpty()
                        if (hostAddr.startsWith("192.168.") ||
                            hostAddr.startsWith("10.") ||
                            hostAddr.startsWith("172.")
                        ) {
                            return hostAddr
                        }
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildInteractiveSyncHtml(tabs: List<TabItem>, bookmarks: List<BookmarkItem>): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html><html><head><meta charset='utf-8'/>")
        sb.appendLine("<meta name='viewport' content='width=device-width,initial-scale=1'/>")
        sb.appendLine("<title>Nibras Nova Local LAN Sync</title>")
        sb.appendLine("<style>body{background:#0A0F1D;color:#E2E8F0;font-family:sans-serif;padding:24px;max-width:720px;margin:auto}h1,h2{color:#00E5FF}a{color:#00E5FF;text-decoration:none}.card{background:#121826;padding:14px;border-radius:12px;margin-bottom:10px;border:1px solid #26324B}</style>")
        sb.appendLine("</head><body>")
        sb.appendLine("<h1>Nibras Nova — Peer-to-Peer LAN Sync</h1>")
        sb.appendLine("<h2>Open Tabs (${tabs.size})</h2>")
        for (t in tabs) {
            if (!t.isHomePage) {
                sb.appendLine("<div class='card'><strong>${t.title}</strong><br/><a href='${t.url}' target='_blank'>${t.url}</a></div>")
            }
        }
        sb.appendLine("<h2>Bookmarks (${bookmarks.size})</h2>")
        for (b in bookmarks) {
            sb.appendLine("<div class='card'><strong>${b.title}</strong><br/><a href='${b.url}' target='_blank'>${b.url}</a></div>")
        }
        sb.appendLine("</body></html>")
        return sb.toString()
    }
}
