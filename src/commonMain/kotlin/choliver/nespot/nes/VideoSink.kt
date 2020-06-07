package choliver.nespot.nes

interface VideoSink {
  enum class ColorPackingMode(
    val convert: (Rgb) -> List<Int> // Output is big-endian
  ) {
    BGRA({ listOf(it.b, it.g, it.r, 255) }),
    ABGR({ listOf(255, it.b, it.g, it.r) })
  }

  data class Rgb(
    val r: Int,
    val g: Int,
    val b: Int
  )

  val colorPackingMode: ColorPackingMode get() = ColorPackingMode.ABGR  // Don't care
  fun put(color: Int) {}
}

