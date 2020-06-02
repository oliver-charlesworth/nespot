package choliver.nespot.playtest

import choliver.nespot.AudioSink
import choliver.nespot.VideoSink
import choliver.nespot.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.cartridge.Rom
import choliver.nespot.playtest.Scenario.Stimulus

class GhostingRunner(
  rom: Rom,
  private val ghost: Scenario
) {
  private var idxGhost = 0

  private val core = RunnerCore(
    rom = rom,
    videoSink = object : VideoSink {
      override val colorPackingMode = BGRA
    },
    audioSink = object : AudioSink {}
  )

  fun run() = core.run { timestamp ->
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
