package com.example.ludo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.PlayerColor
import com.example.ludo.viewmodel.TurnPhase

@Composable
fun DiceView(
    diceValue: Int,
    activeColor: PlayerColor,
    turnPhase: TurnPhase,
    isBot: Boolean,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    diceSize: Dp = 68.dp
) {
    val isRolling = turnPhase == TurnPhase.ROLLING
    val canRoll = turnPhase == TurnPhase.WAITING_FOR_ROLL && !isBot
    val canTapToMove = turnPhase == TurnPhase.SELECTING_TOKEN && !isBot

    val infiniteTransition = rememberInfiniteTransition(label = "dicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canRoll) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rollRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRolling) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollRotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag("dice_roll_box")
                .scale(if (canRoll) pulseScale else 1f)
                .rotate(if (isRolling) rollRotation else 0f)
                .shadow(elevation = if (canRoll) 10.dp else 4.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE8ECEF))
                    )
                )
                .border(
                    width = if (canRoll || canTapToMove) 3.dp else 1.5.dp,
                    color = if (canRoll || canTapToMove) activeColor.primary else Color(0xFFCFD8DC),
                    shape = RoundedCornerShape(16.dp)
                )
                .size(diceSize)
                .clickable(
                    enabled = (canRoll || canTapToMove) && !isRolling,
                    onClick = onDiceClick
                )
                .padding(10.dp)
        ) {
            DicePips(value = diceValue, pipColor = if (diceValue == 6) activeColor.primary else Color(0xFF263238))
        }

        Spacer(modifier = Modifier.height(4.dp))

        val labelText = when {
            isRolling -> "Rolling..."
            canRoll -> "Tap to Roll"
            canTapToMove -> "Tap Move"
            isBot -> "Bot Turn"
            else -> "Rolled: $diceValue"
        }

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (canRoll) FontWeight.Bold else FontWeight.Medium,
                color = if (canRoll) activeColor.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun DicePips(
    value: Int,
    pipColor: Color,
    modifier: Modifier = Modifier
) {
    val pipSize = 9.dp

    Box(modifier = modifier.fillMaxSize()) {
        when (value) {
            1 -> {
                Pip(pipColor, pipSize, Modifier.align(Alignment.Center))
            }
            2 -> {
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomEnd))
            }
            3 -> {
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.Center))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomEnd))
            }
            4 -> {
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopEnd))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomEnd))
            }
            5 -> {
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.TopEnd))
                Pip(pipColor, pipSize, Modifier.align(Alignment.Center))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomStart))
                Pip(pipColor, pipSize, Modifier.align(Alignment.BottomEnd))
            }
            6 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pip(pipColor, pipSize)
                        Pip(pipColor, pipSize)
                    }
                    Row(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pip(pipColor, pipSize)
                        Pip(pipColor, pipSize)
                    }
                    Row(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pip(pipColor, pipSize)
                        Pip(pipColor, pipSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pip(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
    )
}
