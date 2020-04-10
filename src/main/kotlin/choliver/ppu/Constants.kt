package choliver.ppu

const val CANVAS_WIDTH = 256
const val CANVAS_HEIGHT = 240


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


// ----------- CPU address space ----------- //

// http://wiki.nesdev.com/w/index.php/PPU_registers

const val ADDR_PPUCTRL = 0x2000
// 7 - (V) Enable NMI at start of VBI (0: off; 1: on)
// 6 - (P) PPU master/slave select (0: read backdrop from EXT pins; 1: output color on EXT pins)
// 5 - (H) Sprite size 0: (8x8 pixels; 1: 8x16 pixels)
// 4 - (B) Background pattern table address (0: $0000; 1: $1000)
// 3 - (S) Sprite pattern table address for 8x8 sprites (0: $0000; 1: $1000; ignored in 8x16 mode)
// 2 - (I) VRAM address increment per CPU read/write of PPUDATA (0: add 1, going across; 1: add 32, going down)
// 1:0 - (NN) - Base nametable address (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)

const val ADDR_PPUMASK = 0x2001
// 7 - (B) Emphasize blue
// 6 - (G) Emphasize green
// 5 - (R) Emphasize red
// 4 - (s) 1: Show sprites
// 3 - (b) 1: Show background
// 2 - (M) 1: Show sprites in leftmost 8 pixels of screen, 0: Hide
// 1 - (m) 1: Show background in leftmost 8 pixels of screen, 0: Hide
// 0 - (G) Greyscale (0: normal color, 1: produce a greyscale display)


const val ADDR_PPUSTATUS = 0x2002
const val ADDR_OAMADDR = 0x2003
const val ADDR_OAMDATA = 0x2004
const val ADDR_PPUSCROLL = 0x2005
const val ADDR_PPUADDR = 0x2006
const val ADDR_PPUDATA = 0x2007

// 0x2000 -> 0x2007 mirrored to 0x2008 -> 0x3FFF


const val ADDR_OAMDMA = 0x4014

