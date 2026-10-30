package com.nibras.nova.ui.playground

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.nibras.nova.R
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

private val EditorBg = Color(0xFF0B1022)
private val GutterBg = Color(0xFF0F1630)
private val CodeText = Color(0xFFE3E8F7)
private fun colorOf(k: TokenKind) = when (k) {
    TokenKind.COMMENT -> Color(0xFF6B7899); TokenKind.STRING -> Color(0xFFA5E28B); TokenKind.TAG -> Color(0xFF7FB2FF)
    TokenKind.ATTR -> Color(0xFFFFC66D); TokenKind.KEYWORD -> Color(0xFFD69CFF); TokenKind.NUMBER -> Color(0xFFFF9E7A)
    TokenKind.PROPERTY -> Color(0xFF7FDDE8); TokenKind.COLOR -> Color(0xFFFFD27A)
}

class SyntaxTransformation(private val lang: CodeLang) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val out = buildAnnotatedString {
            append(s)
            CodeHighlighter.tokens(s, lang).forEach { t ->
                addStyle(SpanStyle(color = colorOf(t.kind), fontStyle = if (t.kind == TokenKind.COMMENT) FontStyle.Italic else FontStyle.Normal), t.start, t.end)
            }
        }
        return TransformedText(out, OffsetMapping.Identity)
    }
}

/** Saved snippets in app-private storage. */
class SnippetStore(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("nova_playground", Context.MODE_PRIVATE)
    private fun all(): JSONObject = runCatching { JSONObject(sp.getString("snippets", "{}")!!) }.getOrDefault(JSONObject())
    fun names(): List<String> = all().keys().asSequence().toList().sorted()
    fun save(name: String, html: String, css: String, js: String) {
        val a = all(); a.put(name, JSONObject().put("html", html).put("css", css).put("js", js)); sp.edit().putString("snippets", a.toString()).apply()
    }
    fun load(name: String): Triple<String, String, String>? = all().optJSONObject(name)?.let { Triple(it.optString("html"), it.optString("css"), it.optString("js")) }
    fun delete(name: String) { val a = all(); a.remove(name); sp.edit().putString("snippets", a.toString()).apply() }
    var draft: Triple<String, String, String>?
        get() = sp.getString("draft", null)?.let { runCatching { JSONObject(it) }.getOrNull() }?.let { Triple(it.optString("html"), it.optString("css"), it.optString("js")) }
        set(v) { sp.edit().putString("draft", v?.let { JSONObject().put("html", it.first).put("css", it.second).put("js", it.third).toString() }).apply() }
}

@Composable
fun HtmlPlaygroundDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize().imePadding(), color = EditorBg) { HtmlPlaygroundContent(onDismiss) }
    }
}

@Composable
fun HtmlPlaygroundContent(onBack: () -> Unit, initialTab: Int = 0, previewEnabled: Boolean = true) {
    val context = LocalContext.current
    val store = remember { SnippetStore(context) }
    val start = remember { store.draft ?: PlaygroundTemplates.ALL[0].let { Triple(it.html, it.css, it.js) } }
    var html by remember { mutableStateOf(start.first) }
    var css by remember { mutableStateOf(start.second) }
    var js by remember { mutableStateOf(start.third) }
    var tab by remember { mutableIntStateOf(initialTab) }
    var runDoc by remember { mutableStateOf<String?>(null) }
    val logs = remember { mutableStateListOf<Pair<Int, String>>() }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { store.draft = Triple(html, css, js) } }
    fun run() { logs.clear(); runDoc = PlaygroundDoc.build(html, css, js) + "<!--${System.nanoTime()}-->"; tab = 3 }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(PlaygroundDoc.build(html, css, js).replace(PlaygroundDoc.CONSOLE_HOOK, "").toByteArray()) }
            android.widget.Toast.makeText(context, context.getString(R.string.pg_exported), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(Modifier.fillMaxSize().background(EditorBg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pr_close), tint = Color.White) }
            Text(stringResource(R.string.pg_title), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Button(onClick = { run() }, contentPadding = PaddingValues(horizontal = 14.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.pg_run))
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.pg_templates)) }, onClick = { menu = false; dialog = "templates" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pg_save)) }, onClick = { menu = false; dialog = "save" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pg_load)) }, onClick = { menu = false; dialog = "load" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pg_export)) }, onClick = { menu = false; exportLauncher.launch("nova-playground.html") })
                    DropdownMenuItem(text = { Text(stringResource(R.string.pg_share)) }, onClick = { menu = false; shareHtml(context, PlaygroundDoc.build(html, css, js).replace(PlaygroundDoc.CONSOLE_HOOK, "")) })
                }
            }
        }
        val tabs = listOf("HTML", "CSS", "JS", stringResource(R.string.pg_preview))
        TabRow(selectedTabIndex = tab, containerColor = GutterBg, contentColor = Color.White) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { if (i == 3 && runDoc == null) run() else tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> CodeEditor(html, { html = it }, CodeLang.HTML, Modifier.weight(1f))
            1 -> CodeEditor(css, { css = it }, CodeLang.CSS, Modifier.weight(1f))
            2 -> CodeEditor(js, { js = it }, CodeLang.JS, Modifier.weight(1f))
            else -> Column(Modifier.weight(1f)) {
                Box(Modifier.weight(0.65f).fillMaxWidth().background(Color.White)) {
                    if (previewEnabled) runDoc?.let { doc -> SandboxPreview(doc) { level, msg -> logs.add(level to msg) } }
                }
                ConsolePanel(logs, Modifier.weight(0.35f)) { logs.clear() }
            }
        }
    }
    }

    when (dialog) {
        "templates" -> AlertDialog(onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.pg_templates)) },
            text = {
                Column {
                    val labels = mapOf("hello" to R.string.pg_t_hello, "counter" to R.string.pg_t_counter, "cards" to R.string.pg_t_cards, "canvas" to R.string.pg_t_canvas, "single" to R.string.pg_t_single)
                    PlaygroundTemplates.ALL.forEach { t ->
                        Text(stringResource(labels.getValue(t.id)), Modifier.fillMaxWidth().clickable { html = t.html; css = t.css; js = t.js; tab = 0; dialog = null }.padding(vertical = 12.dp))
                    }
                }
            }, confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.pr_close)) } })
        "save" -> {
            var name by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.pg_save)) },
                text = { OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.pg_name)) }, singleLine = true) },
                confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { store.save(name.trim(), html, css, js); dialog = null
                    android.widget.Toast.makeText(context, context.getString(R.string.pc_saved), android.widget.Toast.LENGTH_SHORT).show() }) { Text(stringResource(R.string.pc_save)) } },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.pr_close)) } })
        }
        "load" -> {
            var names by remember { mutableStateOf(store.names()) }
            AlertDialog(onDismissRequest = { dialog = null }, title = { Text(stringResource(R.string.pg_load)) },
                text = {
                    if (names.isEmpty()) Text(stringResource(R.string.pg_none)) else LazyColumn {
                        items(names) { n ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n, Modifier.weight(1f).clickable { store.load(n)?.let { (h, c, j) -> html = h; css = c; js = j; tab = 0 }; dialog = null }.padding(vertical = 12.dp))
                                IconButton(onClick = { store.delete(n); names = store.names() }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.pr_close)) } })
        }
    }
}

private fun shareHtml(context: Context, doc: String) {
    runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, "nova-playground.html"); f.writeText(doc)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/html").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
fun CodeEditor(value: String, onChange: (String) -> Unit, lang: CodeLang, modifier: Modifier = Modifier) {
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp, color = CodeText)
    val transformation = remember(lang) { SyntaxTransformation(lang) }
    val lines = CodeHighlighter.lineCount(value)
    Row(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text((1..lines).joinToString("\n"), style = style.copy(color = Color(0xFF55607F), textAlign = TextAlign.End),
            modifier = Modifier.background(GutterBg).padding(horizontal = 8.dp, vertical = 8.dp).widthIn(min = 28.dp))
        Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            BasicTextField(
                value, onChange, textStyle = style, cursorBrush = SolidColor(Color(0xFFFFD27A)),
                visualTransformation = transformation,
                keyboardOptions = KeyboardOptions(autoCorrect = false, capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Ascii),
                modifier = Modifier.padding(8.dp).widthIn(min = 300.dp)
            )
        }
    }
}

@Composable
private fun ConsolePanel(logs: List<Pair<Int, String>>, modifier: Modifier, onClear: () -> Unit) {
    Column(modifier.fillMaxWidth().background(GutterBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.pg_console), color = Color(0xFFB4BED8), fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text(stringResource(R.string.pg_clear), fontSize = 12.sp) }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
            if (logs.isEmpty()) item { Text(stringResource(R.string.pg_console_empty), color = Color(0xFF55607F), fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            items(logs) { (lvl, m) ->
                Text("› $m", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    color = when (lvl) { 2 -> Color(0xFFFF7A7A); 1 -> Color(0xFFFFD27A); else -> CodeText })
            }
        }
    }
}

/** Isolated preview: no file/content access, no JS bridges, no navigation away, no storage. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SandboxPreview(doc: String, onLog: (Int, String) -> Unit) {
    val log by rememberUpdatedState(onLog)
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false; settings.allowContentAccess = false
            settings.domStorageEnabled = false; settings.databaseEnabled = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    val lvl = when (m.messageLevel()) { ConsoleMessage.MessageLevel.ERROR -> 2; ConsoleMessage.MessageLevel.WARNING -> 1; else -> 0 }
                    post { log(lvl, m.message()) }; return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val u = request.url?.toString().orEmpty()
                    if (com.nibras.nova.core.security.AdultContentFilter.shouldBlock(u))
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    return null
                }
            }
            tag = ""
        }
    }, update = { wv ->
        if (wv.tag != doc) { wv.tag = doc; wv.loadDataWithBaseURL("https://playground.nova.invalid/", doc, "text/html", "utf-8", null) }
    }, modifier = Modifier.fillMaxSize())
}
