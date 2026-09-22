package com.example.ludo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.PlayerColor
import com.example.ludo.viewmodel.LudoViewModel
import com.example.ludo.viewmodel.TurnPhase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoScreen(viewModel: LudoViewModel) {
    val state by viewModel.gameState.collectAsState()

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    val activePlayer = state.activePlayer
    val activeColor = activePlayer?.color ?: PlayerColor.RED

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PlayerColor.RED.primary, PlayerColor.BLUE.primary)
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ludo King",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier.testTag("rules_button")
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Rules")
                    }
                    IconButton(
                        onClick = { showNewGameDialog = true },
                        modifier = Modifier.testTag("new_game_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "New Game")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Mode and turns badge
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = state.gameMode.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Turn #${state.totalTurns + 1}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Top Player Cards (e.g. Red [top-left] and Green [top-right])
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.players.getOrNull(0)?.let { p0 ->
                    PlayerInfoCard(
                        player = p0,
                        isActive = state.activePlayerIndex == 0,
                        modifier = Modifier.weight(1f)
                    )
                }
                state.players.getOrNull(1)?.let { p1 ->
                    PlayerInfoCard(
                        player = p1,
                        isActive = state.activePlayerIndex == 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The Authentic 15x15 Ludo Board
            LudoBoardView(
                players = state.players,
                activePlayer = activePlayer,
                movableTokenIds = state.movableTokenIds,
                onTokenClicked = { tokenId ->
                    viewModel.onTokenClicked(tokenId)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Player Cards (e.g. Blue [bottom-left] and Yellow [bottom-right])
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // In standard 4-player order: Red(0), Green(1), Yellow(2), Blue(3)
                // Layout bottom-left as Blue(3) or Red(0) depending on count
                val pBottomLeft = if (state.players.size == 4) state.players.getOrNull(3) else state.players.getOrNull(2)
                val pBottomRight = if (state.players.size == 4) state.players.getOrNull(2) else null

                pBottomLeft?.let { p ->
                    val idx = state.players.indexOf(p)
                    PlayerInfoCard(
                        player = p,
                        isActive = state.activePlayerIndex == idx,
                        modifier = Modifier.weight(1f)
                    )
                } ?: Spacer(modifier = Modifier.weight(1f))

                pBottomRight?.let { p ->
                    val idx = state.players.indexOf(p)
                    PlayerInfoCard(
                        player = p,
                        isActive = state.activePlayerIndex == idx,
                        modifier = Modifier.weight(1f)
                    )
                } ?: Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Turn & Interactive Dice Control Panel
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = activeColor.lightContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, activeColor.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .testTag("action_control_panel")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(activeColor.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activePlayer?.name ?: ""}'s Turn",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = activeColor.darkShade
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = state.statusMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        )

                        if (state.turnPhase == TurnPhase.SELECTING_TOKEN && !state.isCurrentPlayerBot) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "👆 Tap the highlighted token on board",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = activeColor.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 3D Animated Dice View
                    DiceView(
                        diceValue = state.diceValue,
                        activeColor = activeColor,
                        turnPhase = state.turnPhase,
                        isBot = state.isCurrentPlayerBot,
                        onDiceClick = {
                            viewModel.onDiceClicked()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (showNewGameDialog) {
        NewGameDialog(
            currentMode = state.gameMode,
            onDismiss = { showNewGameDialog = false },
            onStartGame = { count, mode, botFlags ->
                viewModel.startNewGame(
                    playerCount = count,
                    mode = mode,
                    botFlags = botFlags
                )
            }
        )
    }

    if (showRulesDialog) {
        RulesDialog(onDismiss = { showRulesDialog = false })
    }

    if (state.isGameOver && state.winnersList.isNotEmpty()) {
        VictoryPodiumDialog(
            winners = state.winnersList,
            onPlayAgain = {
                viewModel.startNewGame(
                    playerCount = state.players.size,
                    mode = state.gameMode
                )
            }
        )
    }
}
