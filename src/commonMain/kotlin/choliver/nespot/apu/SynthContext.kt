package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.apu.FrameSequencer.Ticks

class SynthContext<SynthT : Synth, SweepT : Sweep, EnvelopeT : Envelope>(
  val synth: SynthT,
  val timer: Timer,
  val sweep: SweepT,
  val envelope: EnvelopeT,
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00)
) {
  fun advance(numCycles: Int, ticks: Ticks) {
    if (ticks.quarter) {
      envelope.advance()
      synth.onQuarterFrame()
    }
    if (ticks.half) {
      sweep.advance()
      synth.onHalfFrame()
    }
    synth.onTimer(timer.advance(numCycles))
  }

  val current get() = if (sweep.mute) 0 else (synth.output * envelope.level)
}
