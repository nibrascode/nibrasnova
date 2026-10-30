package com.nibras.nova.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibras.nova.R

val HomeBg = Color(0xFF070D26)
private val Glass = Color(0x1AFFFFFF)
private val GlassBorder = Color(0x33A8C4FF)
private val TextDim = Color(0xFFB4BED8)
private val Accent = Color(0xFF5B8CFF)

/** A real nibrascode.com page bundled with the app so "Discover" works offline. */
data class DiscoverItem(
    val path: String,
    @StringRes val title: Int,
    @StringRes val category: Int,
    @DrawableRes val thumb: Int,
    val readMinutes: Int?,
    val pathOverride: Map<String, String> = emptyMap()
) {
    /** Localized URL on nibrascode.com: az is the root, other languages use a prefix. */
    fun url(lang: String): String {
        val l = if (lang in setOf("en", "tr", "ru", "ar")) lang else "az"
        pathOverride[l]?.let { return "https://nibrascode.com$it" }
        return if (l == "az") "https://nibrascode.com/$path" else "https://nibrascode.com/$l/$path"
    }
}

val DISCOVER_ITEMS = listOf(
    DiscoverItem("programming/python", R.string.kesf_python, R.string.cat_programming, R.drawable.kesf_python, 6),
    DiscoverItem("programming/gradle", R.string.kesf_gradle, R.string.cat_programming, R.drawable.kesf_gradle, 2),
    DiscoverItem("resources/android/apk-nedir", R.string.kesf_apk, R.string.cat_android, R.drawable.kesf_apk, 2),
    DiscoverItem("apps/nibras-pdf", R.string.kesf_nibras_pdf, R.string.cat_app, R.drawable.kesf_nibras_pdf, null),
    DiscoverItem("programming/kotlin", R.string.kesf_kotlin, R.string.cat_programming, R.drawable.kesf_kotlin, 2),
    DiscoverItem(
        "resurslar/pdf-birlesdirmek", R.string.kesf_pdf_merge, R.string.cat_pdf, R.drawable.kesf_pdf_merge, 2,
        pathOverride = mapOf("ru" to "/ru/resources/obedinit-pdf")
    ),
    DiscoverItem("apps/nibras-arabic", R.string.kesf_nibras_arabic, R.string.cat_app, R.drawable.kesf_nibras_arabic, null),
    DiscoverItem("resources/ereb-dili/ereb-elifbasi", R.string.kesf_alphabet, R.string.cat_arabic, R.drawable.kesf_alphabet, 2),
)

@Composable
fun HomeHero(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(230.dp)) {
        Image(
            painter = painterResource(R.drawable.home_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0.0f to HomeBg.copy(alpha = 0.55f), 0.35f to Color.Transparent, 0.8f to Color.Transparent, 1f to HomeBg)
            )
        )
        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_nova_n), contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Nibras ") }
                        withStyle(SpanStyle(brush = Brush.linearGradient(listOf(Color(0xFF6FA8FF), Color(0xFFB36BFF))))) { append("Nova") }
                    },
                    fontSize = 30.sp, fontWeight = FontWeight.SemiBold
                )
                Text(stringResource(R.string.home_tagline), color = TextDim, fontSize = 13.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onGo: () -> Unit,
    engineName: String,
    isPrivate: Boolean,
    onEngineClick: () -> Unit,
    onMic: () -> Unit,
    onQr: () -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(Brush.horizontalGradient(listOf(Color(0x2A5B8CFF), Color(0x1A8E6BFF))))
            .border(1.dp, if (isPrivate) Color(0xFFB36BFF) else GlassBorder, RoundedCornerShape(29.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEngineClick) {
            if (engineName.startsWith("Google", ignoreCase = true)) {
                Image(painterResource(R.drawable.ic_brand_google), contentDescription = engineName, modifier = Modifier.size(24.dp))
            } else {
                Box(Modifier.size(26.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text(engineName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, color = TextDim, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, stringResource(R.string.t_clear_search), tint = TextDim) }
        } else {
            IconButton(onClick = onQr) { Icon(Icons.Default.QrCodeScanner, stringResource(R.string.t_scan_qr_code), tint = TextDim) }
        }
        IconButton(onClick = onMic) { Icon(Icons.Default.Mic, stringResource(R.string.home_voice_search), tint = TextDim) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickTile(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Glass).border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@DrawableRes
fun brandIconFor(url: String): Int? = when {
    url.contains("google.", true) -> R.drawable.ic_brand_google
    url.contains("youtube.", true) || url.contains("youtu.be", true) -> R.drawable.ic_brand_youtube
    url.contains("github.", true) -> R.drawable.ic_brand_github
    url.contains("nibrascode.com", true) -> R.drawable.ic_nova_n
    else -> null
}

@Composable
fun TileIcon(url: String, title: String) {
    val res = brandIconFor(url)
    if (res != null) Image(painterResource(res), contentDescription = null, modifier = Modifier.size(28.dp))
    else Text(title.take(1).uppercase(), color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
}

@Composable
fun DiscoverHeader(onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.home_discover), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onSeeAll) {
            Text(stringResource(R.string.home_see_all), color = TextDim, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = TextDim, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun DiscoverRow(items: List<DiscoverItem>, onOpen: (DiscoverItem) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(items, key = { it.path }) { item -> DiscoverCard(item) { onOpen(item) } }
    }
}

@Composable
fun DiscoverCard(item: DiscoverItem, onClick: () -> Unit) {
    Column(
        Modifier.width(168.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF111A3A))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Image(
            painterResource(item.thumb), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(92.dp)
        )
        Column(Modifier.padding(10.dp)) {
            Text(
                stringResource(item.category), color = Color(0xFF9CC0FF), fontSize = 11.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0x333D7BFF)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(item.title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2, lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (item.readMinutes != null) Icons.Default.Schedule else Icons.Default.Language, null, tint = TextDim, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (item.readMinutes != null) stringResource(R.string.home_read_min, item.readMinutes) else "nibrascode.com",
                    color = TextDim, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun HomeBottomBar(
    tabCount: Int,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onTabs: () -> Unit,
    moreMenu: @Composable (dismiss: () -> Unit) -> Unit,
    moreExpanded: Boolean,
    onMore: () -> Unit,
    onDismissMore: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF0B1433)).navigationBarsPadding().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        NavItem(Icons.Default.Home, stringResource(R.string.nav_home), selected = true) {}
        NavItem(Icons.Outlined.BookmarkBorder, stringResource(R.string.nav_bookmarks), onClick = onBookmarks)
        NavItem(Icons.Default.History, stringResource(R.string.nav_history), onClick = onHistory)
        NavItem(null, stringResource(R.string.nav_tabs), badge = tabCount, onClick = onTabs)
        Box {
            NavItem(Icons.Default.Menu, stringResource(R.string.nav_more), onClick = onMore)
            DropdownMenu(expanded = moreExpanded, onDismissRequest = onDismissMore) { moreMenu(onDismissMore) }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector?, label: String, selected: Boolean = false, badge: Int = 0, onClick: () -> Unit) {
    val tint = if (selected) Accent else TextDim
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        else Box(Modifier.size(24.dp).border(1.6.dp, tint, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Text(badge.coerceAtMost(99).toString(), color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = tint, fontSize = 11.sp, maxLines = 1)
    }
}
