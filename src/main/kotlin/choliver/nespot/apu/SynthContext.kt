package choliver.nespot.apu

import choliver.nespot.Data
import choliver.nespot.apu.Sequencer.Ticks

class SynthContext<S : Synth>(
  val synth: S,
  val timer: Counter = Counter(),
  val envelope: Envelope = Envelope(),
  val sweep: Sweep = Sweep(timer),
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00),
  var enabled: Boolean = false
) {
  fun take(ticks: Ticks): Int {
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
    return if (enabled) (synth.output * envelope.level) else 0
  }
}
