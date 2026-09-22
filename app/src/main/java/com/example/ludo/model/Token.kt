package com.example.ludo.model

/**
 * Represents a token on the Ludo board.
 *
 * step values:
 *   -1 : in yard / base
 *   0..50 : on common track (relative to player start: step 0 is starting square)
 *   51..55 : inside player's private home corridor (5 squares)
 *   56 : reached central goal (finished)
 */
data class Token(
    val id: Int,
    val color: PlayerColor,
    val step: Int = -1
) {
    val isInYard: Boolean get() = step == -1
    val isFinished: Boolean get() = step == 56
    val isOnBoard: Boolean get() = step in 0..55
    val isInHomeCorridor: Boolean get() = step in 51..55

    fun canMove(diceValue: Int): Boolean {
        if (isFinished) return false
        if (isInYard) {
            return diceValue == 6
        }
        return step + diceValue <= 56
    }
}

data class Player(
    val color: PlayerColor,
    val name: String,
    val isBot: Boolean = false,
    val tokens: List<Token> = List(4) { id -> Token(id = id, color = color) },
    val rank: Int? = null,
    val isActiveInGame: Boolean = true
) {
    val hasWon: Boolean get() = tokens.all { it.isFinished }
    val finishedTokensCount: Int get() = tokens.count { it.isFinished }
    val tokensInYardCount: Int get() = tokens.count { it.isInYard }
}
