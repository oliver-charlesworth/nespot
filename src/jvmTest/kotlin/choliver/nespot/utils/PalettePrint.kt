package choliver.nespot.utils

import java.io.File

@ExperimentalUnsignedTypes
fun main() {
  val bytes = File("palette.pal").readBytes()

  val str = bytes
    .map { it.toUByte() }
    .chunked(3)
    .chunked(64)
    .joinToString(",\n") { chunk ->
      chunk.joinToString(", ") { "Rgb(${it[0]}, ${it[1]}, ${it[2]})" }
    }

  println(str)
}
