package choliver.nespot.playtest

import choliver.nespot.AudioSink
import choliver.nespot.VideoSink
import choliver.nespot.cartridge.Rom
import choliver.nespot.hash
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.playtest.Scenario.Stimulus

class RunnerCore(
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
  private var timestamp = 0L
  val stimuli = mutableListOf<Stimulus>()

  fun run(onStep: RunnerCore.(Long) -> Unit): Scenario {
    while (!closed) {
      nes.step()
      onStep(timestamp)
      timestamp++
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
