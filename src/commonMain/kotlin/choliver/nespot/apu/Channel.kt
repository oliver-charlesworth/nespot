package choliver.nespot.apu

import choliver.nespot.common.Data

class Channel<SynthT : Synth, SweepT : Sweep, EnvelopeT : Envelope>(
  val synth: SynthT,
  val timer: Timer,
  val sweep: SweepT,
  val envelope: EnvelopeT,
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00)
) {
  fun onQuarterFrame() {
    envelope.advance()
    synth.onQuarterFrame()
  }

  fun onHalfFrame() {
    sweep.advance()
    synth.onHalfFrame()
  }

  fun advance(numCycles: Int) {
    synth.onTimer(timer.advance(numCycles))
  }

  val output get() = if (sweep.mute) 0 else (synth.output * envelope.level)
}
