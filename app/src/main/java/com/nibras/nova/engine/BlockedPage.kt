package com.nibras.nova.engine

import android.webkit.WebResourceResponse
import com.nibras.nova.R
import com.nibras.nova.core.common.L
import java.io.ByteArrayInputStream

object BlockedPage {
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    fun html(isRtl: Boolean = L.s(R.string.blocked_back).any { it in '\u0600'..'\u06FF' }): String {
        val dir = if (isRtl) "rtl" else "ltr"
        return """<!doctype html><html dir="$dir"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>${esc(L.s(R.string.blocked_title))}</title>
<style>
html,body{margin:0;height:100%;background:#070d26;color:#e8ecf8;font-family:system-ui,-apple-system,Roboto,sans-serif}
.w{min-height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:32px;box-sizing:border-box;
background:radial-gradient(ellipse at 50% 30%,rgba(91,140,255,.18),transparent 60%)}
.ar{font-size:30px;color:#9cc0ff;margin-bottom:10px;font-family:'Noto Naskh Arabic','Amiri',serif;direction:rtl}
.m{font-size:34px;line-height:1.3;font-weight:600;max-width:560px;margin:0 0 10px}
.s{font-size:14px;color:#8d97b5;margin-bottom:36px}
.t{font-size:15px;color:#b4bed8;margin:0 0 4px}.b{font-size:13px;color:#7f89a8;max-width:420px;margin:0 0 28px}
button{background:linear-gradient(90deg,#3d7bff,#8e6bff);color:#fff;border:0;border-radius:24px;padding:13px 34px;font-size:16px}
</style></head><body><div class="w">
${if (isRtl) "" else "<div class=\"ar\">اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ</div>"}
<p class="m">${esc(L.s(R.string.blocked_reminder))}</p>
<div class="s">${esc(L.s(R.string.blocked_source))}</div>
<p class="t">${esc(L.s(R.string.blocked_title))}</p>
<p class="b">${esc(L.s(R.string.blocked_body))}</p>
<button onclick="if(history.length>1){history.back()}else{location.replace('about:blank')}">${esc(L.s(R.string.blocked_back))}</button>
</div></body></html>"""
    }

    fun response(): WebResourceResponse =
        WebResourceResponse("text/html", "utf-8", ByteArrayInputStream(html().toByteArray(Charsets.UTF_8)))

    fun redirectResponse(target: String): WebResourceResponse {
        val t = target.replace("\\", "\\\\").replace("'", "\\'").replace("<", "\\x3c")
        val body = "<!doctype html><meta charset=utf-8><script>location.replace('$t')</script>"
        return WebResourceResponse("text/html", "utf-8", ByteArrayInputStream(body.toByteArray()))
    }

    fun empty(): WebResourceResponse = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
