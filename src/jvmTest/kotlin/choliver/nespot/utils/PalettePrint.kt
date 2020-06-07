package choliver.nespot.utils

import java.io.File

fun main() {
  val bytes = File("palette.pal").readBytes()

  val str = bytes.toList()
    .map { it.toInt() }
    .map { if (it >= 0) it else (it + 256) }
    .chunked(3)
    .chunked(64)
    .joinToString(",\n") { chunk ->
      chunk.joinToString(", ") { "Rgb(${it[0]}, ${it[1]}, ${it[2]})" }
    }

  println(str)
}
