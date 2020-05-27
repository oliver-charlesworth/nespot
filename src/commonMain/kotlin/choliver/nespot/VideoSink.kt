package choliver.nespot

interface VideoSink {
  enum class ColorPackingMode(
    val convert: (List<Int>) -> List<Int> // Output is big-endian
  ) {
    BGRA({ listOf(it[2], it[1], it[0], 255) }),
    ABGR({ listOf(255, it[2], it[1], it[0]) })
  }

  val colorPackingMode: ColorPackingMode get() = ColorPackingMode.ABGR  // Don't care
  fun put(color: Int) {}
}

