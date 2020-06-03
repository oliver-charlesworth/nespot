package choliver.nespot.playtest

import choliver.nespot.AudioSink
import choliver.nespot.VideoSink
import choliver.nespot.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.cartridge.Rom
import choliver.nespot.playtest.Scenario.Stimulus
import choliver.nespot.runner.AudioPlayer
import choliver.nespot.runner.Event
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction
import choliver.nespot.runner.KeyAction.Joypad
import choliver.nespot.runner.Screen
import java.util.concurrent.LinkedBlockingQueue

/**
 * Capture scenario based on live user input.
 */
fun liveCapture(rom: Rom): Scenario {
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
      if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0)) {
        takeSnapshot()
      }
      when (val e = events.poll()) {
        is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
          is Joypad -> buttonDown(action.button)
        }
        is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
          is Joypad -> buttonUp(action.button)
        }
        is Close -> close()
        is Error -> close()
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
fun ghostCapture(rom: Rom, ghost: Scenario): Scenario {
  val captor = ScenarioCaptor(
    rom = rom,
    videoSink = object : VideoSink { override val colorPackingMode = BGRA },
    audioSink = object : AudioSink {}
  )

  var idxGhost = 0
  return captor.capture { timestamp ->
    if (ghost.stimuli[idxGhost].timestamp == timestamp) {
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
