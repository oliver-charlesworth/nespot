package choliver.ppu

// Palette generated using default settings here: http://drag.wootest.net/misc/palgen.html
val COLORS = object {}.javaClass.getResource("/nespalette.pal")
  .readBytes()
  .map { it.toUByte().toInt() }
  .toList()
  .chunked(3)
  .map { 255 + 256 * (it[0] + 256 * (it[1] + 256 * it[2])) }
