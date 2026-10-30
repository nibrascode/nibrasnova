package com.nibras.nova.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nibras.nova.ui.theme.NovaHttpsVerifiedGreen
import com.nibras.nova.ui.theme.NovaIncognitoAccent
import com.nibras.nova.ui.theme.NovaTurquoise
import com.nibras.nova.ui.theme.NovaWarningAmber
import kotlin.math.abs

@Composable
fun OmniAddressBar(
    currentUrl: String,
    pageTitle: String,
    isLoading: Boolean,
    loadingProgress: Int,
    isPrivate: Boolean,
    isReaderAvailable: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUrlSubmitted: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onReloadOrStopClick: () -> Unit,
    onReaderModeClick: () -> Unit,
    onQrScannerClick: () -> Unit,
    onSecurityBadgeClick: () -> Unit,
    onSwipePreviousTab: () -> Unit = {},
    onSwipeNextTab: () -> Unit = {},
    onSwipeDownTabOverview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var textValue by remember(currentUrl) {
        mutableStateOf(if (currentUrl == "nova://home") "" else currentUrl)
    }

    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentUrl, isFocused) {
        if (!isFocused) {
            textValue = if (currentUrl == "nova://home") "" else currentUrl
        }
    }

    val isHttps = currentUrl.startsWith("https://", ignoreCase = true)
    val isHome = currentUrl == "nova://home" || currentUrl.isBlank()
    val borderColor = when {
        isFocused && isPrivate -> NovaIncognitoAccent
        isFocused -> NovaTurquoise
        isPrivate -> NovaIncognitoAccent.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outline
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .pointerInput(isFocused) {
                    if (!isFocused) {
                        detectDragGestures(
                            onDragStart = {
                                dragAccumX = 0f
                                dragAccumY = 0f
                            },
                            onDragEnd = {
                                if (abs(dragAccumX) > 90f && abs(dragAccumX) > abs(dragAccumY)) {
                                    if (dragAccumX > 0) onSwipePreviousTab() else onSwipeNextTab()
                                } else if (dragAccumY > 80f && abs(dragAccumY) > abs(dragAccumX)) {
                                    onSwipeDownTabOverview()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumX += dragAmount.x
                                dragAccumY += dragAmount.y
                            }
                        )
                    }
                }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSecurityBadgeClick,
                modifier = Modifier.size(36.dp)
            ) {
                when {
                    isHome || isFocused -> Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search or enter URL",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    isHttps -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure HTTPS Connection",
                        tint = NovaHttpsVerifiedGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Unencrypted HTTP Connection",
                        tint = NovaWarningAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isFocused) {
                        focusRequester.requestFocus()
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        onQueryChanged(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (!state.isFocused) {
                                onQueryChanged("")
                            }
                        },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (textValue.isNotBlank()) {
                                onUrlSubmitted(textValue.trim())
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (textValue.isEmpty() && !isFocused) {
                            Text(
                                text = if (!isHome && pageTitle.isNotBlank()) pageTitle else "Search or enter address…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                )
            }

            if (isFocused && textValue.isNotEmpty()) {
                IconButton(
                    onClick = {
                        textValue = ""
                        onQueryChanged("")
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                if (isReaderAvailable && !isHome) {
                    IconButton(
                        onClick = onReaderModeClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Open Reader Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onQrScannerClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR Code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (!isHome) {
                    IconButton(
                        onClick = onReloadOrStopClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Stop loading" else "Reload page",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        if (isLoading && loadingProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = if (isPrivate) NovaIncognitoAccent else NovaTurquoise,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
