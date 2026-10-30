package com.nibras.nova.ui.playground

import org.junit.Assert.*
import org.junit.Test

class PlaygroundCoreTest {
    @Test fun wrapsFragment() {
        val d = PlaygroundDoc.build("<p>x</p>", "p{color:red}", "console.log(1)")
        assertTrue(d.startsWith("<!doctype html>"))
        assertTrue(d.indexOf(PlaygroundDoc.CONSOLE_HOOK) < d.indexOf("<style>"))
        assertTrue(d.indexOf("<p>x</p>") < d.indexOf("console.log(1)"))
    }
    @Test fun injectsIntoFullDocument() {
        val d = PlaygroundDoc.build("<!doctype html><html><head><title>t</title></head><body><p>a</p></body></html>", "b{}", "go()")
        assertEquals(1, Regex("<html").findAll(d).count())
        assertTrue(d.indexOf("<style>") < d.indexOf("</head>"))
        assertTrue(d.indexOf("go()") < d.indexOf("</body>"))
        assertTrue(d.indexOf(PlaygroundDoc.CONSOLE_HOOK) < d.indexOf("<title>"))
    }
    @Test fun highlightsJs() {
        val src = "const a = 'x'; // hi\nreturn 42"
        val t = CodeHighlighter.tokens(src, CodeLang.JS)
        fun kindAt(s: String) = t.first { src.substring(it.start, it.end) == s }.kind
        assertEquals(TokenKind.KEYWORD, kindAt("const")); assertEquals(TokenKind.STRING, kindAt("'x'"))
        assertEquals(TokenKind.COMMENT, kindAt("// hi")); assertEquals(TokenKind.NUMBER, kindAt("42"))
    }
    @Test fun highlightsHtmlAndCss() {
        val h = "<a href=\"x\">t</a><!-- c -->"
        val ht = CodeHighlighter.tokens(h, CodeLang.HTML)
        assertTrue(ht.any { h.substring(it.start, it.end) == "<a" && it.kind == TokenKind.TAG })
        assertTrue(ht.any { h.substring(it.start, it.end) == "href" && it.kind == TokenKind.ATTR })
        assertTrue(ht.any { it.kind == TokenKind.COMMENT })
        val c = "p { color: #fff; margin: 4px }"
        val ct = CodeHighlighter.tokens(c, CodeLang.CSS)
        assertTrue(ct.any { c.substring(it.start, it.end) == "#fff" && it.kind == TokenKind.COLOR })
        assertTrue(ct.any { c.substring(it.start, it.end) == "4px" && it.kind == TokenKind.NUMBER })
        assertEquals(2, CodeHighlighter.lineCount("a\nb"))
    }
}
