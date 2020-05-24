package choliver.nespot.apu

class EnvelopeInactive(override val level: Int) : Envelope {
  override fun advance() {}
}
