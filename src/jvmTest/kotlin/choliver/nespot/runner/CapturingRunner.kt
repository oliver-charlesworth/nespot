package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.hash
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.Scenario.Stimulus
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(
  private val rom: Rom,
  private val ghost: Scenario? = null
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
  private var idxGhost = 0
  val stimuli = mutableListOf<Stimulus>()

  fun run(): Scenario {
    screen.show()
    audio.start()
    try {
      while (!closed) {
        nes.step()
        if (ghost != null) {
          inject(ghost)
        } else {
          consumeEvent()
          if (timestamp % SNAPSHOT_PERIOD == 0L && (timestamp > 0)) {
            takeSnapshot()
          }
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

  private fun inject(ghost: Scenario) {
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
