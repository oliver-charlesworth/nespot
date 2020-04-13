package choliver.nes.ppu

const val CANVAS_WIDTH = 256
const val CANVAS_HEIGHT = 240
const val TILE_SIZE = 8


// ----------- PPU address space ----------- //

// http://wiki.nesdev.com/w/index.php/PPU_memory_map

// http://wiki.nesdev.com/w/index.php/PPU_pattern_tables
const val BASE_PATTERN_TABLE_0    = 0x0000  // to 0x0FFF inc.
const val BASE_PATTERN_TABLE_1    = 0x1000  // to 0x1FFF inc.

// http://wiki.nesdev.com/w/index.php/PPU_nametables
const val BASE_NAME_TABLE_0       = 0x2000  // to 0x23FF inc.
const val BASE_NAME_TABLE_1       = 0x2400  // to 0x27FF inc.
const val BASE_NAME_TABLE_2       = 0x2800  // to 0x2BFF inc.
const val BASE_NAME_TABLE_3       = 0x2C00  // to 0x2FFF inc.

// http://wiki.nesdev.com/w/index.php/PPU_palettes
const val BASE_PALETTE_BACKGROUND = 0x3F00  // to 0x3F0F inc.
const val BASE_PALETTE_SPRITES    = 0x3F10  // to 0x3F1F inc.

// 0x2000 -> 0x2EFF mirrored to 0x3000 to 0x3EFF
// 0x3F00 -> 0x3F1F mirrored to 0x3F20 to 0x3FFF


