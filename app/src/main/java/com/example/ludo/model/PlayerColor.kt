package com.example.ludo.model

import androidx.compose.ui.graphics.Color

enum class PlayerColor(
    val title: String,
    val primary: Color,
    val secondary: Color,
    val lightContainer: Color,
    val darkShade: Color,
    val startTrackIndex: Int, // Index on the 52-cell track
    val yardRow: Int,
    val yardCol: Int,
    val neonGlow: Color,
    val darkCore: Color,
    val cyberPlate: Color,
    val codeName: String
) {
    RED(
        title = "Red",
        primary = Color(0xFFFF2A55),
        secondary = Color(0xFFFF7A96),
        lightContainer = Color(0xFFFFEBEE),
        darkShade = Color(0xFFB71C1C),
        startTrackIndex = 0,
        yardRow = 0,
        yardCol = 0,
        neonGlow = Color(0xFFFF1744),
        darkCore = Color(0xFF1A060A),
        cyberPlate = Color(0xFF260D12),
        codeName = "ALPHA-RED"
    ),
    GREEN(
        title = "Green",
        primary = Color(0xFF00E676),
        secondary = Color(0xFF69F0AE),
        lightContainer = Color(0xFFE8F5E9),
        darkShade = Color(0xFF1B5E20),
        startTrackIndex = 13,
        yardRow = 0,
        yardCol = 9,
        neonGlow = Color(0xFF00FF88),
        darkCore = Color(0xFF05170D),
        cyberPlate = Color(0xFF0A2616),
        codeName = "BETA-GRN"
    ),
    YELLOW(
        title = "Yellow",
        primary = Color(0xFFFFD600),
        secondary = Color(0xFFFFF176),
        lightContainer = Color(0xFFFFFDE7),
        darkShade = Color(0xFFF57F17),
        startTrackIndex = 26,
        yardRow = 9,
        yardCol = 9,
        neonGlow = Color(0xFFFFEA00),
        darkCore = Color(0xFF171304),
        cyberPlate = Color(0xFF262007),
        codeName = "GAMMA-YLW"
    ),
    BLUE(
        title = "Blue",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFF80EAFF),
        lightContainer = Color(0xFFE3F2FD),
        darkShade = Color(0xFF0D47A1),
        startTrackIndex = 39,
        yardRow = 9,
        yardCol = 0,
        neonGlow = Color(0xFF18FFFF),
        darkCore = Color(0xFF03141F),
        cyberPlate = Color(0xFF072133),
        codeName = "DELTA-BLU"
    );

    companion object {
        val ORDER = listOf(RED, GREEN, YELLOW, BLUE)
    }
}
