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
    diceSize: Dp = 48.dp,
    isActivePlayer: Boolean = true,
    showLabel: Boolean = true,
    diceTag: String = "dice_roll_box"
) {
    val isRolling = isActivePlayer && turnPhase == TurnPhase.ROLLING
    val canRoll = isActivePlayer && turnPhase == TurnPhase.WAITING_FOR_ROLL && !isBot
    val canTapToMove = isActivePlayer && turnPhase == TurnPhase.SELECTING_TOKEN && !isBot

    val infiniteTransition = rememberInfiniteTransition(label = "dicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canRoll) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val rollRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRolling) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollRotation"
    )

    val pipSize = (diceSize.value * 0.16f).coerceIn(6f, 10f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .testTag(diceTag)
                .scale(if (canRoll) pulseScale else 1f)
                .rotate(if (isRolling) rollRotation else 0f)
                .shadow(
                    elevation = if (canRoll) 8.dp else if (isActivePlayer) 4.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isActivePlayer) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                        } else {
                            listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF))
                        }
                    )
                )
                .border(
                    width = if (canRoll || canTapToMove) 2.5.dp else if (isActivePlayer) 1.5.dp else 1.dp,
                    color = when {
                        canRoll || canTapToMove -> activeColor.primary
                        isActivePlayer -> activeColor.primary.copy(alpha = 0.6f)
                        else -> activeColor.primary.copy(alpha = 0.25f)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .size(diceSize)
                .clickable(
                    onClick = onDiceClick
                )
                .padding((diceSize.value * 0.12f).coerceIn(4f, 8f).dp)
        ) {
            DicePips(
                value = diceValue,
                pipColor = if (diceValue == 6) activeColor.primary else if (isActivePlayer) Color(0xFF1E293B) else Color(0xFF64748B),
                pipSize = pipSize
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))

            val labelText = when {
                isRolling -> "Rolling..."
                canRoll -> "ROLL!"
                canTapToMove -> "MOVE"
                isActivePlayer && isBot -> "Bot..."
                isActivePlayer -> "$diceValue"
                else -> "$diceValue"
            }

            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = if (canRoll) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (canRoll) activeColor.primary else if (isActivePlayer) activeColor.darkShade else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun DicePips(
    value: Int,
    pipColor: Color,
    modifier: Modifier = Modifier,
    pipSize: Dp = 8.dp
) {

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
