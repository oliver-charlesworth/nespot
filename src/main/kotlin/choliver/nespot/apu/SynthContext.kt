package choliver.nespot.apu

import choliver.nespot.Data

internal class SynthContext<S : Synth>(
  val synth: S,
  val level: Double,
  val timer: Counter = Counter(),
  val envelope: Envelope = Envelope(),
  val sweep: Sweep = Sweep(timer),
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00),
  var enabled: Boolean = false
) {
  fun take(ticks: Sequencer.Ticks): Double {
    if (ticks.quarter) {
      envelope.advance()
      synth.onQuarterFrame()
    }
    if (ticks.half) {
      sweep.advance()
      synth.onHalfFrame()
    }
    repeat(timer.take()) {
      synth.onTimer()
    }
    return synth.output * envelope.level * (if (enabled) level else 0.0)
  }
}
