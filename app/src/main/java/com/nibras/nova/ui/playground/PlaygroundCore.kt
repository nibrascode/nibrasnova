package com.nibras.nova.ui.playground

/** Pure helpers for the HTML playground (unit-tested). */
object PlaygroundDoc {
    /** Console hook: forwards console.* and uncaught errors (stringified) to the WebView console. */
    const val CONSOLE_HOOK = "<script>(function(){var o={};['log','info','warn','error','debug'].forEach(function(k){o[k]=console[k];" +
        "console[k]=function(){var a=[].slice.call(arguments).map(function(x){if(typeof x==='string')return x;try{return JSON.stringify(x)}catch(e){return String(x)}});" +
        "o[k].call(console,a.join(' '))}});window.addEventListener('error',function(e){console.error((e.message||'Error')+' (line '+(e.lineno||'?')+')')});" +
        "window.addEventListener('unhandledrejection',function(e){console.error('Unhandled: '+(e.reason&&e.reason.message||e.reason))});})();</script>"

    fun isFullDocument(html: String): Boolean = Regex("<html[\\s>]|<!doctype", RegexOption.IGNORE_CASE).containsMatchIn(html)

    fun build(html: String, css: String, js: String): String {
        val style = if (css.isBlank()) "" else "<style>\n$css\n</style>"
        val script = if (js.isBlank()) "" else "<script>\n$js\n</script>"
        if (!isFullDocument(html)) {
            return "<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                CONSOLE_HOOK + style + "</head><body>\n" + html + "\n" + script + "</body></html>"
        }
        var doc = html
        val headOpen = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(doc)
        doc = if (headOpen != null) doc.replaceRange(headOpen.range.last + 1, headOpen.range.last + 1, CONSOLE_HOOK)
        else Regex("<html[^>]*>", RegexOption.IGNORE_CASE).find(doc)?.let { doc.replaceRange(it.range.last + 1, it.range.last + 1, "<head>$CONSOLE_HOOK</head>") } ?: (CONSOLE_HOOK + doc)
        if (style.isNotEmpty()) {
            val i = doc.indexOf("</head>", ignoreCase = true)
            doc = if (i >= 0) doc.substring(0, i) + style + doc.substring(i) else style + doc
        }
        if (script.isNotEmpty()) {
            val i = doc.lastIndexOf("</body>", ignoreCase = true)
            doc = if (i >= 0) doc.substring(0, i) + script + doc.substring(i) else doc + script
        }
        return doc
    }
}

enum class CodeLang { HTML, CSS, JS }
enum class TokenKind { COMMENT, STRING, TAG, ATTR, KEYWORD, NUMBER, PROPERTY, COLOR }
data class Token(val start: Int, val end: Int, val kind: TokenKind)

object CodeHighlighter {
    private val JS_KW = "\\b(?:var|let|const|function|return|if|else|for|while|do|switch|case|break|continue|new|class|extends|import|export|from|default|try|catch|finally|throw|async|await|typeof|instanceof|in|of|this|null|undefined|true|false)\\b"
    private val rules: Map<CodeLang, List<Pair<Regex, TokenKind>>> = mapOf(
        CodeLang.HTML to listOf(
            Regex("<!--[\\s\\S]*?(?:-->|$)") to TokenKind.COMMENT,
            Regex("\"[^\"\\n]*\"|'[^'\\n]*'") to TokenKind.STRING,
            Regex("</?[a-zA-Z][\\w-]*|/?>") to TokenKind.TAG,
            Regex("(?<=\\s)[a-zA-Z_:][\\w:.-]*(?==)") to TokenKind.ATTR,
        ),
        CodeLang.CSS to listOf(
            Regex("/\\*[\\s\\S]*?(?:\\*/|$)") to TokenKind.COMMENT,
            Regex("\"[^\"\\n]*\"|'[^'\\n]*'") to TokenKind.STRING,
            Regex("#[0-9a-fA-F]{3,8}\\b") to TokenKind.COLOR,
            Regex("[a-zA-Z-]+(?=\\s*:)") to TokenKind.PROPERTY,
            Regex("-?\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg|fr)?") to TokenKind.NUMBER,
            Regex("@[a-zA-Z-]+") to TokenKind.KEYWORD,
        ),
        CodeLang.JS to listOf(
            Regex("//[^\\n]*|/\\*[\\s\\S]*?(?:\\*/|$)") to TokenKind.COMMENT,
            Regex("\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|`(?:\\\\.|[^`\\\\])*`") to TokenKind.STRING,
            Regex(JS_KW) to TokenKind.KEYWORD,
            Regex("\\b\\d+(?:\\.\\d+)?\\b") to TokenKind.NUMBER,
        ),
    )

    /** Non-overlapping tokens; earlier rules (comments, strings) win. */
    fun tokens(text: String, lang: CodeLang): List<Token> {
        if (text.length > 60_000) return emptyList()
        val taken = BooleanArray(text.length)
        val out = ArrayList<Token>()
        for ((re, kind) in rules.getValue(lang)) {
            for (m in re.findAll(text)) {
                val r = m.range; if (r.isEmpty()) continue
                if ((r.first..r.last).any { taken[it] }) continue
                for (i in r) taken[i] = true
                out += Token(r.first, r.last + 1, kind)
            }
        }
        return out.sortedBy { it.start }
    }

    fun lineCount(text: String): Int = text.count { it == '\n' } + 1
}

object PlaygroundTemplates {
    data class Template(val id: String, val html: String, val css: String, val js: String)
    val ALL = listOf(
        Template("hello", "<h1>Salam, dünya!</h1>\n<p>Nibras Nova HTML test yeri.</p>",
            "body { font-family: sans-serif; padding: 16px; }\nh1 { color: #3d7bff; }", "console.log('Hello from JS');"),
        Template("counter", "<button id=\"btn\">+1</button>\n<p>Count: <b id=\"n\">0</b></p>",
            "button { font-size: 20px; padding: 8px 20px; border-radius: 12px; }",
            "let n = 0;\ndocument.getElementById('btn').onclick = () => {\n  n++;\n  document.getElementById('n').textContent = n;\n  console.log('count', n);\n};"),
        Template("cards", "<div class=\"grid\">\n  <div class=\"card\">1</div>\n  <div class=\"card\">2</div>\n  <div class=\"card\">3</div>\n</div>",
            ".grid { display: flex; gap: 12px; flex-wrap: wrap; }\n.card { flex: 1 1 90px; height: 90px; border-radius: 14px;\n  background: linear-gradient(135deg, #3d7bff, #8e6bff);\n  color: #fff; display: grid; place-items: center; font-size: 28px; }", ""),
        Template("canvas", "<canvas id=\"c\" width=\"300\" height=\"200\"></canvas>",
            "body { margin: 0; background: #070d26; }",
            "const c = document.getElementById('c').getContext('2d');\nlet t = 0;\nfunction draw() {\n  c.fillStyle = '#070d26'; c.fillRect(0, 0, 300, 200);\n  c.fillStyle = '#ffd27a';\n  c.beginPath(); c.arc(150 + Math.cos(t) * 80, 100 + Math.sin(t) * 50, 14, 0, Math.PI * 2); c.fill();\n  t += 0.05; requestAnimationFrame(draw);\n}\ndraw();"),
        Template("single", "<!doctype html>\n<html>\n<head>\n  <title>Single file</title>\n  <style>p { color: teal; }</style>\n</head>\n<body>\n  <p>One file with HTML, CSS and JS.</p>\n  <script>console.log(document.title);</script>\n</body>\n</html>", "", ""),
    )
}
