package com.example.ludo.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import com.example.ludo.viewmodel.GameMode
import com.example.ludo.viewmodel.LudoGameState
import com.example.ludo.viewmodel.TurnPhase
import org.json.JSONArray
import org.json.JSONObject

class LudoGameSaver(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ludo_game_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SAVED_GAME = "active_saved_game_json"
    }

    fun saveGame(state: LudoGameState) {
        if (state.isGameOver || state.players.isEmpty()) {
            clearSavedGame()
            return
        }

        try {
            val root = JSONObject()
            root.put("activePlayerIndex", state.activePlayerIndex)
            root.put("diceValue", state.diceValue)
            root.put("consecutiveSixes", state.consecutiveSixes)
            root.put("totalTurns", state.totalTurns)
            root.put("gameMode", state.gameMode.name)

            // Safe turn phase for resumption
            val safePhase = when (state.turnPhase) {
                TurnPhase.ROLLING, TurnPhase.MOVING_TOKEN, TurnPhase.TURN_TRANSITION -> TurnPhase.WAITING_FOR_ROLL
                else -> state.turnPhase
            }
            root.put("turnPhase", safePhase.name)
            root.put("statusMessage", state.statusMessage)

            val movableArray = JSONArray()
            state.movableTokenIds.forEach { movableArray.put(it) }
            root.put("movableTokenIds", movableArray)

            val diceMapObj = JSONObject()
            state.playerDiceValues.forEach { (color, value) ->
                diceMapObj.put(color.name, value)
            }
            root.put("playerDiceValues", diceMapObj)

            val playersArray = JSONArray()
            for (player in state.players) {
                val playerObj = JSONObject()
                playerObj.put("color", player.color.name)
                playerObj.put("name", player.name)
                playerObj.put("isBot", player.isBot)
                playerObj.put("isActiveInGame", player.isActiveInGame)
                if (player.rank != null) {
                    playerObj.put("rank", player.rank)
                }

                val tokensArray = JSONArray()
                for (token in player.tokens) {
                    val tokenObj = JSONObject()
                    tokenObj.put("id", token.id)
                    tokenObj.put("step", token.step)
                    tokensArray.put(tokenObj)
                }
                playerObj.put("tokens", tokensArray)
                playersArray.put(playerObj)
            }
            root.put("players", playersArray)

            prefs.edit().putString(KEY_SAVED_GAME, root.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSavedGame(): LudoGameState? {
        val jsonStr = prefs.getString(KEY_SAVED_GAME, null) ?: return null
        return try {
            val root = JSONObject(jsonStr)
            val activePlayerIndex = root.optInt("activePlayerIndex", 0)
            val diceValue = root.optInt("diceValue", 1)
            val consecutiveSixes = root.optInt("consecutiveSixes", 0)
            val totalTurns = root.optInt("totalTurns", 0)
            val gameMode = try {
                GameMode.valueOf(root.optString("gameMode", GameMode.PASS_AND_PLAY.name))
            } catch (e: Exception) {
                GameMode.PASS_AND_PLAY
            }
            val turnPhase = try {
                TurnPhase.valueOf(root.optString("turnPhase", TurnPhase.WAITING_FOR_ROLL.name))
            } catch (e: Exception) {
                TurnPhase.WAITING_FOR_ROLL
            }
            val statusMessage = root.optString("statusMessage", "Resuming match...")

            val movableSet = mutableSetOf<Int>()
            val movableArray = root.optJSONArray("movableTokenIds")
            if (movableArray != null) {
                for (i in 0 until movableArray.length()) {
                    movableSet.add(movableArray.getInt(i))
                }
            }

            val diceMap = mutableMapOf<PlayerColor, Int>()
            val diceMapObj = root.optJSONObject("playerDiceValues")
            if (diceMapObj != null) {
                for (key in diceMapObj.keys()) {
                    try {
                        val color = PlayerColor.valueOf(key)
                        diceMap[color] = diceMapObj.getInt(key)
                    } catch (_: Exception) {}
                }
            }

            val playersList = mutableListOf<Player>()
            val playersArray = root.optJSONArray("players") ?: return null
            for (i in 0 until playersArray.length()) {
                val pObj = playersArray.getJSONObject(i)
                val color = PlayerColor.valueOf(pObj.getString("color"))
                val name = pObj.getString("name")
                val isBot = pObj.optBoolean("isBot", false)
                val isActiveInGame = pObj.optBoolean("isActiveInGame", true)
                val rank = if (pObj.has("rank")) pObj.getInt("rank") else null

                val tokensList = mutableListOf<Token>()
                val tokensArray = pObj.optJSONArray("tokens")
                if (tokensArray != null) {
                    for (t in 0 until tokensArray.length()) {
                        val tObj = tokensArray.getJSONObject(t)
                        tokensList.add(
                            Token(
                                id = tObj.getInt("id"),
                                color = color,
                                step = tObj.getInt("step")
                            )
                        )
                    }
                } else {
                    for (id in 0 until 4) {
                        tokensList.add(Token(id = id, color = color, step = -1))
                    }
                }

                playersList.add(
                    Player(
                        color = color,
                        name = name,
                        isBot = isBot,
                        tokens = tokensList,
                        rank = rank,
                        isActiveInGame = isActiveInGame
                    )
                )
            }

            if (playersList.isEmpty()) return null

            playersList.forEach { p ->
                if (!diceMap.containsKey(p.color)) {
                    diceMap[p.color] = 1
                }
            }

            LudoGameState(
                players = playersList,
                activePlayerIndex = activePlayerIndex.coerceIn(0, playersList.size - 1),
                diceValue = diceValue.coerceIn(1, 6),
                turnPhase = turnPhase,
                consecutiveSixes = consecutiveSixes,
                movableTokenIds = movableSet,
                statusMessage = statusMessage,
                gameMode = gameMode,
                totalTurns = totalTurns,
                playerDiceValues = diceMap
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearSavedGame() {
        prefs.edit().remove(KEY_SAVED_GAME).apply()
    }
}
