package com.nibras.nova.ui.home

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nibras.nova.R
import com.nibras.nova.core.prayer.PrayerPrefs
import com.nibras.nova.core.prayer.Qibla
import kotlin.math.roundToInt

/** Returns true heading in degrees (null when no sensor), plus accuracy. */
@Composable
fun rememberTrueHeading(lat: Double?, lng: Double?): Pair<Float?, Int> {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    DisposableEffect(lat, lng) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: sm?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val declination = if (lat != null && lng != null) GeomagneticField(lat.toFloat(), lng.toFloat(), 0f, System.currentTimeMillis()).declination else 0f
        val rot = FloatArray(9); val ori = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, ori)
                val mag = Math.toDegrees(ori[0].toDouble()).toFloat()
                val h = ((mag + declination) % 360 + 360) % 360
                val prev = heading
                heading = if (prev == null) h else {
                    var d = h - prev; if (d > 180) d -= 360; if (d < -180) d += 360
                    ((prev + d * 0.15f) % 360 + 360) % 360
                }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) { accuracy = a }
        }
        if (sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    return heading to accuracy
}

@Composable
fun QiblaDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val context = LocalContext.current
        val prefs = remember { PrayerPrefs(context) }
        val lat = prefs.lat; val lng = prefs.lng
        val (heading, accuracy) = rememberTrueHeading(lat, lng)
        QiblaContent(lat, lng, prefs.cityName, heading, accuracy, onDismiss)
    }
}

@Composable
fun QiblaContent(lat: Double?, lng: Double?, place: String?, heading: Float?, accuracy: Int, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(HomeBg).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pr_close), tint = Color.White) }
            Text(stringResource(R.string.qb_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        if (lat == null || lng == null) {
            Text(stringResource(R.string.qb_need_location), color = Color(0xFFB4BED8), textAlign = TextAlign.Center)
            return@Column
        }
        val bearing = Qibla.bearing(lat, lng)
        val dist = Qibla.distanceKm(lat, lng)
        Text(place ?: stringResource(R.string.pr_location), color = Color(0xFFB4BED8), fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        val rotation = if (heading != null) Qibla.arrowRotation(bearing, heading.toDouble()).toFloat() else bearing.toFloat()
        val aligned = heading != null && (rotation < 4f || rotation > 356f)
        Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
            // Dial rotates with the phone so "N" points to true north; arrow points to Qibla.
            Canvas(Modifier.fillMaxSize().rotate(if (heading != null) -heading else 0f)) {
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0x332A4BD8), Color(0x11000000))), r)
                drawCircle(Color(0x55A8C4FF), r - 4, style = Stroke(2f))
                for (i in 0 until 72) {
                    val a = Math.toRadians(i * 5.0)
                    val len = if (i % 18 == 0) 22f else if (i % 6 == 0) 14f else 7f
                    val o = Offset(center.x + (r - 8) * kotlin.math.sin(a).toFloat(), center.y - (r - 8) * kotlin.math.cos(a).toFloat())
                    val i2 = Offset(center.x + (r - 8 - len) * kotlin.math.sin(a).toFloat(), center.y - (r - 8 - len) * kotlin.math.cos(a).toFloat())
                    drawLine(if (i == 0) Color(0xFFFF5A5A) else Color(0x99FFFFFF), o, i2, strokeWidth = if (i % 18 == 0) 4f else 2f)
                }
            }
            Canvas(Modifier.size(220.dp).rotate(rotation)) {
                val c = center; val h = size.height / 2
                val p = Path().apply { moveTo(c.x, c.y - h + 6); lineTo(c.x + 22, c.y + 10); lineTo(c.x, c.y - 4); lineTo(c.x - 22, c.y + 10); close() }
                drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFD27A), Color(0xFFB36BFF))))
                drawCircle(if (aligned) Color(0xFF3DDC84) else Color(0xFFFFD27A), 9f, c)
            }
            Text("🕋", fontSize = 28.sp, modifier = Modifier.align(Alignment.TopCenter).rotate(0f))
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.qb_bearing, bearing.roundToInt()), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.qb_distance, dist.roundToInt()), color = Color(0xFFB4BED8), fontSize = 15.sp)
        Spacer(Modifier.height(16.dp))
        val hint = when {
            heading == null -> stringResource(R.string.qb_no_sensor)
            accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW -> stringResource(R.string.qb_calibrate)
            aligned -> stringResource(R.string.qb_aligned)
            else -> stringResource(R.string.qb_turn)
        }
        Text(hint, color = if (aligned) Color(0xFF3DDC84) else Color(0xFFFFD27A), textAlign = TextAlign.Center, fontSize = 14.sp)
    }
}
