package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AbyssalPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.VoidBlack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class ArcaneParticle(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val radiusDp: Float,
    val isCyan: Boolean,
    val phase: Float
)

@Composable
fun DarkFantasyBackground(
    particlesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val particles = remember {
        List(24) { idx ->
            ArcaneParticle(
                xFraction = ((idx * 37 + 13) % 100) / 100f,
                yOffset = ((idx * 53 + 7) % 100) / 100f,
                speed = 0.4f + (idx % 5) * 0.16f,
                radiusDp = 1.4f + (idx % 4) * 0.9f,
                isCyan = idx % 3 == 0,
                phase = (idx * 0.6f)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "arcane_particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_tick"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        VoidBlack,
                        AbyssalPurple,
                        Color(0xFF140928),
                        VoidBlack
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Atmospheric nebula glow orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.12f),
                radius = size.width * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.78f),
                    radius = size.width * 0.60f
                ),
                center = Offset(size.width * 0.12f, size.height * 0.78f),
                radius = size.width * 0.60f
            )

            if (particlesEnabled) {
                particles.forEach { p ->
                    val progress = (p.yOffset - tick * p.speed + 2f) % 1f
                    val sway = sin((tick * 2 * PI + p.phase).toDouble()).toFloat() * 14.dp.toPx()
                    val cx = (p.xFraction * size.width) + sway
                    val cy = progress * size.height
                    val alpha = (sin(progress * PI).toFloat().coerceIn(0f, 1f)) * 0.55f
                    val color = (if (p.isCyan) secondary else primary).copy(alpha = alpha)
                    drawCircle(
                        color = color,
                        radius = p.radiusDp.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun GlassRuneCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    glowBorder: Boolean = true,
    containerColor: Color = GlassSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(cornerRadius)
    val border = if (glowBorder) {
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = 0.65f),
                    secondary.copy(alpha = 0.35f),
                    primary.copy(alpha = 0.25f)
                )
            )
        )
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = border,
        tonalElevation = 4.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun GhostAvatarOrb(
    isThinking: Boolean = false,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 1800 else 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (isThinking) 2.6.dp.toPx() else 1.5.dp.toPx()
            val radius = (this.size.minDimension / 2f) - strokeWidth
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primary,
                        secondary,
                        primary.copy(alpha = 0.25f),
                        secondary,
                        primary
                    )
                ),
                radius = radius,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (isThinking) {
                val rad = Math.toRadians(pulse.toDouble())
                val orbX = center.x + cos(rad).toFloat() * radius
                val orbY = center.y + sin(rad).toFloat() * radius
                drawCircle(
                    color = ElectricCyan,
                    radius = 3.dp.toPx(),
                    center = Offset(orbX, orbY)
                )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.img_ghost_avatar),
            contentDescription = "Ghost Malli AI Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size - 6.dp)
                .clip(CircleShape)
                .border(1.dp, NeonViolet.copy(alpha = 0.4f), CircleShape)
        )
    }
}

@Composable
fun ArcaneTypingIndicator(
    label: String = "Ghost Malli AI is channeling...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots_phase"
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                val active = (phase.toInt() % 3) == index
                Box(
                    modifier = Modifier
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) ElectricCyan else NeonViolet.copy(alpha = 0.45f)
                        )
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
