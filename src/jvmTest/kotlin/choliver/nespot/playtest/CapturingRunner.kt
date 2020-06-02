package choliver.nespot.playtest

import choliver.nespot.cartridge.Rom
import choliver.nespot.runner.AudioPlayer
import choliver.nespot.runner.Event
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction
import choliver.nespot.runner.KeyAction.Joypad
import choliver.nespot.runner.Screen
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(
  rom: Rom
) {
  private val events = LinkedBlockingQueue<Event>()
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()

  private val core = RunnerCore(
    rom = rom,
    videoSink = screen.sink,
    audioSink = audio.sink
  )

  fun run(): Scenario {
    screen.show()
    audio.start()

    try {
      return core.run { timestamp ->
        if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0)) {
          takeSnapshot()
        }
        consumeEvent()
      }
    } finally {
      screen.hide()
      screen.close()
      audio.close()
    }
  }

  private fun RunnerCore.consumeEvent() {
    when (val e = this@CapturingRunner.events.poll()) {
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
