package io.github.halilozel1903.prayertimes.sample

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.prayertimes.Province
import io.github.halilozel1903.prayertimes.Qibla
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Qibla from the center of [province], computed offline with `Qibla.bearing` and checked against Aladhan's
 * `/qibla` endpoint (live). The dial is drawn with true north at the top. When the device has a rotation vector
 * sensor and [allowCompass] is set, the user can turn the compass on and the dial follows the phone's heading,
 * corrected for magnetic declination. Without a sensor (for example on an emulator) the screen says so and shows
 * the computed bearing only; it never pretends to be a live compass.
 */
@Composable
fun QiblaScreen(province: Province, repository: PrayerRepository, allowCompass: Boolean) {
    val context = LocalContext.current
    val locale = appLocale()
    val bearing = remember(province) { Qibla.bearing(province) }
    val distance = remember(province) { Qibla.distanceKm(province.latitude, province.longitude) }

    var reload by remember { mutableIntStateOf(0) }
    val aladhan by produceState<Load<Double>>(Load.Loading, province, reload) {
        value = Load.Loading
        value = load { repository.qibla(province) }
    }
    ReportLoad(aladhan)

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val hasSensor = rotationSensor != null
    var compassOn by rememberSaveable { mutableStateOf(allowCompass && hasSensor) }
    var heading by remember { mutableFloatStateOf(0f) }
    val declination = remember(province) {
        GeomagneticField(province.latitude.toFloat(), province.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
    }

    if (compassOn && rotationSensor != null) {
        DisposableEffect(rotationSensor, declination) {
            val matrix = FloatArray(9)
            val orientation = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, orientation)
                    val magnetic = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    heading = ((magnetic + declination) % 360f + 360f) % 360f
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
    val dialRotation = if (compassOn && hasSensor) -heading else 0f

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.qibla_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.qibla_from, province.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            QiblaDial(
                bearing = bearing,
                dialRotation = dialRotation,
                modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f).padding(vertical = 8.dp),
            )

            Text(
                "${bearing.formatDegrees(locale)}°",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.qibla_bearing, bearing.formatDegrees(locale), Qibla.compassPoint(bearing)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.qibla_distance, String.format(locale, "%,d", distance.roundToInt())),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (val state = aladhan) {
                Load.Loading -> Text("…", style = MaterialTheme.typography.bodyMedium)
                is Load.Failed -> ErrorState(state, onRetry = { reload++ }, modifier = Modifier.size(width = 320.dp, height = 220.dp))
                is Load.Ready -> Text(
                    stringResource(R.string.qibla_aladhan, state.value.formatDegrees(locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasSensor && allowCompass) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.qibla_use_compass),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = compassOn, onCheckedChange = { compassOn = it })
                        }
                    }
                    Text(
                        when {
                            !hasSensor -> stringResource(R.string.qibla_no_sensor, bearing.formatDegrees(locale))
                            compassOn -> stringResource(R.string.qibla_live, declination.toDouble().formatDegrees(locale))
                            else -> stringResource(R.string.qibla_static, bearing.formatDegrees(locale))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

/** A compass card with true north at the top (turned by [dialRotation]) and an arrow to the Kaaba. */
@Composable
private fun QiblaDial(bearing: Double, dialRotation: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labels = listOf(
        stringResource(R.string.north_short),
        stringResource(R.string.east_short),
        stringResource(R.string.south_short),
        stringResource(R.string.west_short),
    )
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(colors.surfaceContainerHigh, radius = radius, center = center)
        drawCircle(colors.outlineVariant, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

        rotate(dialRotation, center) {
            for (degree in 0 until 360 step 15) {
                val major = degree % 90 == 0
                val angle = Math.toRadians(degree.toDouble() - 90.0)
                val outer = radius * 0.94f
                val inner = radius * if (major) 0.80f else 0.87f
                drawLine(
                    if (major) colors.onSurface else colors.outline,
                    Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                    Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                    strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            labels.forEachIndexed { index, label ->
                val angle = Math.toRadians(index * 90.0 - 90.0)
                val layout = measurer.measure(
                    label,
                    TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (index == 0) colors.error else colors.onSurfaceVariant,
                    ),
                )
                val r = radius * 0.66f
                drawText(
                    layout,
                    topLeft = Offset(
                        center.x + cos(angle).toFloat() * r - layout.size.width / 2f,
                        center.y + sin(angle).toFloat() * r - layout.size.height / 2f,
                    ),
                )
            }

            // The arrow to the Kaaba.
            rotate(bearing.toFloat(), center) {
                val tip = Offset(center.x, center.y - radius * 0.78f)
                drawLine(colors.primary, center, Offset(center.x, tip.y + 18.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - 14.dp.toPx(), tip.y + 26.dp.toPx())
                    lineTo(tip.x + 14.dp.toPx(), tip.y + 26.dp.toPx())
                    close()
                }
                drawPath(head, colors.primary)
                val box = 14.dp.toPx()
                drawRect(colors.onSurface, topLeft = Offset(tip.x - box / 2, tip.y - box - 6.dp.toPx()), size = Size(box, box))
                drawLine(
                    colors.secondary,
                    Offset(tip.x - box / 2, tip.y - box * 0.7f - 6.dp.toPx()),
                    Offset(tip.x + box / 2, tip.y - box * 0.7f - 6.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        drawCircle(colors.primary, radius = 7.dp.toPx(), center = center)
    }
}
