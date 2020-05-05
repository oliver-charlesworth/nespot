package choliver.nespot.apu

interface Synth {
  var enabled: Boolean
  val output: Int
  val hasRemainingOutput: Boolean

  fun onTimer() {}
  fun onQuarterFrame() {}
  fun onHalfFrame() {}
}
