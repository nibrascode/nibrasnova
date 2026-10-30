package com.nibras.nova.core.halal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.LruCache
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

/**
 * Halal search mode: images are intercepted before the WebView shows them, classified on the
 * device and replaced by a blurred copy when not judged safe. Nothing is uploaded anywhere;
 * the image is fetched exactly as the page would have fetched it.
 */
object HalalImageFilter {
    private const val PREFS = "nova_halal"
    private const val MAX_BYTES = 12 * 1024 * 1024
    private lateinit var app: Context
    private val unsafeCache = LruCache<String, Boolean>(4000)
    private val blurredUrls = LruCache<String, Boolean>(2000)
    @Volatile private var classifier: NsfwClassifier? = null
    @Volatile private var modelTried = false

    data class RevealRequest(val webView: WeakReference<WebView>, val url: String)
    private val _reveals = MutableSharedFlow<RevealRequest>(extraBufferCapacity = 4)
    val revealRequests: SharedFlow<RevealRequest> = _reveals

    fun init(context: Context) { app = context.applicationContext }

    var enabled: Boolean
        get() = ::app.isInitialized && app.getSharedPreferences(PREFS, 0).getBoolean("on", true)
        set(v) { app.getSharedPreferences(PREFS, 0).edit().putBoolean("on", v).apply() }

    private fun model(): NsfwClassifier? {
        if (!modelTried) synchronized(this) {
            if (!modelTried) { classifier = runCatching { NsfwClassifier(app) }.getOrNull(); modelTried = true }
        }
        return classifier
    }

    fun isBlurred(url: String): Boolean = blurredUrls.get(url) == true

    /** Called from shouldInterceptRequest for sub-resources. Null = let the WebView load it normally. */
    fun intercept(request: WebResourceRequest, pageUrl: String?): WebResourceResponse? {
        if (!enabled || request.isForMainFrame || !request.method.equals("GET", true)) return null
        val raw = request.url?.toString() ?: return null
        if (!raw.startsWith("http")) return null
        val headers = request.requestHeaders.orEmpty()
        val (url, reveal) = HalalLogic.stripReveal(raw)
        if (!HalalLogic.looksLikeImage(url, headers.entries.firstOrNull { it.key.equals("Accept", true) }?.value)) return null
        val fetched = runCatching { fetch(url, headers) }.getOrNull() ?: return null
        val (code, mime, bytes, extra) = fetched
        if (reveal || code !in 200..299 || mime.contains("svg") || mime.contains("icon")) return respond(code, mime, bytes, extra)

        val cached = unsafeCache.get(url)
        val unsafe = cached ?: run {
            val bmp = decodeSampled(bytes) ?: return respond(code, mime, bytes, extra)
            val verdict = if (bmp.width < HalalLogic.MIN_SIDE_PX || bmp.height < HalalLogic.MIN_SIDE_PX) false
            else model()?.let { m -> runCatching { m.score(bmp) }.getOrNull()?.let { (s, skin) -> HalalLogic.isUnsafe(s, skin) } ?: true }
                ?: HalalLogic.isImageSearch(pageUrl)
            if (!verdict) { bmp.recycle(); unsafeCache.put(url, false); return respond(code, mime, bytes, extra) }
            unsafeCache.put(url, true)
            return blurredResponse(url, bmp, extra)
        }
        if (!unsafe) return respond(code, mime, bytes, extra)
        val bmp = decodeSampled(bytes) ?: return respond(code, mime, bytes, extra)
        return blurredResponse(url, bmp, extra)
    }

    private fun blurredResponse(url: String, bmp: Bitmap, extra: Map<String, String>): WebResourceResponse {
        blurredUrls.put(url, true)
        val out = blur(bmp); bmp.recycle()
        val bos = ByteArrayOutputStream(); out.compress(Bitmap.CompressFormat.JPEG, 70, bos); out.recycle()
        return respond(200, "image/jpeg", bos.toByteArray(), extra)
    }

    /** Heavy blur: shrink to a few pixels and scale back up, then dim. */
    fun blur(src: Bitmap): Bitmap {
        val w = src.width.coerceAtLeast(1); val h = src.height.coerceAtLeast(1)
        val sw = 10; val sh = (10f * h / w).toInt().coerceIn(1, 40)
        val tiny = Bitmap.createScaledBitmap(src, sw, sh, true)
        val mid = Bitmap.createScaledBitmap(tiny, sw * 4, sh * 4, true)
        val out = Bitmap.createScaledBitmap(mid, w, h, true)
        tiny.recycle(); mid.recycle()
        val m = if (out.isMutable) out else out.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(m).drawColor(Color.argb(90, 10, 14, 38))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 210; textAlign = Paint.Align.CENTER; textSize = (minOf(w, h) / 6f).coerceIn(14f, 64f) }
        Canvas(m).drawText("👁", w / 2f, h / 2f + p.textSize / 3, p)
        return m
    }

    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return null
        var s = 1; while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= 448) s *= 2
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s }) }.getOrNull()
    }

    private data class Fetched(val code: Int, val mime: String, val bytes: ByteArray, val extra: Map<String, String>)

    private fun fetch(start: String, headers: Map<String, String>): Fetched {
        var url = start
        repeat(5) {
            val f = fetchOnce(url, headers)
            val loc = f.extra["Location"]
            if (f.code !in 300..399 || loc == null) return f
            url = URL(URL(url), loc).toString()
        }
        throw IllegalStateException("redirects")
    }

    private fun fetchOnce(url: String, headers: Map<String, String>): Fetched {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 20_000; c.instanceFollowRedirects = true
        headers.forEach { (k, v) -> if (!k.equals("Accept-Encoding", true) && !k.equals("Range", true)) c.setRequestProperty(k, v) }
        runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()?.let { c.setRequestProperty("Cookie", it) }
        try {
            val code = c.responseCode
            val stream = if (code >= 400) c.errorStream else c.inputStream
            val bytes = stream?.use { s ->
                val bos = ByteArrayOutputStream(); val buf = ByteArray(16 * 1024); var total = 0
                while (true) { val n = s.read(buf); if (n < 0) break; total += n; if (total > MAX_BYTES) throw IllegalStateException("too big"); bos.write(buf, 0, n) }
                bos.toByteArray()
            } ?: ByteArray(0)
            val mime = c.contentType?.substringBefore(';')?.trim()?.lowercase() ?: "image/*"
            val extra = buildMap {
                c.getHeaderField("Access-Control-Allow-Origin")?.let { put("Access-Control-Allow-Origin", it) }
                c.getHeaderField("Cache-Control")?.let { put("Cache-Control", it) }
                c.getHeaderField("Location")?.let { put("Location", it) }
            }
            return Fetched(code, mime, bytes, extra)
        } finally { c.disconnect() }
    }

    private fun respond(code: Int, mime: String, bytes: ByteArray, extra: Map<String, String>): WebResourceResponse {
        val reason = if (code in 200..299) "OK" else "Status"
        return WebResourceResponse(mime, null, code.coerceIn(200, 599).let { if (it in 300..399) 502 else it }, reason, extra - "Location", ByteArrayInputStream(bytes))
    }

    /** JS bridge for tap-to-reveal. */
    class Bridge(webView: WebView) {
        private val ref = WeakReference(webView)
        @Volatile private var last = 0L
        @JavascriptInterface fun isBlurred(url: String?): Boolean = url != null && HalalImageFilter.isBlurred(url)
        @JavascriptInterface fun requestReveal(url: String?) {
            val now = System.currentTimeMillis()
            if (url == null || !isBlurred(url) || now - last < 1200) return
            last = now
            _reveals.tryEmit(RevealRequest(ref, url))
        }
    }

    const val TAP_JS = "(function(){if(window.__novaHalal)return;window.__novaHalal=1;" +
        "document.addEventListener('click',function(e){var t=e.target;if(!t||t.tagName!=='IMG'||!window.NovaHalal)return;" +
        "var s=t.currentSrc||t.src;if(!s||!NovaHalal.isBlurred(s))return;e.preventDefault();e.stopPropagation();NovaHalal.requestReveal(s);},true);" +
        "window.__novaReveal=function(s){var imgs=document.getElementsByTagName('img');for(var i=0;i<imgs.length;i++){var m=imgs[i];" +
        "if((m.currentSrc||m.src)===s){m.removeAttribute('srcset');m.src=s+(s.indexOf('?')<0?'?':'&')+'nova_reveal=1';}}};})();"

    fun revealJs(url: String): String = "window.__novaReveal&&window.__novaReveal(" + org.json.JSONObject.quote(url) + ")"
}
