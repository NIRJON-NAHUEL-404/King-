package com.example.ludo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
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
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            // Mode, Turn Status & Counter badge bar (Strictly fixed height to prevent any layout jumping)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = state.gameMode.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Live Game Status Badge (occupies dynamic middle width, single-line, zero jitter)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = activeColor.cyberPlate,
                    border = BorderStroke(1.dp, activeColor.neonGlow.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(activeColor.neonGlow)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${activePlayer?.name ?: ""}: ${state.statusMessage}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Turn #${state.totalTurns + 1}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Top Player Units (Red [top-left] and Green [top-right])
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                val pRed = state.players.find { it.color == PlayerColor.RED }
                val pGreen = state.players.find { it.color == PlayerColor.GREEN }

                // Red quadrant (Top-Left): Big Dice on top hugged to left corner, Card underneath
                pRed?.let { p ->
                    val idx = state.players.indexOf(p)
                    val isActive = state.activePlayerIndex == idx
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        CornerDiceBox(
                            player = p,
                            isActive = isActive,
                            diceValue = state.playerDiceValues[p.color] ?: 1,
                            turnPhase = state.turnPhase,
                            onDiceClick = { viewModel.onPlayerDiceClicked(p.color) },
                            diceSize = 54.dp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        PlayerInfoCard(
                            player = p,
                            isActive = isActive,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))

                // Green quadrant (Top-Right): Big Dice on top hugged to right corner, Card underneath
                pGreen?.let { p ->
                    val idx = state.players.indexOf(p)
                    val isActive = state.activePlayerIndex == idx
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        CornerDiceBox(
                            player = p,
                            isActive = isActive,
                            diceValue = state.playerDiceValues[p.color] ?: 1,
                            turnPhase = state.turnPhase,
                            onDiceClick = { viewModel.onPlayerDiceClicked(p.color) },
                            diceSize = 54.dp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        PlayerInfoCard(
                            player = p,
                            isActive = isActive,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // The Authentic 15x15 Ludo Board
            LudoBoardView(
                players = state.players,
                activePlayer = activePlayer,
                movableTokenIds = state.movableTokenIds,
                onTokenClicked = { tokenId ->
                    viewModel.onTokenClicked(tokenId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Player Units (Blue [bottom-left] and Yellow [bottom-right])
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                val pBlue = state.players.find { it.color == PlayerColor.BLUE }
                val pYellow = state.players.find { it.color == PlayerColor.YELLOW }

                // Blue quadrant (Bottom-Left): Card on top, Big Dice underneath hugged to left corner
                pBlue?.let { p ->
                    val idx = state.players.indexOf(p)
                    val isActive = state.activePlayerIndex == idx
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(1f)
                    ) {
                        PlayerInfoCard(
                            player = p,
                            isActive = isActive,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        CornerDiceBox(
                            player = p,
                            isActive = isActive,
                            diceValue = state.playerDiceValues[p.color] ?: 1,
                            turnPhase = state.turnPhase,
                            onDiceClick = { viewModel.onPlayerDiceClicked(p.color) },
                            diceSize = 54.dp
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))

                // Yellow quadrant (Bottom-Right): Card on top, Big Dice underneath hugged to right corner
                pYellow?.let { p ->
                    val idx = state.players.indexOf(p)
                    val isActive = state.activePlayerIndex == idx
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        PlayerInfoCard(
                            player = p,
                            isActive = isActive,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        CornerDiceBox(
                            player = p,
                            isActive = isActive,
                            diceValue = state.playerDiceValues[p.color] ?: 1,
                            turnPhase = state.turnPhase,
                            onDiceClick = { viewModel.onPlayerDiceClicked(p.color) },
                            diceSize = 54.dp
                        )
                    }
                } ?: Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(6.dp))
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
