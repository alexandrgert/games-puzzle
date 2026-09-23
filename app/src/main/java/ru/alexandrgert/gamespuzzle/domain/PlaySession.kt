package ru.alexandrgert.gamespuzzle.domain

import java.io.Serializable
import java.util.Random
import kotlinx.serialization.Serializable as KotlinSerializable

@KotlinSerializable
data class PlaySessionSnapshot(
    val sizeN: Int,
    val statsEnabled: Boolean,
    val tiles: IntArray,
    val locked: BooleanArray,
    val selectedIndex: Int,
    val moves: Int,
    val peek: Boolean,
    val elapsedMs: Long,
    val completed: Boolean,
) : Serializable

class PlaySession(
    val size: GridSize,
    val statsEnabled: Boolean,
    random: Random,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private var startedAtMs = currentTimeMillis()
    private var wonAtMs: Long? = null
    private var pausedElapsedMs: Long? = null

    var board: Board = BoardEngine.shuffle(size, random)
        private set
    var selected: Cell? = null
        private set
    var moves: Int = 0
        private set
    var peek: Boolean = false
        private set
    val elapsedMs: Long
        get() = pausedElapsedMs ?: ((wonAtMs ?: currentTimeMillis()) - startedAtMs).coerceAtLeast(0L)

    fun pauseTimer() {
        if (wonAtMs == null && pausedElapsedMs == null) pausedElapsedMs = elapsedMs
    }

    fun resumeTimer() {
        val elapsed = pausedElapsedMs ?: return
        startedAtMs = currentTimeMillis() - elapsed
        pausedElapsedMs = null
    }

    fun tap(cell: Cell): MoveResult? {
        if (!cell.inBounds(size.n) || board.isLockedCell(cell)) return null

        val first = selected
        if (first == null) {
            selected = cell
            return null
        }
        if (first == cell) {
            selected = null
            return null
        }

        return swap(first, cell)
    }

    fun swap(a: Cell, b: Cell): MoveResult? {
        if (a == b || !a.inBounds(size.n) || !b.inBounds(size.n)) return null

        selected = null
        val result = BoardEngine.trySwap(board, a, b)
        when (result) {
            is MoveResult.Applied -> {
                board = result.board
                moves++
                if (isWin() && wonAtMs == null) wonAtMs = currentTimeMillis()
            }
            is MoveResult.Reverted -> Unit
        }
        return result
    }

    fun tileShownAt(cell: Cell): Int = board.tileAt(cell)

    fun togglePeek() {
        peek = !peek
    }

    fun isWin(): Boolean = BoardEngine.isWin(board)

    fun snapshot(): PlaySessionSnapshot = PlaySessionSnapshot(
        sizeN = size.n,
        statsEnabled = statsEnabled,
        tiles = board.tiles.copyOf(),
        locked = board.locked.copyOf(),
        selectedIndex = selected?.index(size.n) ?: NO_SELECTION,
        moves = moves,
        peek = peek,
        elapsedMs = elapsedMs,
        completed = wonAtMs != null,
    )

    companion object {
        private const val NO_SELECTION = -1

        fun restore(
            snapshot: PlaySessionSnapshot,
            currentTimeMillis: () -> Long = System::currentTimeMillis,
        ): PlaySession? = runCatching {
            val size = GridSize.entries.single { it.n == snapshot.sizeN }
            require(snapshot.moves >= 0)
            require(snapshot.elapsedMs >= 0L)
            val board = Board(size, snapshot.tiles, snapshot.locked)
            require(snapshot.completed == BoardEngine.isWin(board))
            val selected = when (snapshot.selectedIndex) {
                NO_SELECTION -> null
                else -> Cell.fromIndex(snapshot.selectedIndex, size.n).also {
                    require(snapshot.selectedIndex in snapshot.tiles.indices)
                    require(!board.isLockedCell(it))
                }
            }
            val now = currentTimeMillis()
            PlaySession(size, snapshot.statsEnabled, Random(0L), currentTimeMillis).apply {
                this.board = board
                this.selected = selected
                this.moves = snapshot.moves
                this.peek = snapshot.peek
                this.startedAtMs = now - snapshot.elapsedMs
                this.wonAtMs = if (snapshot.completed) now else null
            }
        }.getOrNull()
    }
}
