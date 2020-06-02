package choliver.nespot.playtest

import choliver.nespot.cartridge.Rom
import choliver.nespot.runner.AudioPlayer
import choliver.nespot.runner.Event
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction
import choliver.nespot.runner.KeyAction.Joypad
import choliver.nespot.runner.Screen
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(
  private val rom: Rom
) {
  fun run(): Scenario {
    val events = LinkedBlockingQueue<Event>()
    val screen = Screen(onEvent = { events += it })
    val audio = AudioPlayer()

    val core = RunnerCore(
      rom = rom,
      videoSink = screen.sink,
      audioSink = audio.sink
    )

    screen.show()
    audio.start()

    try {
      return core.run { timestamp ->
        if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0)) {
          takeSnapshot()
        }
        consumeEvent(events)
      }
    } finally {
      screen.hide()
      screen.close()
      audio.close()
    }
  }

  private fun RunnerCore.consumeEvent(events: Queue<Event>) {
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

  companion object {
    private const val SNAPSHOT_PERIOD = 1_000_000
  }
}
