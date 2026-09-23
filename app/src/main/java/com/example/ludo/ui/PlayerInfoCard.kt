package com.example.ludo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.Player
import com.example.ludo.viewmodel.TurnPhase

/**
 * Compact Player Profile Card ("প্লেয়ার কোট").
 * Sleek and space-efficient: displays avatar, name, rank, and token progress.
 */
@Composable
fun PlayerInfoCard(
    player: Player,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = player.color

    val infiniteTransition = rememberInfiniteTransition(label = "playerTurnPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) color.neonGlow else Color(0xFF1E293B).copy(alpha = 0.35f),
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) color.cyberPlate else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (isActive) 4.dp else 1.dp,
        modifier = modifier
            .testTag("player_card_${player.color.name.lowercase()}")
            .height(38.dp)
            .scale(pulseScale)
            .border(
                width = if (isActive) 1.5.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .shadow(if (isActive) 4.dp else 1.dp, RoundedCornerShape(10.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 3.5.dp)
                .fillMaxWidth()
        ) {
            // Player Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(color.secondary, color.darkCore)
                        )
                    )
                    .border(1.dp, Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (player.isBot) Icons.Default.Android else Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Player Name and Finished Token Dots
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                    )

                    if (player.rank != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Rank",
                                tint = Color(0xFFFFD600),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "#${player.rank}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color(0xFFFFD600)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                // Finished tokens indicators (4 dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val finishedCount = player.finishedTokensCount
                    for (i in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < finishedCount) color.neonGlow else Color(0xFF64748B)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$finishedCount/4",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) color.neonGlow else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

/**
 * Large, easy-to-tap Corner Dice Box ("কোটের নিচে বড় ছক্কা").
 * Positioned under the player card with generous size and animated pulse for easy rolling.
 */
@Composable
fun CornerDiceBox(
    player: Player,
    isActive: Boolean,
    diceValue: Int,
    turnPhase: TurnPhase,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    diceSize: Dp = 54.dp
) {
    val color = player.color

    val infiniteTransition = rememberInfiniteTransition(label = "diceStationPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive && turnPhase == TurnPhase.WAITING_FOR_ROLL) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val canRoll = isActive && turnPhase == TurnPhase.WAITING_FOR_ROLL && !player.isBot
    val canTapToMove = isActive && turnPhase == TurnPhase.SELECTING_TOKEN && !player.isBot

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 76.dp, height = 82.dp)
            .scale(if (canRoll) pulseScale else 1f)
            .shadow(
                elevation = if (canRoll) 10.dp else if (isActive) 5.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (isActive) color.neonGlow else Color.Black
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(color.cyberPlate, Color(0xFF09101A))
                    } else {
                        listOf(Color(0xFF16202C), Color(0xFF0C131D))
                    }
                )
            )
            .border(
                width = if (canRoll) 2.5.dp else if (isActive) 1.5.dp else 0.8.dp,
                color = when {
                    canRoll -> color.neonGlow
                    isActive -> color.primary.copy(alpha = 0.85f)
                    else -> Color(0xFF1E2F42)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                enabled = canRoll || canTapToMove,
                onClick = onDiceClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        DiceView(
            diceValue = diceValue,
            activeColor = color,
            turnPhase = turnPhase,
            isBot = player.isBot,
            isActivePlayer = isActive,
            showLabel = true,
            diceSize = diceSize,
            diceTag = "dice_${player.color.name.lowercase()}",
            onDiceClick = onDiceClick
        )
    }
}
