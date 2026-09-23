package ru.alexandrgert.gamespuzzle.ui.play

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.alexandrgert.gamespuzzle.domain.GridSize

class PlayLifecycleTest {
    @Test
    fun stopAndStartEventsExcludeBackgroundTimeThroughScreenObserver() {
        var now = 1_000L
        val viewModel = PlayViewModel(currentTimeMillis = { now })
        viewModel.start(GridSize.FIVE, Random(27L))
        val owner = TestLifecycleOwner()
        owner.lifecycle.addObserver(playLifecycleObserver(viewModel))
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

        now = 1_500L
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        now = 9_000L
        owner.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        now = 9_300L
        viewModel.tick()

        assertEquals(800L, viewModel.state!!.elapsedMs)
    }

    private class TestLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }
}
