package com.example.ludo.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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

/**
 * High-tech Digital Sci-Fi / Cyber Ludo Board.
 * Features:
 * - Obsidian holo-chassis with circuit traces and cybernetic corner brackets
 * - Quantum launch bays (docking stations) with reticle markings & neon energy containment
 * - Holographic glass track pads with illuminated neon energy corridors
 * - Radiant Quantum Singularity Reactor in the center with rotating energy arcs
 * - Levitation cyber-drone tokens with high-specular plasma cores and targeting reticles
 */
@Composable
fun LudoBoardView(
    players: List<Player>,
    activePlayer: Player?,
    movableTokenIds: Set<Int>,
    onTokenClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sciFiBoardAnim")

    // Pulsing energy glow for movable tokens and active systems
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Rotating holographic reactor core in center
    val reactorRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactorRotation"
    )

    // Secondary subtle reverse rotation for energy shield ring
    val shieldRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shieldRotation"
    )

    // Animated continuous rotation for the energy vortex spinning under movable tokens
    val tokenSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tokenSpinAngle"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(1.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF00E5FF))
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
            drawSciFiLudoBoard(cellSize, reactorRotation, shieldRotation)
            drawSciFiTokens(tokenScreenPositions, cellSize, pulseGlow, tokenSpinAngle)
        }
    }
}

private data class TokenScreenPosition(
    val token: Token,
    val player: Player,
    val center: Offset,
    val isMovable: Boolean
)

/**
 * Draws the master Digital Sci-Fi Holo-Board
 */
private fun DrawScope.drawSciFiLudoBoard(
    cellSize: Float,
    reactorRotation: Float,
    shieldRotation: Float
) {
    // 1. Deep Space Cyber Chassis Background
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0F1A2A), Color(0xFF070D16), Color(0xFF03070C)),
            center = center,
            radius = size.width * 0.75f
        ),
        size = size
    )

    // 2. Faint Digital Matrix / Circuit Grid across entire background
    drawCircuitBackgroundGrid(cellSize)

    // 3. Draw 4 Cyber Command Hubs (Yards)
    drawCyberYard(0f, 0f, cellSize, PlayerColor.RED)
    drawCyberYard(9 * cellSize, 0f, cellSize, PlayerColor.GREEN)
    drawCyberYard(9 * cellSize, 9 * cellSize, cellSize, PlayerColor.YELLOW)
    drawCyberYard(0f, 9 * cellSize, cellSize, PlayerColor.BLUE)

    // 4. Draw Track Cells Grid (Horizontal & Vertical Arms)
    val trackBorderColor = Color(0xFFB0BEC5)
    val trackDotColor = Color(0xFF90A4AE)

    // Vertical corridors
    for (r in 0..5) {
        for (c in 6..8) {
            drawSciFiTrackCell(r, c, cellSize, trackBorderColor, trackDotColor)
        }
    }
    for (r in 9..14) {
        for (c in 6..8) {
            drawSciFiTrackCell(r, c, cellSize, trackBorderColor, trackDotColor)
        }
    }

    // Horizontal corridors
    for (r in 6..8) {
        for (c in 0..5) {
            drawSciFiTrackCell(r, c, cellSize, trackBorderColor, trackDotColor)
        }
    }
    for (r in 6..8) {
        for (c in 9..14) {
            drawSciFiTrackCell(r, c, cellSize, trackBorderColor, trackDotColor)
        }
    }

    // 5. Draw Glowing Home Warp Corridors (Laser Runways)
    drawCyberHomeCorridor(PlayerColor.RED, cellSize)
    drawCyberHomeCorridor(PlayerColor.GREEN, cellSize)
    drawCyberHomeCorridor(PlayerColor.YELLOW, cellSize)
    drawCyberHomeCorridor(PlayerColor.BLUE, cellSize)

    // 6. Draw Glowing Launch / Spawn Portals (Start Cells)
    drawSciFiStartCell(6, 1, cellSize, PlayerColor.RED, directionAngle = 0f)      // Moves Right
    drawSciFiStartCell(1, 8, cellSize, PlayerColor.GREEN, directionAngle = 90f)    // Moves Down
    drawSciFiStartCell(8, 13, cellSize, PlayerColor.YELLOW, directionAngle = 180f) // Moves Left
    drawSciFiStartCell(13, 6, cellSize, PlayerColor.BLUE, directionAngle = 270f)   // Moves Up

    // 7. Draw Holographic Safe Stars (Shield Checkpoints)
    val safeStars = listOf(
        Pair(2, 6),
        Pair(6, 12),
        Pair(12, 8),
        Pair(8, 2)
    )
    safeStars.forEach { (r, c) ->
        drawHoloShieldStar(
            center = Offset((c + 0.5f) * cellSize, (r + 0.5f) * cellSize),
            radius = cellSize * 0.40f,
            shieldRotation = shieldRotation
        )
    }

    // 8. Draw Central Quantum Singularity Reactor (Finish Hub)
    drawQuantumSingularityCore(cellSize, reactorRotation)

    // 9. Outer Sci-Fi Holo-Chassis Frame & Tech Brackets
    drawSciFiFrame(cellSize)
}

/**
 * Faint digital matrix traces
 */
private fun DrawScope.drawCircuitBackgroundGrid(cellSize: Float) {
    val lineCol = Color(0xFF142436).copy(alpha = 0.35f)
    for (i in 1..14) {
        val p = i * cellSize
        drawLine(
            color = lineCol,
            start = Offset(p, 0f),
            end = Offset(p, size.height),
            strokeWidth = 0.75f
        )
        drawLine(
            color = lineCol,
            start = Offset(0f, p),
            end = Offset(size.width, p),
            strokeWidth = 0.75f
        )
    }
}

/**
 * Sleek Luminous Cyber White Crystal Track Pad
 */
private fun DrawScope.drawSciFiTrackCell(
    row: Int,
    col: Int,
    cellSize: Float,
    borderColor: Color,
    dotColor: Color
) {
    val topLeft = Offset(col * cellSize, row * cellSize)
    val pad = 1.0f

    // Luminous Cyber White Tile
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFF3F8)),
            startY = topLeft.y,
            endY = topLeft.y + cellSize
        ),
        topLeft = Offset(topLeft.x + pad, topLeft.y + pad),
        size = Size(cellSize - 2 * pad, cellSize - 2 * pad),
        cornerRadius = CornerRadius(cellSize * 0.14f, cellSize * 0.14f)
    )

    // Top-edge subtle glassy specular sheen
    drawLine(
        color = Color.White,
        start = Offset(topLeft.x + pad + 2f, topLeft.y + pad + 1f),
        end = Offset(topLeft.x + cellSize - pad - 2f, topLeft.y + pad + 1f),
        strokeWidth = 1f
    )

    // Cyber border
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(topLeft.x + pad, topLeft.y + pad),
        size = Size(cellSize - 2 * pad, cellSize - 2 * pad),
        cornerRadius = CornerRadius(cellSize * 0.14f, cellSize * 0.14f),
        style = Stroke(width = 1.2f)
    )

    // Center tech micro-circuit crosshair
    val cx = (col + 0.5f) * cellSize
    val cy = (row + 0.5f) * cellSize
    val tick = cellSize * 0.08f
    drawLine(color = dotColor, start = Offset(cx - tick, cy), end = Offset(cx + tick, cy), strokeWidth = 1.2f)
    drawLine(color = dotColor, start = Offset(cx, cy - tick), end = Offset(cx, cy + tick), strokeWidth = 1.2f)
}

/**
 * Cyber Yard / Quantum Docking Hub
 */
private fun DrawScope.drawCyberYard(
    x: Float,
    y: Float,
    cellSize: Float,
    color: PlayerColor
) {
    val yardSize = 6 * cellSize
    val margin = 2f

    // 1. Dark Armor Outer Plate
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(color.darkCore, Color(0xFF060B12), color.cyberPlate),
            start = Offset(x, y),
            end = Offset(x + yardSize, y + yardSize)
        ),
        topLeft = Offset(x + margin, y + margin),
        size = Size(yardSize - 2 * margin, yardSize - 2 * margin),
        cornerRadius = CornerRadius(cellSize * 0.35f, cellSize * 0.35f)
    )

    // 2. Glowing Neon Edge Containment Rim
    drawRoundRect(
        color = color.primary.copy(alpha = 0.45f),
        topLeft = Offset(x + margin, y + margin),
        size = Size(yardSize - 2 * margin, yardSize - 2 * margin),
        cornerRadius = CornerRadius(cellSize * 0.35f, cellSize * 0.35f),
        style = Stroke(width = 1.5f)
    )

    // 3. Inner Cyber Base Plate
    val inset = cellSize * 0.75f
    val plateSize = yardSize - 2 * inset
    val plateTopLeft = Offset(x + inset, y + inset)

    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.cyberPlate.copy(alpha = 0.95f), color.darkCore, Color(0xFF04080E)),
            center = Offset(plateTopLeft.x + plateSize / 2f, plateTopLeft.y + plateSize / 2f),
            radius = plateSize * 0.7f
        ),
        topLeft = plateTopLeft,
        size = Size(plateSize, plateSize),
        cornerRadius = CornerRadius(cellSize * 0.3f, cellSize * 0.3f)
    )

    // Inner Glowing Neon Rim
    drawRoundRect(
        color = color.primary.copy(alpha = 0.75f),
        topLeft = plateTopLeft,
        size = Size(plateSize, plateSize),
        cornerRadius = CornerRadius(cellSize * 0.3f, cellSize * 0.3f),
        style = Stroke(width = 1.5f)
    )

    // Center Holographic Radar Ring in the Yard
    val yardCenter = Offset(x + yardSize / 2f, y + yardSize / 2f)
    drawCircle(
        color = color.primary.copy(alpha = 0.15f),
        radius = cellSize * 1.6f,
        center = yardCenter
    )
    drawCircle(
        color = color.primary.copy(alpha = 0.35f),
        radius = cellSize * 1.6f,
        center = yardCenter,
        style = Stroke(
            width = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellSize * 0.2f, cellSize * 0.15f))
        )
    )
    drawCircle(
        color = color.neonGlow.copy(alpha = 0.5f),
        radius = cellSize * 0.5f,
        center = yardCenter,
        style = Stroke(width = 1f)
    )

    // Center Tech Cross
    val crossLen = cellSize * 0.4f
    drawLine(
        color = color.neonGlow.copy(alpha = 0.6f),
        start = Offset(yardCenter.x - crossLen, yardCenter.y),
        end = Offset(yardCenter.x + crossLen, yardCenter.y),
        strokeWidth = 1.2f
    )
    drawLine(
        color = color.neonGlow.copy(alpha = 0.6f),
        start = Offset(yardCenter.x, yardCenter.y - crossLen),
        end = Offset(yardCenter.x, yardCenter.y + crossLen),
        strokeWidth = 1.2f
    )

    // 4. The 4 Token Spawn Bays (Magnetic Docking Pods)
    val slotOffsets = listOf(
        Offset(x + 1.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 1.5f * cellSize, y + 4.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 4.5f * cellSize)
    )

    val padRadius = cellSize * 0.72f
    slotOffsets.forEach { slotCenter ->
        // Recessed Docking Well
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF03060B), color.darkCore, color.cyberPlate),
                center = slotCenter,
                radius = padRadius
            ),
            radius = padRadius,
            center = slotCenter
        )

        // Outer Neon Containment Ring
        drawCircle(
            color = color.primary.copy(alpha = 0.85f),
            radius = padRadius,
            center = slotCenter,
            style = Stroke(width = 2f)
        )

        // Inner Power Coil Ring
        drawCircle(
            color = color.neonGlow.copy(alpha = 0.4f),
            radius = padRadius * 0.75f,
            center = slotCenter,
            style = Stroke(width = 1f)
        )

        // Docking Reticle Crosshairs
        val tick = padRadius * 0.35f
        drawLine(
            color = color.neonGlow.copy(alpha = 0.5f),
            start = Offset(slotCenter.x - tick, slotCenter.y),
            end = Offset(slotCenter.x + tick, slotCenter.y),
            strokeWidth = 1f
        )
        drawLine(
            color = color.neonGlow.copy(alpha = 0.5f),
            start = Offset(slotCenter.x, slotCenter.y - tick),
            end = Offset(slotCenter.x, slotCenter.y + tick),
            strokeWidth = 1f
        )
    }

    // Corner Sci-Fi Tech Brackets on Yard perimeter
    drawCornerBrackets(Offset(x + margin, y + margin), yardSize - 2 * margin, color.neonGlow, cellSize * 0.3f)
}

/**
 * Glowing Neon Laser Runways (Home Corridors)
 */
private fun DrawScope.drawCyberHomeCorridor(color: PlayerColor, cellSize: Float) {
    val pad = 1f
    when (color) {
        PlayerColor.RED -> {
            // Row 7, Cols 1..5 (Moving Right ->)
            for (c in 1..5) {
                val tX = c * cellSize + pad
                val tY = 7 * cellSize + pad
                drawCyberRunwayCell(tX, tY, cellSize - 2 * pad, color, angle = 0f, stepIndex = c)
            }
        }
        PlayerColor.GREEN -> {
            // Col 7, Rows 1..5 (Moving Down v)
            for (r in 1..5) {
                val tX = 7 * cellSize + pad
                val tY = r * cellSize + pad
                drawCyberRunwayCell(tX, tY, cellSize - 2 * pad, color, angle = 90f, stepIndex = r)
            }
        }
        PlayerColor.YELLOW -> {
            // Row 7, Cols 9..13 (Moving Left <-)
            for (c in 9..13) {
                val tX = c * cellSize + pad
                val tY = 7 * cellSize + pad
                drawCyberRunwayCell(tX, tY, cellSize - 2 * pad, color, angle = 180f, stepIndex = 14 - c)
            }
        }
        PlayerColor.BLUE -> {
            // Col 7, Rows 9..13 (Moving Up ^)
            for (r in 9..13) {
                val tX = 7 * cellSize + pad
                val tY = r * cellSize + pad
                drawCyberRunwayCell(tX, tY, cellSize - 2 * pad, color, angle = 270f, stepIndex = 14 - r)
            }
        }
    }
}

/**
 * Individual cell on the home acceleration corridor
 */
private fun DrawScope.drawCyberRunwayCell(
    x: Float,
    y: Float,
    size: Float,
    color: PlayerColor,
    angle: Float,
    stepIndex: Int
) {
    val alphaFactor = 0.5f + (stepIndex / 5f) * 0.45f

    // Glowing Neon Glass Base
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.primary.copy(alpha = alphaFactor * 0.75f),
                color.darkCore.copy(alpha = 0.9f)
            ),
            startY = y,
            endY = y + size
        ),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(size * 0.15f, size * 0.15f)
    )

    // Radiant Neon Border
    drawRoundRect(
        color = color.neonGlow.copy(alpha = alphaFactor),
        topLeft = Offset(x, y),
        size = Size(size, size),
        cornerRadius = CornerRadius(size * 0.15f, size * 0.15f),
        style = Stroke(width = 1.5f)
    )

    // Forward Acceleration Chevron (>>>)
    val cx = x + size / 2f
    val cy = y + size / 2f
    val rad = Math.toRadians(angle.toDouble())
    val forward = Offset(cos(rad).toFloat(), sin(rad).toFloat())
    val perp = Offset(-sin(rad).toFloat(), cos(rad).toFloat())

    val chevronPath = Path().apply {
        val tip = Offset(cx + forward.x * size * 0.28f, cy + forward.y * size * 0.28f)
        val wing1 = Offset(cx - forward.x * size * 0.18f + perp.x * size * 0.22f, cy - forward.y * size * 0.18f + perp.y * size * 0.22f)
        val wing2 = Offset(cx - forward.x * size * 0.18f - perp.x * size * 0.22f, cy - forward.y * size * 0.18f - perp.y * size * 0.22f)
        moveTo(wing1.x, wing1.y)
        lineTo(tip.x, tip.y)
        lineTo(wing2.x, wing2.y)
    }

    drawPath(
        path = chevronPath,
        color = Color.White.copy(alpha = 0.9f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
}

/**
 * Launch Gate / Start Portal
 */
private fun DrawScope.drawSciFiStartCell(
    row: Int,
    col: Int,
    cellSize: Float,
    color: PlayerColor,
    directionAngle: Float
) {
    val pad = 1f
    val topLeft = Offset(col * cellSize + pad, row * cellSize + pad)
    val size = cellSize - 2 * pad

    // Energy portal pad
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.primary, color.darkCore),
            center = Offset(topLeft.x + size / 2f, topLeft.y + size / 2f),
            radius = size * 0.8f
        ),
        topLeft = topLeft,
        size = Size(size, size),
        cornerRadius = CornerRadius(size * 0.18f, size * 0.18f)
    )

    // Pulsing glowing border
    drawRoundRect(
        color = color.neonGlow,
        topLeft = topLeft,
        size = Size(size, size),
        cornerRadius = CornerRadius(size * 0.18f, size * 0.18f),
        style = Stroke(width = 2f)
    )

    // Concentric Launch Rings
    val cx = topLeft.x + size / 2f
    val cy = topLeft.y + size / 2f
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = size * 0.38f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )
    drawCircle(
        color = Color.White,
        radius = size * 0.22f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = color.darkCore,
        radius = size * 0.12f,
        center = Offset(cx, cy)
    )

    // Directional Launch Arrow
    val rad = Math.toRadians(directionAngle.toDouble())
    val forward = Offset(cos(rad).toFloat(), sin(rad).toFloat())
    val perp = Offset(-sin(rad).toFloat(), cos(rad).toFloat())

    val arrowPath = Path().apply {
        val tip = Offset(cx + forward.x * size * 0.38f, cy + forward.y * size * 0.38f)
        val wing1 = Offset(cx + forward.x * size * 0.18f + perp.x * size * 0.18f, cy + forward.y * size * 0.18f + perp.y * size * 0.18f)
        val wing2 = Offset(cx + forward.x * size * 0.18f - perp.x * size * 0.18f, cy + forward.y * size * 0.18f - perp.y * size * 0.18f)
        moveTo(tip.x, tip.y)
        lineTo(wing1.x, wing1.y)
        lineTo(wing2.x, wing2.y)
        close()
    }
    drawPath(arrowPath, color = color.neonGlow, style = Fill)
}

/**
 * Holographic Safe Star / Quantum Shield Checkpoint
 */
private fun DrawScope.drawHoloShieldStar(
    center: Offset,
    radius: Float,
    shieldRotation: Float
) {
    // Holographic Quantum Shield Hexagon/Circle
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.20f),
        radius = radius * 1.15f,
        center = center
    )

    // Rotating Shield Ring with Dashes
    val rad = Math.toRadians(shieldRotation.toDouble())
    drawCircle(
        color = Color(0xFF0288D1),
        radius = radius * 1.05f,
        center = center,
        style = Stroke(
            width = 1.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(radius * 0.3f, radius * 0.2f))
        )
    )

    // Radiant Gold/Cyan Quantum Star
    val path = Path()
    val outerR = radius * 0.85f
    val innerR = outerR * 0.38f
    val points = 8 // 8-point digital holographic star
    var angle = -Math.PI / 2 + rad * 0.5f

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val x = (center.x + cos(angle) * r).toFloat()
        val y = (center.y + sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        angle += Math.PI / points
    }
    path.close()

    // Outer gold star with dark cyber border for sharp visibility on white
    drawPath(path, color = Color(0xFFFFB300), style = Fill)
    drawPath(path, color = Color(0xFF263238), style = Stroke(width = 1.2f))

    // Center Energy Core Point
    drawCircle(color = Color.White, radius = radius * 0.20f, center = center)
    drawCircle(color = Color(0xFF0288D1), radius = radius * 0.10f, center = center)
}

/**
 * Central Quantum Singularity Reactor (Finish Nexus)
 */
private fun DrawScope.drawQuantumSingularityCore(
    cellSize: Float,
    reactorRotation: Float
) {
    val centerTopLeft = Offset(6 * cellSize, 6 * cellSize)
    val centerSize = 3 * cellSize
    val centerPoint = Offset(centerTopLeft.x + centerSize / 2f, centerTopLeft.y + centerSize / 2f)

    // 4 Faceted Holographic Quadrants converging to singularity
    // 1. Red Left Triangle
    val redPath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(
        path = redPath,
        brush = Brush.radialGradient(
            colors = listOf(PlayerColor.RED.primary, PlayerColor.RED.darkCore),
            center = centerPoint,
            radius = centerSize * 0.7f
        )
    )

    // 2. Green Top Triangle
    val greenPath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y)
        close()
    }
    drawPath(
        path = greenPath,
        brush = Brush.radialGradient(
            colors = listOf(PlayerColor.GREEN.primary, PlayerColor.GREEN.darkCore),
            center = centerPoint,
            radius = centerSize * 0.7f
        )
    )

    // 3. Yellow Right Triangle
    val yellowPath = Path().apply {
        moveTo(centerTopLeft.x + centerSize, centerTopLeft.y)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(
        path = yellowPath,
        brush = Brush.radialGradient(
            colors = listOf(PlayerColor.YELLOW.primary, PlayerColor.YELLOW.darkCore),
            center = centerPoint,
            radius = centerSize * 0.7f
        )
    )

    // 4. Blue Bottom Triangle
    val bluePath = Path().apply {
        moveTo(centerTopLeft.x, centerTopLeft.y + centerSize)
        lineTo(centerPoint.x, centerPoint.y)
        lineTo(centerTopLeft.x + centerSize, centerTopLeft.y + centerSize)
        close()
    }
    drawPath(
        path = bluePath,
        brush = Brush.radialGradient(
            colors = listOf(PlayerColor.BLUE.primary, PlayerColor.BLUE.darkCore),
            center = centerPoint,
            radius = centerSize * 0.7f
        )
    )

    // Quadrant Partition Neon Laser Lines
    val dividerColor = Color(0xFF00E5FF).copy(alpha = 0.75f)
    drawLine(color = dividerColor, start = centerTopLeft, end = Offset(centerTopLeft.x + centerSize, centerTopLeft.y + centerSize), strokeWidth = 1.5f)
    drawLine(color = dividerColor, start = Offset(centerTopLeft.x + centerSize, centerTopLeft.y), end = Offset(centerTopLeft.x, centerTopLeft.y + centerSize), strokeWidth = 1.5f)

    // Outer Reactor Containment Ring
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.5f),
        radius = centerSize * 0.46f,
        center = centerPoint,
        style = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellSize * 0.35f, cellSize * 0.2f))
        )
    )

    // Rotating Holographic Gear / Segmented Arc Ring
    val radAngle = reactorRotation
    drawArc(
        color = Color(0xFFFFD600),
        startAngle = radAngle,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(centerPoint.x - centerSize * 0.38f, centerPoint.y - centerSize * 0.38f),
        size = Size(centerSize * 0.76f, centerSize * 0.76f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFF00E5FF),
        startAngle = radAngle + 180f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(centerPoint.x - centerSize * 0.38f, centerPoint.y - centerSize * 0.38f),
        size = Size(centerSize * 0.76f, centerSize * 0.76f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Central Singularity Sphere
    val coreRadius = cellSize * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFF0D0221)),
            center = centerPoint,
            radius = coreRadius
        ),
        radius = coreRadius,
        center = centerPoint
    )

    // Core Edge Rim
    drawCircle(
        color = Color.White,
        radius = coreRadius,
        center = centerPoint,
        style = Stroke(width = 2f)
    )

    // Victory Nexus Holographic Star Crystal
    drawSciFiCrystal(centerPoint, radius = cellSize * 0.36f)
}

/**
 * Multi-layer Cyber Crystal Star at the center of the Singularity
 */
private fun DrawScope.drawSciFiCrystal(center: Offset, radius: Float) {
    val path = Path()
    val points = 4
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.4f
        val a = -Math.PI / 2 + (i * Math.PI / points)
        val x = (center.x + cos(a) * r).toFloat()
        val y = (center.y + sin(a) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(path, color = Color(0xFFFFD600), style = Fill)
    drawPath(path, color = Color.White, style = Stroke(width = 1.2f))
    drawCircle(color = Color.White, radius = radius * 0.22f, center = center)
}

/**
 * Outer Frame with Cyberpunk Corner Brackets
 */
private fun DrawScope.drawSciFiFrame(cellSize: Float) {
    // Outer Neon Border
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF1E3A5F), Color(0xFFFF2A55), Color(0xFF00E676))
        ),
        topLeft = Offset.Zero,
        size = size,
        style = Stroke(width = 2f)
    )

    // Corner Sci-Fi Tech Brackets
    val bracketLen = cellSize * 0.8f
    drawCornerBrackets(Offset.Zero, size.width, Color(0xFF00E5FF), bracketLen)
}

/**
 * Draws HUD-style corner brackets [ ]
 */
private fun DrawScope.drawCornerBrackets(
    topLeft: Offset,
    dimension: Float,
    color: Color,
    bracketLen: Float
) {
    val sw = 2.5f
    val bR = topLeft.x + dimension
    val bB = topLeft.y + dimension

    // Top-Left
    drawLine(color, Offset(topLeft.x, topLeft.y), Offset(topLeft.x + bracketLen, topLeft.y), sw)
    drawLine(color, Offset(topLeft.x, topLeft.y), Offset(topLeft.x, topLeft.y + bracketLen), sw)

    // Top-Right
    drawLine(color, Offset(bR - bracketLen, topLeft.y), Offset(bR, topLeft.y), sw)
    drawLine(color, Offset(bR, topLeft.y), Offset(bR, topLeft.y + bracketLen), sw)

    // Bottom-Left
    drawLine(color, Offset(topLeft.x, bB), Offset(topLeft.x + bracketLen, bB), sw)
    drawLine(color, Offset(topLeft.x, bB), Offset(topLeft.x, bB - bracketLen), sw)

    // Bottom-Right
    drawLine(color, Offset(bR - bracketLen, bB), Offset(bR, bB), sw)
    drawLine(color, Offset(bR, bB), Offset(bR, bB - bracketLen), sw)
}

/**
 * Draws High-Tech Levitation Cyber-Drone Tokens.
 * Features:
 * - Enlarged base size for bold visual clarity
 * - Noticeable scale-up for movable tokens after dice roll ("নিজ নিজ জায়গা থেকে অন্যান্য গুটি থেকে বড় দেখাবে")
 * - Spinning circular holographic energy vortex underneath active tokens ("গুটির নিচে কিছু একটা গোল করে ঘুরবে")
 * - 3D multi-layered orb aesthetic with metallic titanium crown, glowing plasma core, and glass specular dome
 */
private fun DrawScope.drawSciFiTokens(
    tokens: List<TokenScreenPosition>,
    cellSize: Float,
    pulseGlow: Float,
    tokenSpinAngle: Float
) {
    // Generous base size so tokens look prominent and clearly visible
    val defaultRadius = cellSize * 0.44f

    tokens.forEach { item ->
        val center = item.center
        val color = item.player.color

        // When movable, scale up noticeably larger than all other tokens on the board!
        val currentRadius = if (item.isMovable) {
            defaultRadius * 1.32f * (0.95f + 0.08f * ((pulseGlow - 0.9f) / 0.45f))
        } else {
            defaultRadius
        }

        // 1. Spinning Circular Energy Vortex beneath active movable tokens ("গুটির নিচে গোল করে ঘুরবে")
        if (item.isMovable) {
            val vortexRadius = currentRadius * 1.45f

            // Radiant ground plasma flare
            drawCircle(
                color = color.neonGlow.copy(alpha = 0.35f),
                radius = vortexRadius * 1.15f,
                center = center
            )

            // Outer rotating segmented energy ring (clockwise rotation)
            for (i in 0 until 4) {
                drawArc(
                    color = color.neonGlow,
                    startAngle = tokenSpinAngle + i * 90f,
                    sweepAngle = 44f,
                    useCenter = false,
                    topLeft = Offset(center.x - vortexRadius, center.y - vortexRadius),
                    size = Size(vortexRadius * 2f, vortexRadius * 2f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            // Inner counter-rotating halo ring (counter-clockwise rotation)
            val innerVortex = vortexRadius * 0.76f
            for (i in 0 until 4) {
                drawArc(
                    color = Color.White.copy(alpha = 0.85f),
                    startAngle = -tokenSpinAngle * 1.4f + i * 90f + 25f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerVortex, center.y - innerVortex),
                    size = Size(innerVortex * 2f, innerVortex * 2f),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )
            }

            // 4 rotating orbital power particles
            val rad = Math.toRadians(tokenSpinAngle.toDouble())
            for (i in 0 until 4) {
                val a = rad + i * (Math.PI / 2.0)
                val px = (center.x + cos(a) * vortexRadius).toFloat()
                val py = (center.y + sin(a) * vortexRadius).toFloat()
                drawCircle(color = Color.White, radius = currentRadius * 0.12f, center = Offset(px, py))
                drawCircle(color = color.neonGlow, radius = currentRadius * 0.07f, center = Offset(px, py))
            }

            // Outer targeting reticle brackets
            val reticleArm = currentRadius * 0.32f
            val rD = vortexRadius * 1.15f
            drawLine(color.neonGlow, Offset(center.x - rD, center.y), Offset(center.x - rD + reticleArm, center.y), 2.2f)
            drawLine(color.neonGlow, Offset(center.x + rD, center.y), Offset(center.x + rD - reticleArm, center.y), 2.2f)
            drawLine(color.neonGlow, Offset(center.x, center.y - rD), Offset(center.x, center.y - rD + reticleArm), 2.2f)
            drawLine(color.neonGlow, Offset(center.x, center.y + rD), Offset(center.x, center.y + rD - reticleArm), 2.2f)
        }

        // 2. Projected Ground Glow & 3D Levitation Shadow
        val shadowOffsetY = if (item.isMovable) 6.5f else 3f
        drawCircle(
            color = color.neonGlow.copy(alpha = if (item.isMovable) 0.40f else 0.20f),
            radius = currentRadius * 1.12f,
            center = Offset(center.x, center.y + shadowOffsetY * 0.7f)
        )
        drawCircle(
            color = Color(0xFF03070C).copy(alpha = if (item.isMovable) 0.65f else 0.45f),
            radius = currentRadius * 0.95f,
            center = Offset(center.x + 1.5f, center.y + shadowOffsetY)
        )

        // 3. Cyber Titanium / Precision Armor Outer Rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFF94A3B8), Color(0xFF1E293B)),
                center = Offset(center.x - currentRadius * 0.35f, center.y - currentRadius * 0.35f),
                radius = currentRadius * 1.4f
            ),
            radius = currentRadius,
            center = center
        )

        // Outer Rim Cyber Seam
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = currentRadius,
            center = center,
            style = Stroke(width = 1.3f)
        )

        // 4. Glowing Plasma Energy Core (Player Color)
        val plasmaRadius = currentRadius * 0.78f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.neonGlow, color.secondary, color.primary, color.darkCore),
                center = Offset(center.x - plasmaRadius * 0.25f, center.y - plasmaRadius * 0.25f),
                radius = plasmaRadius * 1.35f
            ),
            radius = plasmaRadius,
            center = center
        )

        // Neon Core Energy Ring
        drawCircle(
            color = color.neonGlow,
            radius = plasmaRadius,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // 5. High-Specular Glass Top Reflection (3D Holographic Orb Dome)
        val specR = plasmaRadius * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, color.secondary.copy(alpha = 0.8f), Color.Transparent),
                center = Offset(center.x - specR * 0.45f, center.y - specR * 0.5f),
                radius = specR * 1.25f
            ),
            radius = specR,
            center = Offset(center.x - plasmaRadius * 0.22f, center.y - plasmaRadius * 0.22f)
        )

        // 6. Central Quantum Power Core Node
        drawCircle(
            color = Color.White,
            radius = currentRadius * 0.18f,
            center = center
        )
        drawCircle(
            color = color.neonGlow,
            radius = currentRadius * 0.09f,
            center = center
        )
    }
}
