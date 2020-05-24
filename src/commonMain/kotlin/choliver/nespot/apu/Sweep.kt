package choliver.nespot.apu

interface Sweep {
  val mute: Boolean
  fun advance()
}
