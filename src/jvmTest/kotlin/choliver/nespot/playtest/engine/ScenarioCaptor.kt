package choliver.nespot.playtest.engine

import choliver.nespot.cartridge.Rom
import choliver.nespot.hash
import choliver.nespot.nes.AudioSink
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.nes.VideoSink
import choliver.nespot.playtest.engine.Scenario.Stimulus

class ScenarioCaptor(
  private val rom: Rom,
  videoSink: VideoSink,
  audioSink: AudioSink
) {
  interface StepHandler {
    fun ScenarioCaptor.handleStep(timestamp: Long)
  }

  private val sink = CapturingVideoSink(videoSink)
  private val nes = Nes(
    rom = rom,
    videoSink = sink,
    audioSink = audioSink
  )
  private var closed = false
  private var timestamp = 0L
  private val stimuli = mutableListOf<Stimulus>()

  fun capture(handler: StepHandler): Scenario {
    with(handler) {
      while (!closed) {
        nes.step()
        handleStep(timestamp)
        timestamp++
      }
    }

    return Scenario(
      romHash = rom.hash,
      stimuli = stimuli
    )
  }

  fun buttonDown(button: Joypads.Button) {
    stimuli += Stimulus.ButtonDown(timestamp, button)
    nes.joypads.down(1, button)
  }

  fun buttonUp(button: Joypads.Button) {
    stimuli += Stimulus.ButtonUp(timestamp, button)
    nes.joypads.up(1, button)
  }

  fun takeSnapshot() {
    stimuli += Stimulus.Snapshot(timestamp, sink.snapshot)
  }

  fun close() {
    stimuli += Stimulus.Close(timestamp)
    closed = true
  }
}
