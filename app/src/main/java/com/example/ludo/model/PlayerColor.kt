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
    val yardCol: Int
) {
    RED(
        title = "Red",
        primary = Color(0xFFE53935),
        secondary = Color(0xFFFF8A80),
        lightContainer = Color(0xFFFFEBEE),
        darkShade = Color(0xFFB71C1C),
        startTrackIndex = 0,
        yardRow = 0,
        yardCol = 0
    ),
    GREEN(
        title = "Green",
        primary = Color(0xFF43A047),
        secondary = Color(0xFFB9F6CA),
        lightContainer = Color(0xFFE8F5E9),
        darkShade = Color(0xFF1B5E20),
        startTrackIndex = 13,
        yardRow = 0,
        yardCol = 9
    ),
    YELLOW(
        title = "Yellow",
        primary = Color(0xFFFDD835),
        secondary = Color(0xFFFFF59D),
        lightContainer = Color(0xFFFFFDE7),
        darkShade = Color(0xFFF57F17),
        startTrackIndex = 26,
        yardRow = 9,
        yardCol = 9
    ),
    BLUE(
        title = "Blue",
        primary = Color(0xFF1E88E5),
        secondary = Color(0xFF82B1FF),
        lightContainer = Color(0xFFE3F2FD),
        darkShade = Color(0xFF0D47A1),
        startTrackIndex = 39,
        yardRow = 9,
        yardCol = 0
    );

    companion object {
        val ORDER = listOf(RED, GREEN, YELLOW, BLUE)
    }
}
