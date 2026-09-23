package com.example.ludo.viewmodel

import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token

enum class GameMode(val label: String) {
    PASS_AND_PLAY("Pass & Play"),
    VS_COMPUTER("VS Computer")
}

enum class TurnPhase {
    WAITING_FOR_ROLL,
    ROLLING,
    SELECTING_TOKEN,
    MOVING_TOKEN,
    TURN_TRANSITION,
    GAME_OVER
}

data class TokenAnimationState(
    val tokenId: Int,
    val color: PlayerColor,
    val fromStep: Int,
    val toStep: Int,
    val progress: Float
)

data class LudoGameState(
    val players: List<Player> = emptyList(),
    val activePlayerIndex: Int = 0,
    val diceValue: Int = 1,
    val turnPhase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    val consecutiveSixes: Int = 0,
    val movableTokenIds: Set<Int> = emptySet(),
    val statusMessage: String = "Roll the dice to begin!",
    val gameMode: GameMode = GameMode.PASS_AND_PLAY,
    val winnersList: List<Player> = emptyList(),
    val isGameOver: Boolean = false,
    val extraTurnEarned: Boolean = false,
    val lastCapturedToken: Token? = null,
    val totalTurns: Int = 0,
    val animatingToken: TokenAnimationState? = null,
    val playerDiceValues: Map<PlayerColor, Int> = mapOf(
        PlayerColor.RED to 1,
        PlayerColor.GREEN to 1,
        PlayerColor.YELLOW to 1,
        PlayerColor.BLUE to 1
    )
) {
    val activePlayer: Player?
        get() = players.getOrNull(activePlayerIndex)

    val isCurrentPlayerBot: Boolean
        get() = activePlayer?.isBot == true
}
