package com.example

import com.example.ludo.model.LudoBoardCoordinates
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.Token
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTokenYardMovement() {
    val yardToken = Token(id = 0, color = PlayerColor.RED, step = -1)
    assertFalse("Token in yard cannot move on 5", yardToken.canMove(5))
    assertTrue("Token in yard can move on 6", yardToken.canMove(6))
  }

  @Test
  fun testTokenGoalBoundary() {
    val tokenNearGoal = Token(id = 0, color = PlayerColor.RED, step = 54)
    assertTrue("Token at 54 can move 2 steps to 56", tokenNearGoal.canMove(2))
    assertFalse("Token at 54 cannot overshoot 56 with 3", tokenNearGoal.canMove(3))
  }

  @Test
  fun testSafeTrackCells() {
    assertTrue("Red start cell 0 is safe", LudoBoardCoordinates.isSafeTrackCell(0))
    assertTrue("Star cell 8 is safe", LudoBoardCoordinates.isSafeTrackCell(8))
    assertFalse("Cell 1 is regular cell", LudoBoardCoordinates.isSafeTrackCell(1))
  }
}
