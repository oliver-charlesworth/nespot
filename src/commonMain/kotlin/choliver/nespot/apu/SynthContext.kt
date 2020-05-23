package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.Rational
import choliver.nespot.apu.FrameSequencer.Ticks

class SynthContext<S : Synth>(
  val synth: S,
  val timer: Timer,
  val envelope: Envelope = Envelope(),
  val sweep: Sweep = Sweep(timer),
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00)
) {
  constructor(
    cyclesPerSample: Rational,
    synth: S
  ) : this(
    synth = synth,
    timer = Timer(cyclesPerSample)
  )

  fun take(ticks: Ticks): Int {
    if (ticks.quarter) {
      envelope.advance()
      synth.onQuarterFrame()
    }
    if (ticks.half) {
      sweep.advance()
      synth.onHalfFrame()
    }
    synth.onTimer(timer.take())
    return if (sweep.mute) 0 else (synth.output * envelope.level)
  }
}
