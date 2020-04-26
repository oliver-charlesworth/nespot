package choliver.nespot.ppu

import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class Coords(
  var nametableX: Int = 0,    // 0 or 1 in practice
  var coarseX: Int = 0,
  var fineX: Int = 0,
  var nametableY: Int = 0,    // 0 or 1 in practice
  var coarseY: Int = 0,
  var fineY: Int = 0
) {
  fun incrementX() {
    when {
      (fineX < (TILE_SIZE - 1)) -> fineX++
      else -> {
        fineX = 0
        when (coarseX) {
          (NUM_TILE_COLUMNS - 1) -> {
            coarseX = 0
            nametableX = 1 - nametableX   // Wraparound
          }
          else -> coarseX++
        }
      }
    }
  }

  // Takes account of reduced nametable height
  fun incrementY() {
    when {
      (fineY < (TILE_SIZE - 1)) -> fineY++
      else -> {
        fineY = 0
        when (coarseY) {
          (NUM_TILE_ROWS - 1) -> {
            coarseY = 0
            nametableY = 1 - nametableY   // Wraparound
          }
          (NUM_TILE_COLUMNS - 1) -> coarseY = 0   // TODO - need test-case for this weirdness
          else -> coarseY++
        }
      }
    }
  }
}
