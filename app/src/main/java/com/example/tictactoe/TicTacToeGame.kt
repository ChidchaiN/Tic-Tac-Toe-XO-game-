package com.example.tictactoe

enum class Player { X, O, NONE }

data class GameState(
    val board: Array<Array<Player>>,
    var currentPlayer: Player,
    var winner: Player? = null
)

object TicTacToeGame {

    private const val MAX_DEPTH = 4

    fun createInitialState(size: Int): GameState {
        return GameState(
            board = Array(size) { Array(size) { Player.NONE } },
            currentPlayer = Player.X
        )
    }

    fun getMaxDepth(size: Int): Int {
        return when (size) {
            4 -> 4
            5 -> 3
            6 -> 2
            else -> 4
        }
    }

    fun findBestMove(board: Array<Array<Player>>, size: Int): Pair<Int, Int> {
        var bestScore = Int.MIN_VALUE
        var bestMove = Pair(-1, -1)
        val depth = 0  // Starting depth

        for (row in board.indices) {
            for (col in board[row].indices) {
                if (board[row][col] == Player.NONE) {
                    board[row][col] = Player.O
                    val score = minimax(board, false, depth + 1, size)
                    board[row][col] = Player.NONE
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = Pair(row, col)
                    }
                }
            }
        }
        return bestMove
    }

    private fun minimax(board: Array<Array<Player>>, isMaximizing: Boolean, depth: Int, size: Int): Int {
        val winner = checkWinner(board)
        if (winner == Player.O) return 10 - depth
        if (winner == Player.X) return depth - 10
        if (isBoardFull(board)) return 0

        if (depth >= getMaxDepth(size)) return 0

        return if (isMaximizing) {
            var bestScore = Int.MIN_VALUE
            for (row in board.indices) {
                for (col in board[row].indices) {
                    if (board[row][col] == Player.NONE) {
                        board[row][col] = Player.O
                        val score = minimax(board, false, depth + 1, size)
                        board[row][col] = Player.NONE
                        bestScore = maxOf(score, bestScore)
                    }
                }
            }
            bestScore
        } else {
            var bestScore = Int.MAX_VALUE
            for (row in board.indices) {
                for (col in board[row].indices) {
                    if (board[row][col] == Player.NONE) {
                        board[row][col] = Player.X
                        val score = minimax(board, true, depth + 1, size)
                        board[row][col] = Player.NONE
                        bestScore = minOf(score, bestScore)
                    }
                }
            }
            bestScore
        }
    }

    fun checkWinner(board: Array<Array<Player>>): Player? {
        // Check rows and columns
        for (i in board.indices) {
            if (board[i].all { it == Player.X } || board.indices.all { board[it][i] == Player.X }) return Player.X
            if (board[i].all { it == Player.O } || board.indices.all { board[it][i] == Player.O }) return Player.O
        }

        // Check diagonals
        if (board.indices.all { board[it][it] == Player.X }) return Player.X
        if (board.indices.all { board[it][it] == Player.O }) return Player.O
        if (board.indices.all { board[it][board.size - 1 - it] == Player.X }) return Player.X
        if (board.indices.all { board[it][board.size - 1 - it] == Player.O }) return Player.O

        return null
    }

    fun isBoardFull(board: Array<Array<Player>>): Boolean {
        return board.all { row -> row.all { cell -> cell != Player.NONE } }
    }
}

