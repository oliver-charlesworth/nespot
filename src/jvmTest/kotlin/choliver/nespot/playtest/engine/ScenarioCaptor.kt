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
  private val sink = CapturingVideoSink(videoSink)
  private val nes = Nes(
    rom = rom,
    videoSink = sink,
    audioSink = audioSink
  )
  private var closed = false
  private var _timestamp = 0L
  val timestamp get() = _timestamp
  private val stimuli = mutableListOf<Stimulus>()

  fun capture(onStep: ScenarioCaptor.() -> Unit): Scenario {
    while (!closed) {
      nes.step()
      onStep()
      _timestamp++
    }

    return Scenario(
      romHash = rom.hash,
      stimuli = stimuli
    )
  }

  fun buttonDown(button: Joypads.Button) {
    stimuli += Stimulus.ButtonDown(_timestamp, button)
    nes.joypads.down(1, button)
  }

  fun buttonUp(button: Joypads.Button) {
    stimuli += Stimulus.ButtonUp(_timestamp, button)
    nes.joypads.up(1, button)
  }

  fun takeSnapshot() {
    stimuli += Stimulus.Snapshot(_timestamp, sink.snapshot)
  }

  fun close() {
    stimuli += Stimulus.Close(_timestamp)
    closed = true
  }
}
