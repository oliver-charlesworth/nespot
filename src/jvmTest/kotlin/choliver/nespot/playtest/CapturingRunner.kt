package choliver.nespot.playtest

import choliver.nespot.cartridge.Rom
import choliver.nespot.hash
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.playtest.Scenario.Stimulus
import choliver.nespot.runner.AudioPlayer
import choliver.nespot.runner.Event
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction
import choliver.nespot.runner.Screen
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(
  private val rom: Rom
) {
  private val events = LinkedBlockingQueue<Event>()
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()
  private val sink = CapturingVideoSink(screen.sink)
  private val nes = Nes(
    rom = rom,
    videoSink = sink,
    audioSink = audio.sink
  )
  private var closed = false
  private var timestamp = 0L
  val stimuli = mutableListOf<Stimulus>()

  fun run(): Scenario {
    screen.show()
    audio.start()
    try {
      while (!closed) {
        nes.step()
        consumeEvent()
        if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0)) {
          takeSnapshot()
        }
        timestamp++
      }
    } finally {
      screen.hide()
      screen.close()
      audio.close()
    }

    return Scenario(
      romHash = rom.hash,
      stimuli = stimuli
    )
  }

  private fun consumeEvent() {
    when (val e = this@CapturingRunner.events.poll()) {
      is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is KeyAction.Joypad -> buttonDown(action.button)
      }
      is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is KeyAction.Joypad -> buttonUp(action.button)
      }
      is Close -> close()
      is Error -> close()
    }
  }

  private fun buttonDown(button: Joypads.Button) {
    stimuli += Stimulus.ButtonDown(timestamp, button)
    nes.joypads.down(1, button)
  }

  private fun buttonUp(button: Joypads.Button) {
    stimuli += Stimulus.ButtonUp(timestamp, button)
    nes.joypads.up(1, button)
  }

  private fun takeSnapshot() {
    stimuli += Stimulus.Snapshot(timestamp, sink.snapshot)
  }

  private fun close() {
    stimuli += Stimulus.Close(timestamp)
    closed = true
  }

  companion object {
    private const val SNAPSHOT_PERIOD = 1_000_000
  }
}
