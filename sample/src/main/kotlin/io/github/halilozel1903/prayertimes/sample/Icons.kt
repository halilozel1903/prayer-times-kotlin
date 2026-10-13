package io.github.halilozel1903.prayertimes.sample

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import io.github.halilozel1903.prayertimes.Prayer
import kotlin.math.cos
import kotlin.math.sin

/* Small drawn icons, so the sample needs no icon library. */

private fun DrawScope.stroke(s: Float) = Stroke(width = s * 0.09f, cap = StrokeCap.Round)

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawCircle(color, radius = s * 0.28f, center = Offset(s * 0.42f, s * 0.42f), style = stroke(s))
        drawLine(color, Offset(s * 0.63f, s * 0.63f), Offset(s * 0.86f, s * 0.86f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        drawLine(color, Offset(s * 0.25f, s * 0.25f), Offset(s * 0.75f, s * 0.75f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.75f, s * 0.25f), Offset(s * 0.25f, s * 0.75f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** A chevron pointing left; [flip] points it right. */
@Composable
fun ChevronIcon(color: Color, modifier: Modifier = Modifier, flip: Boolean = false) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        val (tip, back) = if (flip) s * 0.64f to s * 0.4f else s * 0.36f to s * 0.6f
        drawLine(color, Offset(back, s * 0.24f), Offset(tip, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(tip, s * 0.5f), Offset(back, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun BackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.09f
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.24f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.46f, s * 0.76f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun PinIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.5f, s * 0.92f)
            cubicTo(s * 0.2f, s * 0.6f, s * 0.16f, s * 0.46f, s * 0.16f, s * 0.38f)
            cubicTo(s * 0.16f, s * 0.18f, s * 0.32f, s * 0.06f, s * 0.5f, s * 0.06f)
            cubicTo(s * 0.68f, s * 0.06f, s * 0.84f, s * 0.18f, s * 0.84f, s * 0.38f)
            cubicTo(s * 0.84f, s * 0.46f, s * 0.8f, s * 0.6f, s * 0.5f, s * 0.92f)
            close()
        }
        drawPath(path, color, style = stroke(s))
        drawCircle(color, radius = s * 0.1f, center = Offset(s * 0.5f, s * 0.38f))
    }
}

@Composable
fun ClockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.38f, center = c, style = stroke(s))
        drawLine(color, c, Offset(s * 0.5f, s * 0.28f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
        drawLine(color, c, Offset(s * 0.66f, s * 0.6f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
fun CalendarIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val st = stroke(s)
        drawRoundRect(
            color,
            topLeft = Offset(s * 0.14f, s * 0.2f),
            size = Size(s * 0.72f, s * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f),
            style = st,
        )
        drawLine(color, Offset(s * 0.14f, s * 0.4f), Offset(s * 0.86f, s * 0.4f), strokeWidth = st.width)
        drawLine(color, Offset(s * 0.34f, s * 0.1f), Offset(s * 0.34f, s * 0.26f), strokeWidth = st.width, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.66f, s * 0.1f), Offset(s * 0.66f, s * 0.26f), strokeWidth = st.width, cap = StrokeCap.Round)
        for (row in 0..1) for (col in 0..2) {
            drawCircle(color, radius = s * 0.045f, center = Offset(s * (0.32f + col * 0.18f), s * (0.55f + row * 0.17f)))
        }
    }
}

@Composable
fun CompassIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        drawCircle(color, radius = s * 0.4f, center = Offset(s / 2, s / 2), style = stroke(s))
        val needle = Path().apply {
            moveTo(s * 0.62f, s * 0.38f)
            lineTo(s * 0.44f, s * 0.44f)
            lineTo(s * 0.38f, s * 0.62f)
            lineTo(s * 0.56f, s * 0.56f)
            close()
        }
        drawPath(needle, color)
    }
}

@Composable
fun SettingsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(s / 2, s / 2)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val inner = Offset(c.x + cos(a).toFloat() * s * 0.28f, c.y + sin(a).toFloat() * s * 0.28f)
            val outer = Offset(c.x + cos(a).toFloat() * s * 0.42f, c.y + sin(a).toFloat() * s * 0.42f)
            drawLine(color, inner, outer, strokeWidth = s * 0.13f, cap = StrokeCap.Round)
        }
        drawCircle(color, radius = s * 0.27f, center = c, style = stroke(s))
        drawCircle(color, radius = s * 0.1f, center = c, style = stroke(s))
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.1f
        drawLine(color, Offset(s * 0.2f, s * 0.52f), Offset(s * 0.42f, s * 0.74f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.74f), Offset(s * 0.82f, s * 0.3f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** A glyph per time: crescent for İmsak and Yatsı, a sun on the horizon for Güneş and Akşam, a sun for Öğle and İkindi. */
@Composable
fun PrayerGlyph(prayer: Prayer, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val s = size.minDimension
        when (prayer) {
            Prayer.Fajr, Prayer.Isha -> {
                drawCircle(color, radius = s * 0.32f, center = Offset(s * 0.46f, s * 0.48f))
                drawCircle(Color.Black, radius = s * 0.28f, center = Offset(s * 0.6f, s * 0.38f), blendMode = BlendMode.Clear)
                if (prayer == Prayer.Fajr) {
                    drawLine(color, Offset(s * 0.1f, s * 0.9f), Offset(s * 0.9f, s * 0.9f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
                } else {
                    drawCircle(color, radius = s * 0.06f, center = Offset(s * 0.78f, s * 0.24f))
                }
            }
            Prayer.Sunrise, Prayer.Maghrib -> {
                val c = Offset(s * 0.5f, s * 0.7f)
                drawArc(color, 180f, 180f, useCenter = true, topLeft = Offset(c.x - s * 0.24f, c.y - s * 0.24f), size = Size(s * 0.48f, s * 0.48f))
                for (i in 0..4) {
                    val a = Math.toRadians(180.0 + i * 45.0)
                    drawLine(
                        color,
                        Offset(c.x + cos(a).toFloat() * s * 0.32f, c.y + sin(a).toFloat() * s * 0.32f),
                        Offset(c.x + cos(a).toFloat() * s * 0.42f, c.y + sin(a).toFloat() * s * 0.42f),
                        strokeWidth = s * 0.07f,
                        cap = StrokeCap.Round,
                    )
                }
                drawLine(color, Offset(s * 0.08f, s * 0.82f), Offset(s * 0.92f, s * 0.82f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
            }
            Prayer.Dhuhr, Prayer.Asr -> {
                val c = Offset(s / 2, s / 2)
                drawCircle(color, radius = s * 0.2f, center = c)
                val rays = if (prayer == Prayer.Dhuhr) 8 else 4
                for (i in 0 until rays) {
                    val a = Math.toRadians(i * 360.0 / rays + if (prayer == Prayer.Asr) 45.0 else 0.0)
                    drawLine(
                        color,
                        Offset(c.x + cos(a).toFloat() * s * 0.3f, c.y + sin(a).toFloat() * s * 0.3f),
                        Offset(c.x + cos(a).toFloat() * s * 0.42f, c.y + sin(a).toFloat() * s * 0.42f),
                        strokeWidth = s * 0.07f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
