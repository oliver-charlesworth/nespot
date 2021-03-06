package choliver.nespot

import choliver.nespot.common.Rational

// See https://wiki.nesdev.com/w/index.php/Cycle_reference_chart
val MASTER_FREQ_HZ = Rational.of(945e6.toInt(), 4) / 11
val CPU_FREQ_HZ = MASTER_FREQ_HZ / 12

const val DOTS_PER_SCANLINE = 341
const val SCANLINES_PER_FRAME = 262
const val SCREEN_WIDTH = 256
const val SCREEN_HEIGHT = 240
const val TILE_SIZE = 8
const val DOTS_PER_CYCLE = 3
const val VISIBLE_WIDTH = SCREEN_WIDTH
const val VISIBLE_HEIGHT = (SCREEN_HEIGHT - 2 * TILE_SIZE)  // Crop top and bottom
const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good

const val NAMETABLE_SIZE = 1024
const val RAM_SIZE = 2048
const val VRAM_SIZE = 2048
const val CHR_SIZE = 8192
const val PRG_RAM_SIZE = 8192
const val PRG_ROM_SIZE = 32768

const val BASE_PRG_RAM = 0x6000
const val BASE_PRG_ROM = 0x8000
const val BASE_CHR_ROM = 0x0000
const val BASE_VRAM = 0x2000
