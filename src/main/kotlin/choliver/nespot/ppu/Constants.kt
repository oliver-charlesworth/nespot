package choliver.nespot.ppu

const val SCREEN_WIDTH = 256
const val SCREEN_HEIGHT = 240

const val TILE_SIZE = 8
const val NUM_TILE_COLUMNS = SCREEN_WIDTH / TILE_SIZE
const val NUM_TILE_ROWS = SCREEN_HEIGHT / TILE_SIZE

const val METATILE_SIZE = 16
const val NUM_METATILE_COLUMNS = SCREEN_WIDTH / METATILE_SIZE
const val NUM_METATILE_ROWS = SCREEN_HEIGHT / METATILE_SIZE

const val NUM_PALETTES = 4
const val NUM_ENTRIES_PER_PALETTE = 4

const val NUM_SPRITES = 64
const val SPRITE_SIZE_BYTES = 4

const val NUM_PATTERNS = 256
const val PATTERN_SIZE_BYTES = TILE_SIZE * 2  // 2 bit/pixel


