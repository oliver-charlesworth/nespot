package choliver.nespot.playtest.engine

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.AudioSink
import choliver.nespot.nes.VideoSink
import choliver.nespot.nes.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.playtest.engine.Scenario.Stimulus
import choliver.nespot.playtest.engine.ScenarioCaptor.StepHandler
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

fun liveCapture(rom: Rom, snapshotPattern: SnapshotPattern) =
  captureWithUi(rom) { events -> LiveHandler(snapshotPattern, events) }

fun uiGhostCapture(rom: Rom, ghost: Scenario) =
  captureWithUi(rom) { GhostHandler(ghost) }

fun headlessGhostCapture(rom: Rom, ghost: Scenario) =
  captureHeadless(rom, GhostHandler(ghost))

private fun captureWithUi(
  rom: Rom,
  createHandler: (events: Queue<Event>) -> StepHandler
): Scenario {
  val events = LinkedBlockingQueue<Event>()
  Screen(onEvent = { events += it }).use { screen ->
    AudioPlayer().use { audio ->
      val captor = ScenarioCaptor(
        rom = rom,
        videoSink = screen.sink,
        audioSink = audio.sink
      )

      screen.show()
      audio.start()

      return captor.capture(createHandler(events))
    }
  }
}

private fun captureHeadless(
  rom: Rom,
  handler: StepHandler
): Scenario {
  val captor = ScenarioCaptor(
    rom = rom,
    videoSink = object : VideoSink {
      override val colorPackingMode = BGRA
    },
    audioSink = object : AudioSink {}
  )

  return captor.capture(handler)
}

private class LiveHandler(
  private val snapshotPattern: SnapshotPattern,
  private val events: Queue<Event>
) : StepHandler {
  override fun ScenarioCaptor.handleStep(timestamp: Long) {
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
}

private class GhostHandler(
  private val ghost: Scenario
) : StepHandler {
  private var idx = 0

  override fun ScenarioCaptor.handleStep(timestamp: Long) {
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
