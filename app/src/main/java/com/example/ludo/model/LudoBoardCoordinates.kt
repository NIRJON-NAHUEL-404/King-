package com.example.ludo.model

data class GridPos(val row: Float, val col: Float)

object LudoBoardCoordinates {
    // 52 Common Track cells in clockwise order starting from Red start (row 6, col 1)
    val TRACK_CELLS: List<Pair<Int, Int>> = listOf(
        Pair(6, 1),   // 0: Red Start (Safe)
        Pair(6, 2),   // 1
        Pair(6, 3),   // 2
        Pair(6, 4),   // 3
        Pair(6, 5),   // 4
        Pair(5, 6),   // 5
        Pair(4, 6),   // 6
        Pair(3, 6),   // 7
        Pair(2, 6),   // 8: Safe Star
        Pair(1, 6),   // 9
        Pair(0, 6),   // 10
        Pair(0, 7),   // 11
        Pair(0, 8),   // 12
        Pair(1, 8),   // 13: Green Start (Safe)
        Pair(2, 8),   // 14
        Pair(3, 8),   // 15
        Pair(4, 8),   // 16
        Pair(5, 8),   // 17
        Pair(6, 9),   // 18
        Pair(6, 10),  // 19
        Pair(6, 11),  // 20
        Pair(6, 12),  // 21: Safe Star
        Pair(6, 13),  // 22
        Pair(6, 14),  // 23
        Pair(7, 14),  // 24
        Pair(8, 14),  // 25
        Pair(8, 13),  // 26: Yellow Start (Safe)
        Pair(8, 12),  // 27
        Pair(8, 11),  // 28
        Pair(8, 10),  // 29
        Pair(8, 9),   // 30
        Pair(9, 8),   // 31
        Pair(10, 8),  // 32
        Pair(11, 8),  // 33
        Pair(12, 8),  // 34: Safe Star
        Pair(13, 8),  // 35
        Pair(14, 8),  // 36
        Pair(14, 7),  // 37
        Pair(14, 6),  // 38
        Pair(13, 6),  // 39: Blue Start (Safe)
        Pair(12, 6),  // 40
        Pair(11, 6),  // 41
        Pair(10, 6),  // 42
        Pair(9, 6),   // 43
        Pair(8, 5),   // 44
        Pair(8, 4),   // 45
        Pair(8, 3),   // 46
        Pair(8, 2),   // 47: Safe Star
        Pair(8, 1),   // 48
        Pair(8, 0),   // 49
        Pair(7, 0),   // 50
        Pair(6, 0)    // 51
    )

    // Star cells indices on the 52 track (safe from capture)
    val STAR_INDICES = setOf(8, 21, 34, 47)
    val START_INDICES = setOf(0, 13, 26, 39)
    val ALL_SAFE_INDICES = STAR_INDICES + START_INDICES

    // Home columns for each player (5 squares leading to central triangle)
    private val HOME_COLUMNS: Map<PlayerColor, List<Pair<Int, Int>>> = mapOf(
        PlayerColor.RED to listOf(Pair(7, 1), Pair(7, 2), Pair(7, 3), Pair(7, 4), Pair(7, 5)),
        PlayerColor.GREEN to listOf(Pair(1, 7), Pair(2, 7), Pair(3, 7), Pair(4, 7), Pair(5, 7)),
        PlayerColor.YELLOW to listOf(Pair(7, 13), Pair(7, 12), Pair(7, 11), Pair(7, 10), Pair(7, 9)),
        PlayerColor.BLUE to listOf(Pair(13, 7), Pair(12, 7), Pair(11, 7), Pair(10, 7), Pair(9, 7))
    )

    // Center goal triangle positions
    private val GOAL_POSITIONS: Map<PlayerColor, GridPos> = mapOf(
        PlayerColor.RED to GridPos(7f, 6.2f),
        PlayerColor.GREEN to GridPos(6.2f, 7f),
        PlayerColor.YELLOW to GridPos(7f, 7.8f),
        PlayerColor.BLUE to GridPos(7.8f, 7f)
    )

    // Base slots in 6x6 yard for each player
    private val YARD_POSITIONS: Map<PlayerColor, List<GridPos>> = mapOf(
        PlayerColor.RED to listOf(
            GridPos(1.5f, 1.5f), GridPos(1.5f, 3.5f),
            GridPos(3.5f, 1.5f), GridPos(3.5f, 3.5f)
        ),
        PlayerColor.GREEN to listOf(
            GridPos(1.5f, 10.5f), GridPos(1.5f, 12.5f),
            GridPos(3.5f, 10.5f), GridPos(3.5f, 12.5f)
        ),
        PlayerColor.YELLOW to listOf(
            GridPos(10.5f, 10.5f), GridPos(10.5f, 12.5f),
            GridPos(12.5f, 10.5f), GridPos(12.5f, 12.5f)
        ),
        PlayerColor.BLUE to listOf(
            GridPos(10.5f, 1.5f), GridPos(10.5f, 3.5f),
            GridPos(12.5f, 1.5f), GridPos(12.5f, 3.5f)
        )
    )

    /**
     * Translates a token's color and step into a grid coordinate (row, col)
     */
    fun getTokenPosition(color: PlayerColor, step: Int, tokenId: Int): GridPos {
        if (step == -1) {
            val yardSlots = YARD_POSITIONS[color] ?: return GridPos(0f, 0f)
            return yardSlots.getOrElse(tokenId.coerceIn(0, 3)) { yardSlots[0] }
        }

        if (step in 0..50) {
            val trackIndex = (color.startTrackIndex + step) % 52
            val cell = TRACK_CELLS[trackIndex]
            return GridPos(cell.first.toFloat(), cell.second.toFloat())
        }

        if (step in 51..55) {
            val homeIndex = step - 51
            val cell = HOME_COLUMNS[color]?.getOrNull(homeIndex) ?: Pair(7, 7)
            return GridPos(cell.first.toFloat(), cell.second.toFloat())
        }

        // Step 56: Home Goal
        val goal = GOAL_POSITIONS[color] ?: GridPos(7f, 7f)
        // Slight offset for multiple tokens in goal so they are all distinct
        val offsetRow = when (tokenId) {
            0 -> -0.15f
            1 -> 0.15f
            2 -> -0.15f
            else -> 0.15f
        }
        val offsetCol = when (tokenId) {
            0 -> -0.15f
            1 -> -0.15f
            2 -> 0.15f
            else -> 0.15f
        }
        return GridPos(goal.row + offsetRow, goal.col + offsetCol)
    }

    /**
     * Gets absolute common track index (0..51) for a token on track, or null if in yard/home corridor
     */
    fun getCommonTrackIndex(color: PlayerColor, step: Int): Int? {
        if (step in 0..50) {
            return (color.startTrackIndex + step) % 52
        }
        return null
    }

    /**
     * Checks if a common track index is a safe star or start cell
     */
    fun isSafeTrackCell(trackIndex: Int): Boolean {
        return ALL_SAFE_INDICES.contains(trackIndex)
    }
}
