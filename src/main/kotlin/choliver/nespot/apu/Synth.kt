package choliver.nespot.apu

interface Synth {
  var length: Int
  val output: Int
  val hasRemainingOutput: Boolean

  fun onTimer() {}
  fun onQuarterFrame() {}
  fun onHalfFrame() {}
}
