package choliver.nespot.apu

interface Envelope {
  val level: Int
  fun advance()
}
