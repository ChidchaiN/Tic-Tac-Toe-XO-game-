package com.example.tictactoe

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var gameState: GameState
    private lateinit var gridLayout: GridLayout
    private lateinit var gameResult: TextView
    private lateinit var sizeSpinner: Spinner
    private lateinit var replaySpinner: Spinner
    private lateinit var databaseHelper: DatabaseHelper

    private var currentGameID: Int = 0
    private var moveCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayout = findViewById(R.id.grid_layout)
        gameResult = findViewById(R.id.game_result)
        sizeSpinner = findViewById(R.id.size_spinner)
        replaySpinner = findViewById(R.id.replay_spinner)
        databaseHelper = DatabaseHelper(this)

        initGame()
        setupBoard()
        setupSizeSpinner()
        // Fetch games using AsyncTask
        fetchData()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val games = databaseHelper.getGames()
                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, games)
                    replaySpinner.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching games", e)
            }
        }
    }


    private fun initGame() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val games = databaseHelper.getGames()
                withContext(Dispatchers.Main) {
                    currentGameID = (games.maxOrNull() ?: 0) + 1
                    moveCount = 0
                    Log.d("MainActivity", "Game initialized: currentGameID=$currentGameID")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing game", e)
            }
        }
    }





    private fun setupBoard() {
        updateBoardSize(3)
        Log.d("MainActivity", "Board setup with size 3x3")
    }


    private fun setupSizeSpinner() {
        sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val size = when (position) {
                    0 -> 3
                    1 -> 4
                    2 -> 5
                    3 -> 6
                    else -> 3
                }
                Log.d("MainActivity", "Size selected: $size")
                try {
                    updateBoardSize(size)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error updating board size", e)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }


    private fun setupReplaySpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch games from the database
                val games = databaseHelper.getGames()

                withContext(Dispatchers.Main) {
                    // Update the spinner adapter with the fetched games
                    val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, games)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    replaySpinner.adapter = adapter

                    // Set up the spinner item selected listener
                    replaySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            val gameID = games[position]
                            Log.d("MainActivity", "Replay game selected: $gameID")
                            CoroutineScope(Dispatchers.IO).launch {
                                replayGame(gameID)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Do nothing
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error setting up replay spinner", e)
            }
        }
    }



    private fun updateBoardSize(size: Int) {
        gameState = GameState(
            board = Array(size) { Array(size) { Player.NONE } },
            currentPlayer = Player.X
        )

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
        Log.d("MainActivity", "Board updated with size $size")
        updateUI()
    }


    private fun handleMove(row: Int, col: Int) {
        if (gameState.board[row][col] == Player.NONE && gameState.winner == null) {
            gameState.board[row][col] = gameState.currentPlayer
            moveCount++
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    databaseHelper.insertMove(currentGameID, moveCount, gameState.currentPlayer.name[0], row, col)
                    withContext(Dispatchers.Main) {
                        updateUI()
                    }
                    if (checkGameOver()) return@launch
                    if (gameState.currentPlayer == Player.X) {
                        gameState.currentPlayer = Player.O
                        val move = TicTacToeGame.findBestMove(gameState.board, gameState.board.size)
                        gameState.board[move.first][move.second] = Player.O
                        moveCount++
                        databaseHelper.insertMove(currentGameID, moveCount, Player.O.name[0], move.first, move.second)
                        withContext(Dispatchers.Main) {
                            updateUI()
                            checkGameOver()
                            gameState.currentPlayer = Player.X
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error handling move", e)
                }
            }
        }
    }



    private fun checkGameOver(): Boolean {
        val winner = TicTacToeGame.checkWinner(gameState.board)
        if (winner != null) {
            gameState.winner = winner
            displayResult(winner)
            disableBoard()
            Log.d("MainActivity", "Game over, winner: $winner")
            return true
        }

        if (TicTacToeGame.isBoardFull(gameState.board)) {
            gameState.winner = Player.NONE
            displayResult(gameState.winner)
            disableBoard()
            Log.d("MainActivity", "Game over, it's a draw")
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
        Log.d("MainActivity", "Result displayed: ${gameResult.text}")
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
            button.isEnabled = gameState.board[row][col] == Player.NONE && gameState.winner == null
        }
        Log.d("MainActivity", "UI updated")
    }



    private fun replayGame(gameID: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val moves = databaseHelper.getMoves(gameID)
                withContext(Dispatchers.Main) {
                    gameState = GameState(
                        board = Array(gameState.board.size) { Array(gameState.board.size) { Player.NONE } },
                        currentPlayer = Player.X
                    )
                    disableBoard()
                    Log.d("MainActivity", "Replaying game with ID: $gameID")
                    for (move in moves) {
                        gameState.board[move.row][move.col] = if (move.player == 'X') Player.X else Player.O
                        updateUI()
                        Log.d("MainActivity", "Move replayed: ${move.player} at (${move.row}, ${move.col})")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error replaying game", e)
            }
        }
    }


}
