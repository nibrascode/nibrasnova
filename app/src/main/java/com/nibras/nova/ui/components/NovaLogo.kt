package com.nibras.nova.ui.components

import com.nibras.nova.R
import com.nibras.nova.core.common.L

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nibras.nova.ui.theme.NovaCyanSecondary
import com.nibras.nova.ui.theme.NovaIncognitoAccent
import com.nibras.nova.ui.theme.NovaTurquoise

@Composable
fun NovaLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,
    showSubtitle: Boolean = true,
    isIncognito: Boolean = false
) {
    val primaryAccent = if (isIncognito) NovaIncognitoAccent else NovaTurquoise
    val secondaryAccent = if (isIncognito) Color(0xFF7E22CE) else NovaCyanSecondary

    Row(
        modifier = modifier.semantics { contentDescription = L.s(R.string.t_nibras_nova_browser_logo) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val strokeWidth = size.minDimension * 0.08f
            val radius = (size.minDimension - strokeWidth) / 2f

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryAccent, secondaryAccent),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Stylized 'N' crest inside the orbital ring
            val padding = size.minDimension * 0.28f
            val left = padding
            val right = size.width - padding
            val top = padding
            val bottom = size.height - padding

            val path = Path().apply {
                moveTo(left, bottom)
                lineTo(left, top)
                lineTo(right, bottom)
                lineTo(right, top)
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(primaryAccent, Color.White),
                    start = Offset(left, top),
                    end = Offset(right, bottom)
                ),
                style = Stroke(width = strokeWidth * 1.1f, cap = StrokeCap.Round)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NIBRAS ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "NOVA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    ),
                    color = primaryAccent
                )
            }
            if (showSubtitle) {
                Text(
                    text = if (isIncognito) L.s(R.string.t_private_session_isolated) else "BY NIBRAS CODE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
