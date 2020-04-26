package choliver.nespot.apu

interface Synth {
  val output: Int
  var length: Int

  fun onTimer() {}
  fun onQuarterFrame() {}
  fun onHalfFrame() {}
}
