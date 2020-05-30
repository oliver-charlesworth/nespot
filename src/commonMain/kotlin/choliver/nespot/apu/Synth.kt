package choliver.nespot.apu

interface Synth {
  var enabled: Boolean
  val output: Int
  val outputRemaining: Boolean

  fun onTimer(num: Int) {}
  fun onQuarterFrame() {}
  fun onHalfFrame() {}
}
