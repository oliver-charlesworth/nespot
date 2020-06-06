package choliver.nespot.playtest.engine

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.AudioSink
import choliver.nespot.nes.VideoSink
import choliver.nespot.nes.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.playtest.engine.Scenario.Stimulus
import choliver.nespot.playtest.engine.SnapshotPattern.FINAL
import choliver.nespot.playtest.engine.SnapshotPattern.PERIODIC
import choliver.nespot.runner.AudioPlayer
import choliver.nespot.runner.Event
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction
import choliver.nespot.runner.KeyAction.Joypad
import choliver.nespot.runner.Screen
import java.util.concurrent.LinkedBlockingQueue

enum class SnapshotPattern {
  PERIODIC,
  FINAL
}

/**
 * Capture scenario based on live user input.
 */
fun liveCapture(
  rom: Rom,
  snapshotPattern: SnapshotPattern
): Scenario {
  val events = LinkedBlockingQueue<Event>()
  val screen = Screen(onEvent = { events += it })
  val audio = AudioPlayer()

  val core = ScenarioCaptor(
    rom = rom,
    videoSink = screen.sink,
    audioSink = audio.sink
  )

  screen.show()
  audio.start()

  try {
    return core.capture { timestamp ->
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
  } finally {
    screen.hide()
    screen.close()
    audio.close()
  }
}

/**
 * Capture scenario based on ghosting a previous scenario.
 */
fun ghostCapture(
  rom: Rom,
  ghost: Scenario
): Scenario {
  val captor = ScenarioCaptor(
    rom = rom,
    videoSink = object : VideoSink {
      override val colorPackingMode = BGRA
    },
    audioSink = object : AudioSink {}
  )

  var idxGhost = 0
  return captor.capture { timestamp ->
    if (timestamp >= ghost.stimuli[idxGhost].timestamp) {
      when (val s = ghost.stimuli[idxGhost]) {
        is Stimulus.ButtonDown -> buttonDown(s.button)
        is Stimulus.ButtonUp -> buttonUp(s.button)
        is Stimulus.Snapshot -> takeSnapshot()
        is Stimulus.Close -> close()
      }
      idxGhost++
    }
  }
}

private const val SNAPSHOT_PERIOD = 1_000_000
