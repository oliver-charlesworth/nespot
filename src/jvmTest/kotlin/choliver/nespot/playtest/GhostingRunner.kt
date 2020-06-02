package choliver.nespot.playtest

import choliver.nespot.VideoSink
import choliver.nespot.VideoSink.ColorPackingMode
import choliver.nespot.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.cartridge.Rom
import choliver.nespot.hash
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.playtest.Scenario.Stimulus

class GhostingRunner(
  private val rom: Rom,
  private val ghost: Scenario
) {
  private val sink = CapturingVideoSink(object : VideoSink {
    override val colorPackingMode = BGRA
  })
  private val nes = Nes(
    rom = rom,
    videoSink = sink
  )
  private var closed = false
  private var timestamp = 0L
  private var idxGhost = 0
  val stimuli = mutableListOf<Stimulus>()

  fun run(): Scenario {
    while (!closed) {
      nes.step()
      inject(ghost)
      timestamp++
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
}
