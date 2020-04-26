package choliver.nespot.cartridge

@Suppress("ArrayInDataClass")
data class MapperConfig(
  val hasPersistentMem: Boolean = false,
  val mirroring: Mirroring = Mirroring.IGNORED,
  val trainerData: ByteArray = byteArrayOf(),
  val prgData: ByteArray = byteArrayOf(),
  val chrData: ByteArray = byteArrayOf()
) {
  enum class Mirroring {
    HORIZONTAL,
    VERTICAL,
    IGNORED   // TODO - not sure if this is mutually exclusive
  }
}
