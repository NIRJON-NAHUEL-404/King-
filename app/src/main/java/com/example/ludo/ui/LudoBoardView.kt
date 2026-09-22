package com.example.ludo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ludo.model.GridPos
import com.example.ludo.model.LudoBoardCoordinates
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LudoBoardView(
    players: List<Player>,
    activePlayer: Player?,
    movableTokenIds: Set<Int>,
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tokenPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(6.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .testTag("ludo_board_canvas")
    ) {
        val boardSize = constraints.maxWidth.toFloat()
        val cellSize = boardSize / 15f

        // Collect all tokens with their computed screen coordinates
        val tokenScreenPositions = mutableListOf<TokenScreenPosition>()

        // Group tokens by cell position to handle stacking
        val tokensByGridPos = mutableMapOf<String, MutableList<Pair<Player, Token>>>()
        players.forEach { p ->
            p.tokens.forEach { t ->
                val gridPos = LudoBoardCoordinates.getTokenPosition(p.color, t.step, t.id)
                val key = "${gridPos.row.toInt()}_${gridPos.col.toInt()}_${t.isInYard}_${t.isFinished}"
                tokensByGridPos.getOrPut(key) { mutableListOf() }.add(Pair(p, t))
            }
        }

        tokensByGridPos.forEach { (_, list) ->
            val count = list.size
            list.forEachIndexed { index, (player, token) ->
                val basePos = LudoBoardCoordinates.getTokenPosition(player.color, token.step, token.id)
                var offsetX = 0f
                var offsetY = 0f

                if (count > 1 && !token.isInYard && !token.isFinished) {
                    val radius = cellSize * 0.22f
                    val angle = (2 * Math.PI * index / count).toFloat()
                    offsetX = cos(angle) * radius
                    offsetY = sin(angle) * radius
                }

                val px = (basePos.col + 0.5f) * cellSize + offsetX
                val py = (basePos.row + 0.5f) * cellSize + offsetY
                val isMovable = player.color == activePlayer?.color && movableTokenIds.contains(token.id)

                tokenScreenPositions.add(
                    TokenScreenPosition(
                        token = token,
                        player = player,
                        center = Offset(px, py),
                        isMovable = isMovable
                    )
                )
            }
        }

        Canvas(
            modifier = Modifier
                .aspectRatio(1f)
                .pointerInput(tokenScreenPositions, activePlayer) {
                    detectTapGestures { tapOffset ->
                        // Find closest movable token first, then any token of active player
                        var tappedToken: TokenScreenPosition? = null
                        var minDistance = Float.MAX_VALUE
                        val hitRadius = cellSize * 0.75f

                        for (pos in tokenScreenPositions) {
                            val dist = hypot(tapOffset.x - pos.center.x, tapOffset.y - pos.center.y)
                            if (dist < hitRadius && dist < minDistance) {
                                if (pos.isMovable) {
                                    tappedToken = pos
                                    minDistance = dist
                                } else if (tappedToken == null && pos.player.color == activePlayer?.color) {
                                    tappedToken = pos
                                    minDistance = dist
                                }
                            }
                        }

                        tappedToken?.let {
                            onTokenClicked(it.token.id)
                        }
                    }
                }
        ) {
            drawLudoBoard(cellSize)
            drawTokens(tokenScreenPositions, cellSize, pulseGlow)
        }
    }
}

private data class TokenScreenPosition(
    val token: Token,
    val player: Player,
    val center: Offset,
    val isMovable: Boolean
)

private fun DrawScope.drawLudoBoard(cellSize: Float) {
    // 1. Board background
    drawRect(color = Color(0xFFFAF7EE), size = size)

    val boardGridColor = Color(0xFFDCD6C8)
    val starColor = Color(0xFFFFB300)

    // 2. Draw 4 Yards (6x6 cells each)
    drawYard(0f, 0f, cellSize, PlayerColor.RED)
    drawYard(9 * cellSize, 0f, cellSize, PlayerColor.GREEN)
    drawYard(9 * cellSize, 9 * cellSize, cellSize, PlayerColor.YELLOW)
    drawYard(0f, 9 * cellSize, cellSize, PlayerColor.BLUE)

    // 3. Draw Track cells grid (horizontal & vertical arms)
    // Vertical corridor (Cols 6, 7, 8; Rows 0..5 and 9..14)
    for (r in 0..5) {
        for (c in 6..8) {
            drawTrackCell(r, c, cellSize, boardGridColor)
        }
    }
    for (r in 9..14) {
        for (c in 6..8) {
            drawTrackCell(r, c, cellSize, boardGridColor)
        }
    }

    // Horizontal corridor (Rows 6, 7, 8; Cols 0..5 and 9..14)
    for (r in 6..8) {
        for (c in 0..5) {
            drawTrackCell(r, c, cellSize, boardGridColor)
        }
    }
    for (r in 6..8) {
        for (c in 9..14) {
            drawTrackCell(r, c, cellSize, boardGridColor)
        }
    }

    // 4. Color the Home Corridors
    // Red Home Column: Row 7, Cols 1..5
    for (c in 1..5) {
        drawRect(
            color = PlayerColor.RED.primary,
            topLeft = Offset(c * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(c * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }

    // Green Home Column: Col 7, Rows 1..5
    for (r in 1..5) {
        drawRect(
            color = PlayerColor.GREEN.primary,
            topLeft = Offset(7 * cellSize, r * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(7 * cellSize, r * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }

    // Yellow Home Column: Row 7, Cols 9..13
    for (c in 9..13) {
        drawRect(
            color = PlayerColor.YELLOW.primary,
            topLeft = Offset(c * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(c * cellSize, 7 * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }

    // Blue Home Column: Col 7, Rows 9..13
    for (r in 9..13) {
        drawRect(
            color = PlayerColor.BLUE.primary,
            topLeft = Offset(7 * cellSize, r * cellSize),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(7 * cellSize, r * cellSize),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }

    // 5. Color the Start Cells
    drawStartCell(6, 1, cellSize, PlayerColor.RED)
    drawStartCell(1, 8, cellSize, PlayerColor.GREEN)
    drawStartCell(8, 13, cellSize, PlayerColor.YELLOW)
    drawStartCell(13, 6, cellSize, PlayerColor.BLUE)

    // 6. Draw Safe Stars
    val starCoords = listOf(Pair(2, 6), Pair(6, 12), Pair(12, 8), Pair(8, 2))
    starCoords.forEach { (r, c) ->
        drawStar(
            center = Offset((c + 0.5f) * cellSize, (r + 0.5f) * cellSize),
            radius = cellSize * 0.38f,
            color = starColor
        )
    }

    // 7. Center Home Triangles (Rows 6..8, Cols 6..8 = 3x3 cells)
    val centerTopLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerSize = 3 * cellSize
    val centerPoint = Offset(centerTopLeft.x + centerSize / 2f, centerTopLeft.y + centerSize / 2f)

    // Red Left Triangle
    val redPath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(redPath, PlayerColor.RED.primary)

    // Green Top Triangle
    val greenPath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y)
        close()
    }
    drawPath(greenPath, PlayerColor.GREEN.primary)

    // Yellow Right Triangle
    val yellowPath = Path().apply {
        moveTo(centerTopLeft.x + centerSize, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(yellowPath, PlayerColor.YELLOW.primary)

    // Blue Bottom Triangle
    val bluePath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y + centerSize)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(bluePath, PlayerColor.BLUE.primary)

    // Center Gold Emblem
    drawCircle(color = Color(0xFFFFD54F), radius = cellSize * 0.45f, center = centerPoint)
    drawCircle(color = Color(0xFFFFA000), radius = cellSize * 0.45f, center = centerPoint, style = Stroke(width = 2f))
    drawStar(center = centerPoint, radius = cellSize * 0.28f, color = Color(0xFF6A1B9A))

    // Outer board border
    drawRect(
        color = Color(0xFF37474F),
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawTrackCell(row: Int, col: Int, cellSize: Float, borderColor: Color) {
    val topLeft = Offset(col * cellSize, row * cellSize)
    drawRect(
        color = Color(0xFFFFFFFF),
        topLeft = topLeft,
        size = Size(cellSize, cellSize)
    )
    drawRect(
        color = borderColor,
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawStartCell(row: Int, col: Int, cellSize: Float, color: PlayerColor) {
    val topLeft = Offset(col * cellSize, row * cellSize)
    drawRect(
        color = color.primary,
        topLeft = topLeft,
        size = Size(cellSize, cellSize)
    )
    drawRect(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = topLeft,
        size = Size(cellSize, cellSize),
        style = Stroke(width = 1.5f)
    )
    // Inner arrow / circle
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.24f,
        center = Offset((col + 0.5f) * cellSize, (row + 0.5f) * cellSize)
    )
    drawCircle(
        color = color.primary,
        radius = cellSize * 0.16f,
        center = Offset((col + 0.5f) * cellSize, (row + 0.5f) * cellSize)
    )
}

private fun DrawScope.drawYard(x: Float, y: Float, cellSize: Float, color: PlayerColor) {
    val yardSize = 6 * cellSize

    // Yard background
    drawRect(
        color = color.primary,
        topLeft = Offset(x, y),
        size = Size(yardSize, yardSize)
    )

    // Inner White Base Plate
    val inset = cellSize * 0.8f
    val plateSize = yardSize - 2 * inset
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + inset, y + inset),
        size = Size(plateSize, plateSize),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.6f, cellSize * 0.6f)
    )

    // 4 Token Yard Slots
    val slotOffsets = listOf(
        Offset(x + 1.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 1.5f * cellSize, y + 4.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 4.5f * cellSize)
    )

    slotOffsets.forEach { slotCenter ->
        drawCircle(
            color = color.lightContainer,
            radius = cellSize * 0.65f,
            center = slotCenter
        )
        drawCircle(
            color = color.primary,
            radius = cellSize * 0.65f,
            center = slotCenter,
            style = Stroke(width = 2.5f)
        )
    }

    // Yard border
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(x, y),
        size = Size(yardSize, yardSize),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.45f
    val points = 5
    var angle = -Math.PI / 2

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val x = (center.x + cos(angle) * r).toFloat()
        val y = (center.y + sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += Math.PI / points
    }
    path.close()
    drawPath(path, color, style = Fill)
}

private fun DrawScope.drawTokens(
    tokens: List<TokenScreenPosition>,
    cellSize: Float,
    pulseGlow: Float
) {
    val baseRadius = cellSize * 0.38f

    tokens.forEach { item ->
        val center = item.center

        // Pulsing glow for movable tokens
        if (item.isMovable) {
            val haloRadius = baseRadius * (1.1f * pulseGlow)
            drawCircle(
                color = item.player.color.primary.copy(alpha = 0.4f),
                radius = haloRadius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = haloRadius * 0.9f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        // Token Drop Shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = baseRadius * 0.95f,
            center = Offset(center.x + 2f, center.y + 4f)
        )

        // Outer Token Rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(item.player.color.secondary, item.player.color.darkShade),
                center = Offset(center.x - baseRadius * 0.3f, center.y - baseRadius * 0.3f),
                radius = baseRadius * 1.4f
            ),
            radius = baseRadius,
            center = center
        )

        // Dark Rim Border
        drawCircle(
            color = Color.White,
            radius = baseRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Inner Crown/Pin Button
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, item.player.color.lightContainer),
                center = center,
                radius = baseRadius * 0.6f
            ),
            radius = baseRadius * 0.52f,
            center = center
        )

        drawCircle(
            color = item.player.color.primary,
            radius = baseRadius * 0.3f,
            center = center
        )
    }
}
