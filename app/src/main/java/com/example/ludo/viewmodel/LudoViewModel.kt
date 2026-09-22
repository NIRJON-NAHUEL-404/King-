package com.example.ludo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.audio.LudoSoundManager
import com.example.ludo.model.LudoBoardCoordinates
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = LudoSoundManager(application)

    private val _gameState = MutableStateFlow(LudoGameState())
    val gameState: StateFlow<LudoGameState> = _gameState.asStateFlow()

    private var botJob: Job? = null
    private var animationJob: Job? = null

    init {
        startNewGame(
            playerCount = 4,
            mode = GameMode.PASS_AND_PLAY,
            botFlags = listOf(false, false, false, false)
        )
    }

    fun startNewGame(
        playerCount: Int,
        mode: GameMode,
        botFlags: List<Boolean> = listOf(false, true, true, true),
        customNames: List<String>? = null
    ) {
        botJob?.cancel()
        animationJob?.cancel()

        val selectedColors = when (playerCount) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> PlayerColor.ORDER
        }

        val players = selectedColors.mapIndexed { index, color ->
            val defaultName = when (mode) {
                GameMode.VS_COMPUTER -> if (index == 0) "Player (You)" else "Bot ${color.title}"
                GameMode.PASS_AND_PLAY -> customNames?.getOrNull(index)?.ifBlank { null } ?: "Player ${color.title}"
            }
            val isBot = if (mode == GameMode.VS_COMPUTER) index > 0 else botFlags.getOrElse(index) { false }
            Player(
                color = color,
                name = defaultName,
                isBot = isBot,
                tokens = List(4) { id -> Token(id = id, color = color, step = -1) }
            )
        }

        _gameState.value = LudoGameState(
            players = players,
            activePlayerIndex = 0,
            diceValue = 1,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            statusMessage = "${players.first().name}'s turn to roll!",
            gameMode = mode
        )

        checkIfActivePlayerIsBot()
    }

    fun rollDice() {
        val state = _gameState.value
        if (state.turnPhase != TurnPhase.WAITING_FOR_ROLL || state.isGameOver) return

        viewModelScope.launch {
            _gameState.update { it.copy(turnPhase = TurnPhase.ROLLING, statusMessage = "Rolling dice...") }
            soundManager.playDiceRoll()

            // Realistic dice rolling animation frames
            for (i in 0 until 6) {
                delay(60)
                val tempDice = Random.nextInt(1, 7)
                _gameState.update { it.copy(diceValue = tempDice) }
            }

            val finalDice = Random.nextInt(1, 7)
            _gameState.update { it.copy(diceValue = finalDice) }

            handleDiceRollResult(finalDice)
        }
    }

    private fun handleDiceRollResult(dice: Int) {
        val state = _gameState.value
        val player = state.activePlayer ?: return

        var newConsecutiveSixes = if (dice == 6) state.consecutiveSixes + 1 else 0

        if (dice == 6) {
            soundManager.playSixRolled()
        }

        // Rule: 3 consecutive sixes cancels turn
        if (newConsecutiveSixes >= 3) {
            _gameState.update {
                it.copy(
                    consecutiveSixes = 0,
                    turnPhase = TurnPhase.TURN_TRANSITION,
                    statusMessage = "${player.name} rolled three 6s! Turn forfeited."
                )
            }
            viewModelScope.launch {
                delay(1200)
                advanceToNextPlayer()
            }
            return
        }

        val movableTokens = player.tokens.filter { it.canMove(dice) }

        if (movableTokens.isEmpty()) {
            _gameState.update {
                it.copy(
                    consecutiveSixes = newConsecutiveSixes,
                    movableTokenIds = emptySet(),
                    turnPhase = TurnPhase.TURN_TRANSITION,
                    statusMessage = "No moves available for ${player.name} with a $dice."
                )
            }
            viewModelScope.launch {
                delay(1100)
                advanceToNextPlayer()
            }
        } else {
            val movableIds = movableTokens.map { it.id }.toSet()
            _gameState.update {
                it.copy(
                    consecutiveSixes = newConsecutiveSixes,
                    movableTokenIds = movableIds,
                    turnPhase = TurnPhase.SELECTING_TOKEN,
                    statusMessage = if (player.isBot) {
                        "${player.name} is thinking..."
                    } else if (movableTokens.size == 1) {
                        "Tap the highlighted token to move!"
                    } else {
                        "Choose a token to advance by $dice!"
                    }
                )
            }

            if (player.isBot) {
                scheduleBotMove(movableTokens, dice)
            } else if (movableTokens.size == 1) {
                // Auto-advance single option after slight hesitation for human ease if desired,
                // or let user tap. Let's provide a quick auto-move after 600ms if not tapped!
                viewModelScope.launch {
                    delay(800)
                    if (_gameState.value.turnPhase == TurnPhase.SELECTING_TOKEN &&
                        _gameState.value.movableTokenIds == movableIds
                    ) {
                        moveToken(movableTokens.first().id)
                    }
                }
            }
        }
    }

    fun onTokenClicked(tokenId: Int) {
        val state = _gameState.value
        if (state.turnPhase != TurnPhase.SELECTING_TOKEN) return
        if (state.isCurrentPlayerBot) return
        if (!state.movableTokenIds.contains(tokenId)) return

        moveToken(tokenId)
    }

    fun onDiceClicked() {
        val state = _gameState.value
        if (state.turnPhase == TurnPhase.WAITING_FOR_ROLL && !state.isCurrentPlayerBot) {
            rollDice()
        } else if (state.turnPhase == TurnPhase.SELECTING_TOKEN && !state.isCurrentPlayerBot) {
            // If user taps dice when a move is pending, advance the first available token
            val firstMovable = state.movableTokenIds.firstOrNull()
            if (firstMovable != null) {
                moveToken(firstMovable)
            }
        }
    }

    private fun moveToken(tokenId: Int) {
        animationJob?.cancel()
        val state = _gameState.value
        val player = state.activePlayer ?: return
        val token = player.tokens.find { it.id == tokenId } ?: return
        val dice = state.diceValue

        val startStep = token.step
        val targetStep = if (token.isInYard) 0 else token.step + dice

        animationJob = viewModelScope.launch {
            _gameState.update {
                it.copy(
                    turnPhase = TurnPhase.MOVING_TOKEN,
                    movableTokenIds = emptySet(),
                    statusMessage = "${player.name} is moving token..."
                )
            }

            // Animate step by step
            if (token.isInYard) {
                delay(200)
                soundManager.playTokenMove()
                updatePlayerTokenStep(player.color, tokenId, 0)
            } else {
                for (s in (startStep + 1)..targetStep) {
                    delay(120)
                    soundManager.playTokenMove()
                    updatePlayerTokenStep(player.color, tokenId, s)
                }
            }

            delay(100)
            handleMoveCompletion(player, tokenId, targetStep, dice)
        }
    }

    private fun updatePlayerTokenStep(playerColor: PlayerColor, tokenId: Int, newStep: Int) {
        _gameState.update { state ->
            val updatedPlayers = state.players.map { p ->
                if (p.color == playerColor) {
                    p.copy(tokens = p.tokens.map { t ->
                        if (t.id == tokenId) t.copy(step = newStep) else t
                    })
                } else p
            }
            state.copy(players = updatedPlayers)
        }
    }

    private fun handleMoveCompletion(
        player: Player,
        tokenId: Int,
        finalStep: Int,
        diceRolled: Int
    ) {
        var extraTurn = false
        var capturedTokenInfo: Token? = null

        // 1. Check Home Reach
        if (finalStep == 56) {
            soundManager.playHomeReached()
            extraTurn = true
        }

        // 2. Check Capture on common track
        if (finalStep in 0..50) {
            val landTrackIndex = (player.color.startTrackIndex + finalStep) % 52
            val isSafe = LudoBoardCoordinates.isSafeTrackCell(landTrackIndex)

            if (!isSafe) {
                // Check if any opponent token is on this cell
                _gameState.value.players.forEach { opponent ->
                    if (opponent.color != player.color) {
                        opponent.tokens.forEach { oppToken ->
                            if (oppToken.step in 0..50) {
                                val oppTrackIndex = (opponent.color.startTrackIndex + oppToken.step) % 52
                                if (oppTrackIndex == landTrackIndex) {
                                    // Captured! Send back to yard
                                    capturedTokenInfo = oppToken
                                    updatePlayerTokenStep(opponent.color, oppToken.id, -1)
                                    soundManager.playCapture()
                                    extraTurn = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Rolling a 6 earns an extra turn (if not already forfeited)
        if (diceRolled == 6) {
            extraTurn = true
        }

        // 4. Check if player has finished all 4 tokens (Won)
        val refreshedPlayer = _gameState.value.players.find { it.color == player.color } ?: player
        if (refreshedPlayer.tokens.all { it.isFinished } && refreshedPlayer.rank == null) {
            val currentWinners = _gameState.value.winnersList
            val newRank = currentWinners.size + 1
            val updatedPlayerWithRank = refreshedPlayer.copy(rank = newRank)

            val updatedWinners = currentWinners + updatedPlayerWithRank
            val updatedPlayers = _gameState.value.players.map {
                if (it.color == player.color) updatedPlayerWithRank else it
            }

            soundManager.playWin()

            val activeRemainingPlayers = updatedPlayers.filter { it.rank == null }
            val isOver = activeRemainingPlayers.size <= 1

            if (isOver) {
                // Rank the last remaining player if any
                val finalWinners = if (activeRemainingPlayers.isNotEmpty()) {
                    updatedWinners + activeRemainingPlayers.first().copy(rank = updatedWinners.size + 1)
                } else updatedWinners

                _gameState.update {
                    it.copy(
                        players = updatedPlayers,
                        winnersList = finalWinners,
                        isGameOver = true,
                        turnPhase = TurnPhase.GAME_OVER,
                        statusMessage = "🏆 Game Over! ${finalWinners.firstOrNull()?.name} is the Winner!"
                    )
                }
                return
            } else {
                _gameState.update {
                    it.copy(
                        players = updatedPlayers,
                        winnersList = updatedWinners,
                        statusMessage = "🎉 ${player.name} finished at Rank #$newRank!"
                    )
                }
            }
        }

        val message = when {
            capturedTokenInfo != null -> "💥 ${player.name} captured an opponent! Bonus turn!"
            finalStep == 56 -> "⭐ ${player.name} reached Home! Bonus turn!"
            diceRolled == 6 -> "🎲 Six rolled! Bonus turn for ${player.name}!"
            else -> null
        }

        if (extraTurn) {
            _gameState.update {
                it.copy(
                    turnPhase = TurnPhase.WAITING_FOR_ROLL,
                    movableTokenIds = emptySet(),
                    statusMessage = message ?: "${player.name} gets an extra turn!"
                )
            }
            checkIfActivePlayerIsBot()
        } else {
            _gameState.update {
                it.copy(
                    turnPhase = TurnPhase.TURN_TRANSITION,
                    movableTokenIds = emptySet(),
                    consecutiveSixes = 0,
                    statusMessage = message ?: "Turn ending..."
                )
            }
            viewModelScope.launch {
                delay(600)
                advanceToNextPlayer()
            }
        }
    }

    private fun advanceToNextPlayer() {
        val state = _gameState.value
        if (state.isGameOver) return

        val playersCount = state.players.size
        var nextIndex = (state.activePlayerIndex + 1) % playersCount

        // Skip players that already won
        var attempts = 0
        while (state.players[nextIndex].hasWon && attempts < playersCount) {
            nextIndex = (nextIndex + 1) % playersCount
            attempts++
        }

        val nextPlayer = state.players[nextIndex]
        _gameState.update {
            it.copy(
                activePlayerIndex = nextIndex,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                consecutiveSixes = 0,
                movableTokenIds = emptySet(),
                totalTurns = it.totalTurns + 1,
                statusMessage = "${nextPlayer.name}'s turn to roll!"
            )
        }

        checkIfActivePlayerIsBot()
    }

    private fun checkIfActivePlayerIsBot() {
        botJob?.cancel()
        val state = _gameState.value
        if (state.isGameOver || state.turnPhase != TurnPhase.WAITING_FOR_ROLL) return
        val activePlayer = state.activePlayer ?: return

        if (activePlayer.isBot) {
            botJob = viewModelScope.launch {
                delay(700)
                rollDice()
            }
        }
    }

    private fun scheduleBotMove(movableTokens: List<Token>, dice: Int) {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            delay(800)
            val bestToken = selectBestBotToken(movableTokens, dice)
            moveToken(bestToken.id)
        }
    }

    /**
     * Smart Ludo AI heuristic evaluation
     */
    private fun selectBestBotToken(movableTokens: List<Token>, dice: Int): Token {
        val state = _gameState.value
        val botPlayer = state.activePlayer ?: return movableTokens.first()

        var bestToken = movableTokens.first()
        var highestScore = Int.MIN_VALUE

        for (token in movableTokens) {
            var score = 0
            val targetStep = if (token.isInYard) 0 else token.step + dice

            // 1. Can capture an opponent? (Highest priority)
            if (targetStep in 0..50) {
                val landTrackIndex = (botPlayer.color.startTrackIndex + targetStep) % 52
                val isSafe = LudoBoardCoordinates.isSafeTrackCell(landTrackIndex)
                if (!isSafe) {
                    val canCapture = state.players.any { opp ->
                        opp.color != botPlayer.color && opp.tokens.any { oppToken ->
                            oppToken.step in 0..50 &&
                                ((opp.color.startTrackIndex + oppToken.step) % 52) == landTrackIndex
                        }
                    }
                    if (canCapture) {
                        score += 2000
                    }
                }
            }

            // 2. Can reach home goal?
            if (targetStep == 56) {
                score += 1500
            }

            // 3. Can enter home corridor?
            if (token.step <= 50 && targetStep in 51..55) {
                score += 800
            }

            // 4. Release token from yard on 6?
            if (token.isInYard && dice == 6) {
                score += 700
            }

            // 5. Land on a safe star cell?
            if (targetStep in 0..50) {
                val landTrackIndex = (botPlayer.color.startTrackIndex + targetStep) % 52
                if (LudoBoardCoordinates.isSafeTrackCell(landTrackIndex)) {
                    score += 500
                }
            }

            // 6. Escape from opponent behind?
            if (token.step in 0..50) {
                val currentTrackIndex = (botPlayer.color.startTrackIndex + token.step) % 52
                val inDanger = state.players.any { opp ->
                    opp.color != botPlayer.color && opp.tokens.any { oppToken ->
                        if (oppToken.step in 0..50) {
                            val oppTrack = (opp.color.startTrackIndex + oppToken.step) % 52
                            val dist = (currentTrackIndex - oppTrack + 52) % 52
                            dist in 1..6
                        } else false
                    }
                }
                if (inDanger) {
                    score += 600
                }
            }

            // 7. Favor tokens already far along the track
            score += targetStep * 5

            if (score > highestScore) {
                highestScore = score
                bestToken = token
            }
        }

        return bestToken
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        botJob?.cancel()
        animationJob?.cancel()
    }
}
