package choliver.nespot.playtest.engine

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.AudioSink
import choliver.nespot.nes.VideoSink
import choliver.nespot.nes.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.playtest.engine.Scenario.Stimulus
import choliver.nespot.playtest.engine.SnapshotPattern.FINAL
import choliver.nespot.playtest.engine.SnapshotPattern.PERIODIC
import choliver.nespot.ui.AudioPlayer
import choliver.nespot.ui.Event
import choliver.nespot.ui.Event.*
import choliver.nespot.ui.KeyAction
import choliver.nespot.ui.KeyAction.Joypad
import choliver.nespot.ui.Screen
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

enum class SnapshotPattern {
  PERIODIC,
  FINAL
}

fun liveCapture(rom: Rom, snapshotPattern: SnapshotPattern) = withUi { screen, audio, events ->
  ScenarioCaptor(rom, screen.sink, audio.sink)
    .capture(liveInjector(snapshotPattern, events))
}

fun uiGhostCapture(rom: Rom, ghost: Scenario) = withUi { screen, audio, _ ->
  ScenarioCaptor(rom, screen.sink, audio.sink)
    .capture(ghostInjector(ghost))
}

fun headlessGhostCapture(rom: Rom, ghost: Scenario) = ScenarioCaptor(
  rom,
  object : VideoSink { override val colorPackingMode = BGRA },
  object : AudioSink {}
).capture(ghostInjector(ghost))

private fun <R> withUi(block: (Screen, AudioPlayer, Queue<Event>) -> R): R {
  val events = LinkedBlockingQueue<Event>()
  Screen(onEvent = { events += it }).use { screen ->
    AudioPlayer().use { audio ->
      screen.show()
      audio.start()
      return block(screen, audio, events)
    }
  }
}

private fun liveInjector(
  snapshotPattern: SnapshotPattern,
  events: Queue<Event>
): ScenarioCaptor.() -> Unit = {
  if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0) && snapshotPattern == PERIODIC) {
    takeSnapshot()
  }
  when (val e = events.poll()) {
    is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
      is Joypad -> buttonDown(action.button)
    }
    is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
      is Joypad -> buttonUp(action.button)
    }
    is Close -> {
      if (snapshotPattern == FINAL) {
        takeSnapshot()
      }
      close()
    }
    is Error -> throw RuntimeException()
  }
}

private fun ghostInjector(ghost: Scenario): ScenarioCaptor.() -> Unit {
  var idx = 0
  return {
    if (timestamp >= ghost.stimuli[idx].timestamp) {
      when (val s = ghost.stimuli[idx]) {
        is Stimulus.ButtonDown -> buttonDown(s.button)
        is Stimulus.ButtonUp -> buttonUp(s.button)
        is Stimulus.Snapshot -> takeSnapshot()
        is Stimulus.Close -> close()
      }
      idx++
    }
  }
}

private const val SNAPSHOT_PERIOD = 1_000_000
