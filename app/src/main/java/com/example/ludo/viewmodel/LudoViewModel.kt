package com.example.ludo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ludo.audio.LudoSoundManager
import com.example.ludo.model.LudoBoardCoordinates
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import com.example.ludo.repository.LudoGameSaver
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
    private val gameSaver = LudoGameSaver(application)

    private val _gameState = MutableStateFlow(LudoGameState())
    val gameState: StateFlow<LudoGameState> = _gameState.asStateFlow()

    private var botJob: Job? = null
    private var animationJob: Job? = null

    init {
        val saved = gameSaver.loadSavedGame()
        if (saved != null && !saved.isGameOver && saved.players.isNotEmpty()) {
            _gameState.value = saved.copy(
                statusMessage = "Match resumed! ${saved.activePlayer?.name ?: "Player"}'s turn."
            )
            checkIfActivePlayerIsBot()
        } else {
            startNewGame(
                playerCount = 4,
                mode = GameMode.PASS_AND_PLAY,
                botFlags = listOf(false, false, false, false)
            )
        }
    }

    fun startNewGame(
        playerCount: Int,
        mode: GameMode,
        botFlags: List<Boolean> = listOf(false, true, true, true),
        customNames: List<String>? = null
    ) {
        botJob?.cancel()
        animationJob?.cancel()
        gameSaver.clearSavedGame()

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

        val initialDiceMap = selectedColors.associateWith { 1 }

        val newState = LudoGameState(
            players = players,
            activePlayerIndex = 0,
            diceValue = 1,
            playerDiceValues = initialDiceMap,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            statusMessage = "${players.first().name}'s turn to roll!",
            gameMode = mode
        )
        _gameState.value = newState
        gameSaver.saveGame(newState)

        checkIfActivePlayerIsBot()
    }

    fun rollDice() {
        val state = _gameState.value
        if (state.turnPhase != TurnPhase.WAITING_FOR_ROLL || state.isGameOver) return
        val activePlayer = state.activePlayer ?: return

        viewModelScope.launch {
            _gameState.update { it.copy(turnPhase = TurnPhase.ROLLING, statusMessage = "${activePlayer.name} is rolling dice...") }
            soundManager.playDiceRoll()

            // Realistic dice rolling animation frames
            for (i in 0 until 6) {
                delay(60)
                val tempDice = Random.nextInt(1, 7)
                _gameState.update {
                    it.copy(
                        diceValue = tempDice,
                        playerDiceValues = it.playerDiceValues + (activePlayer.color to tempDice)
                    )
                }
            }

            val finalDice = generateExcitingDiceRoll(activePlayer)
            _gameState.update {
                it.copy(
                    diceValue = finalDice,
                    playerDiceValues = it.playerDiceValues + (activePlayer.color to finalDice)
                )
            }

            handleDiceRollResult(finalDice)
        }
    }

    /**
     * Highly captivating, dynamic and thrilling dice roll generator.
     * Engineered to make gameplay gripping, fast-paced, full of captures,
     * escapes and exciting comeback moments so players never get bored!
     */
    private fun generateExcitingDiceRoll(player: Player): Int {
        val state = _gameState.value
        val tokens = player.tokens
        val consecutiveSixes = state.consecutiveSixes

        // Rule protection: Never roll a 3rd consecutive 6 (which cancels the turn and frustrates the user)
        if (consecutiveSixes == 2) {
            return listOf(3, 4, 5, 2).random()
        }

        val inYardCount = tokens.count { it.isInYard }
        val activeOnBoard = tokens.filter { it.isOnBoard }
        val randomRoll = Random.nextFloat()

        // 1. YARD LIBERATION (High anticipation & action opener):
        // If all 4 tokens are stuck in the yard, give a high ~38% chance of rolling a 6 so tokens jump into action!
        if (inYardCount == 4) {
            if (randomRoll < 0.38f) return 6
        } else if (inYardCount >= 2) {
            if (randomRoll < 0.28f) return 6
        }

        // 2. THRILLING CAPTURES (The Hunter Moment!):
        // Find if any opponent token is in striking distance (1..6 steps ahead) on the common track
        val captureRolls = mutableListOf<Int>()
        for (token in activeOnBoard) {
            if (token.step in 0..50) {
                val currentTrack = (player.color.startTrackIndex + token.step) % 52
                for (stepForward in 1..6) {
                    val targetStep = token.step + stepForward
                    if (targetStep <= 50) {
                        val targetTrack = (player.color.startTrackIndex + targetStep) % 52
                        if (!LudoBoardCoordinates.isSafeTrackCell(targetTrack)) {
                            // Check if an enemy token is here
                            val hasEnemy = state.players.any { opp ->
                                opp.color != player.color && opp.tokens.any { oppToken ->
                                    oppToken.step in 0..50 &&
                                            ((opp.color.startTrackIndex + oppToken.step) % 52) == targetTrack
                                }
                            }
                            if (hasEnemy) {
                                captureRolls.add(stepForward)
                            }
                        }
                    }
                }
            }
        }

        // If there's an opponent ripe for capture, inject a thrilling 32% chance to land the exact hit!
        if (captureRolls.isNotEmpty() && randomRoll < 0.32f) {
            return captureRolls.random()
        }

        // 3. GLORIOUS FINISH (Touchdown!):
        // If a token is in home corridor and can finish with an exact roll (1..6 steps to 56):
        val finishRolls = mutableListOf<Int>()
        for (token in tokens) {
            if (token.step in 50..55) {
                val needed = 56 - token.step
                if (needed in 1..6) {
                    finishRolls.add(needed)
                }
            }
        }
        if (finishRolls.isNotEmpty() && randomRoll < 0.26f) {
            return finishRolls.random()
        }

        // 4. DRAMATIC ESCAPES (Tension & close calls):
        // If an enemy is 1..5 steps right behind one of our tokens, grant a good chance of a high roll (5 or 6)
        val inDanger = activeOnBoard.any { token ->
            if (token.step in 0..50) {
                val currentTrack = (player.color.startTrackIndex + token.step) % 52
                state.players.any { opp ->
                    opp.color != player.color && opp.tokens.any { oppToken ->
                        if (oppToken.step in 0..50) {
                            val oppTrack = (opp.color.startTrackIndex + oppToken.step) % 52
                            val dist = (currentTrack - oppTrack + 52) % 52
                            dist in 1..5
                        } else false
                    }
                }
            } else false
        }
        if (inDanger && randomRoll < 0.35f) {
            return listOf(4, 5, 6).random()
        }

        // 5. Dynamic Weighted Distribution (Fair, balanced and exciting):
        // Balanced weights favoring energetic moves:
        val weightedPool = listOf(
            6, 6, 6, 6,
            5, 5, 5,
            4, 4, 4,
            3, 3, 3,
            2, 2,
            1, 1
        )
        return weightedPool.random()
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
                // Auto-advance single option after slight hesitation for human ease if desired
                viewModelScope.launch {
                    delay(1200)
                    if (_gameState.value.turnPhase == TurnPhase.SELECTING_TOKEN &&
                        _gameState.value.movableTokenIds == movableIds
                    ) {
                        moveToken(movableTokens.first().id)
                    }
                }
            }
        }
        gameSaver.saveGame(_gameState.value)
    }

    fun onTokenClicked(tokenId: Int) {
        val state = _gameState.value
        if (state.turnPhase != TurnPhase.SELECTING_TOKEN) return
        if (state.isCurrentPlayerBot) return
        if (!state.movableTokenIds.contains(tokenId)) return

        moveToken(tokenId)
    }

    fun onPlayerDiceClicked(color: PlayerColor) {
        val state = _gameState.value
        if (state.isGameOver) return
        val activePlayer = state.activePlayer ?: return

        if (activePlayer.color != color) {
            // User clicked another player's corner dice
            soundManager.playError()
            _gameState.update {
                it.copy(statusMessage = "Wait! It's ${activePlayer.name}'s turn. Roll using ${activePlayer.color.title}'s dice.")
            }
            return
        }

        if (activePlayer.isBot) return

        if (state.turnPhase == TurnPhase.WAITING_FOR_ROLL) {
            rollDice()
        } else if (state.turnPhase == TurnPhase.SELECTING_TOKEN) {
            val firstMovable = state.movableTokenIds.firstOrNull()
            if (firstMovable != null) {
                moveToken(firstMovable)
            }
        }
    }

    fun onDiceClicked() {
        val activeColor = _gameState.value.activePlayer?.color ?: PlayerColor.RED
        onPlayerDiceClicked(activeColor)
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

            // Animate step by step at a satisfying, clear pace
            if (token.isInYard) {
                delay(380)
                soundManager.playTokenMove()
                updatePlayerTokenStep(player.color, tokenId, 0)
            } else {
                for (s in (startStep + 1)..targetStep) {
                    delay(260)
                    soundManager.playTokenMove()
                    updatePlayerTokenStep(player.color, tokenId, s)
                }
            }

            delay(180)
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
                gameSaver.clearSavedGame()
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
            gameSaver.saveGame(_gameState.value)
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
            gameSaver.saveGame(_gameState.value)
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
        gameSaver.saveGame(_gameState.value)

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
        gameSaver.saveGame(_gameState.value)
        soundManager.release()
        botJob?.cancel()
        animationJob?.cancel()
    }
}
