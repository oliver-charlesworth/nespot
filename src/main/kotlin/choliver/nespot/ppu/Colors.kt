package choliver.nespot.ppu

import choliver.nespot.data

// Palette generated using default settings here: http://drag.wootest.net/misc/palgen.html
val COLORS = object {}.javaClass.getResource("/nespalette.pal")
  .readBytes()
  .map { it.data() }
  .toList()
  .chunked(3)
  .map { it[2] + 256 * (it[1] + 256 * (it[0] + 256 * 255)) }
