package choliver.nespot.ppu

// Palette generated using default settings here: http://drag.wootest.net/misc/palgen.html
val COLORS = listOf(
  70, 70, 70,
  0, 6, 90,
  0, 6, 120,
  2, 6, 115,
  53, 3, 76,
  87, 0, 14,
  90, 0, 0,
  65, 0, 0,
  18, 2, 0,
  0, 20, 0,
  0, 30, 0,
  0, 30, 0,
  0, 21, 33,
  0, 0, 0,
  0, 0, 0,
  0, 0, 0,
  157, 157, 157,
  0, 74, 185,
  5, 48, 225,
  87, 24, 218,
  159, 7, 167,
  204, 2, 85,
  207, 11, 0,
  164, 35, 0,
  92, 63, 0,
  11, 88, 0,
  0, 102, 0,
  0, 103, 19,
  0, 94, 110,
  0, 0, 0,
  0, 0, 0,
  0, 0, 0,
  254, 255, 255,
  31, 158, 255,
  83, 118, 255,
  152, 101, 255,
  252, 103, 255,
  255, 108, 179,
  255, 116, 102,
  255, 128, 20,
  196, 154, 0,
  113, 179, 0,
  40, 196, 33,
  0, 200, 116,
  0, 191, 208,
  43, 43, 43,
  0, 0, 0,
  0, 0, 0,
  254, 255, 255,
  158, 213, 255,
  175, 192, 255,
  208, 184, 255,
  254, 191, 255,
  255, 192, 224,
  255, 195, 189,
  255, 202, 156,
  231, 213, 139,
  197, 223, 142,
  166, 230, 163,
  148, 232, 197,
  146, 228, 235,
  167, 167, 167,
  0, 0, 0,
  0, 0, 0
).chunked(3).map { 255 + 256 * (it[0] + 256 * (it[1] + 256 * it[2])) }
