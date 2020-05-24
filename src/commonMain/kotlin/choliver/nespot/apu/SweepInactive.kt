package choliver.nespot.apu

class SweepInactive : Sweep {
  override val mute = false
  override fun advance() {}
}
