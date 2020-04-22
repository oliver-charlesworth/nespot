package choliver.nespot.cartridge

@Suppress("ArrayInDataClass")
data class MapperConfig(
  val hasPersistentMem: Boolean,
  val mirroring: Mirroring,
  val trainerData: ByteArray,
  val prgData: ByteArray,
  val chrData: ByteArray
) {
  enum class Mirroring {
    HORIZONTAL,
    VERTICAL,
    IGNORED   // TODO - not sure if this is mutually exclusive
  }
}
