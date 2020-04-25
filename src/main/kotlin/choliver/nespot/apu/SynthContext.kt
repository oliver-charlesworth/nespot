package choliver.nespot.apu

import choliver.nespot.Data

internal data class SynthContext<S : Synth>(
  val synth: S,
  val level: Double,
  val timer: Counter = Counter(),
  val envelope: Envelope = Envelope(),
  val sweep: Sweep = Sweep(timer),
  val regs: MutableList<Data> = mutableListOf(0x00, 0x00, 0x00, 0x00),
  var enabled: Boolean = false,
  val name: String = ""
)
