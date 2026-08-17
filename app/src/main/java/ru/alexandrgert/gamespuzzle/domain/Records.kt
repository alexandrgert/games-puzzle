package ru.alexandrgert.gamespuzzle.domain

data class BestRecord(val bestTimeMs: Long?, val bestMoves: Int?)

fun mergeRecord(previous: BestRecord?, timeMs: Long, moves: Int): BestRecord {
    val time = listOfNotNull(previous?.bestTimeMs, timeMs).minOrNull()
    val mv = listOfNotNull(previous?.bestMoves, moves).minOrNull()
    return BestRecord(time, mv)
}
