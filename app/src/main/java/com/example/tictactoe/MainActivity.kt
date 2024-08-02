package com.example.tictactoe

import android.os.Bundle
import android.widget.AdapterView
import android.widget.Button
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var gameState: GameState
    private lateinit var gridLayout: GridLayout
    private lateinit var gameResult: TextView
    private lateinit var sizeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.grid_layout)
        gameResult = findViewById(R.id.game_result)
        sizeSpinner = findViewById(R.id.size_spinner)

        // Initialize game state and setup
        initGame()
        setupBoard() // Initial setup for 3x3 board

        // Handle spinner selection to change board size
        sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val size = when (position) {
                    0 -> 3
                    1 -> 4
                    2 -> 5
                    3 -> 6
                    else -> 3
                }
                updateBoardSize(size)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun initGame() {
        // Initialize game state with default board size (3x3)
        gameState = GameState(
            board = Array(3) { Array(3) { Player.NONE } },
            currentPlayer = Player.X
        )
    }

    private fun setupBoard() {
        updateBoardSize(3) // Set up the initial board size to 3x3
    }

    private fun updateBoardSize(size: Int) {
        // Initialize new game state
        gameState = GameState(
            board = Array(size) { Array(size) { Player.NONE } },
            currentPlayer = Player.X
        )

        // Update GridLayout
        gridLayout.removeAllViews()
        gridLayout.rowCount = size
        gridLayout.columnCount = size
        for (i in 0 until size) {
            for (j in 0 until size) {
                val button = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    setOnClickListener { handleMove(i, j) }
                }
                gridLayout.addView(button)
            }
        }
        updateUI() // Update UI after setting up the board
    }


    private fun handleMove(row: Int, col: Int) {
        // Ensure the move is valid
        if (gameState.board[row][col] == Player.NONE && gameState.winner == null) {
            // Make the player's move
            gameState.board[row][col] = gameState.currentPlayer
            updateUI()
            // Check if the game is over
            if (checkGameOver()) return

            // Switch to AI's turn
            if (gameState.currentPlayer == Player.X) {
                gameState.currentPlayer = Player.O
                // AI makes its move
                val move = TicTacToeGame.findBestMove(gameState.board, gameState.board.size)
                gameState.board[move.first][move.second] = Player.O
                updateUI()
                // Check if the game is over after AI's move
                checkGameOver()
                // Switch back to player's turn
                gameState.currentPlayer = Player.X
            }
        }
    }

    private fun checkGameOver(): Boolean {
        val winner = TicTacToeGame.checkWinner(gameState.board)
        if (winner != null) {
            gameState.winner = winner
            displayResult(winner)
            disableBoard()
            return true
        }

        if (TicTacToeGame.isBoardFull(gameState.board)) {
            gameState.winner = Player.NONE
            displayResult(gameState.winner)
            disableBoard()
            return true
        }

        return false
    }



    private fun displayResult(winner: Player?) {
        gameResult.text = when (winner) {
            Player.X -> "Player X Wins!"
            Player.O -> "Player O Wins!"
            Player.NONE -> "It's a Draw!"
            else -> "Game Over"
        }
    }

    private fun disableBoard() {
        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            button.isEnabled = false
        }
    }

    private fun updateUI() {
        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            val row = i / gameState.board.size
            val col = i % gameState.board.size
            button.text = when (gameState.board[row][col]) {
                Player.X -> "X"
                Player.O -> "O"
                else -> ""
            }
        }
    }
}
