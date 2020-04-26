package choliver.nespot.ppu

import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class Coords(
  var nametableX: Int = 0,    // 0 or 1
  var coarseX: Int = 0,       // 0 to 31 inc.
  var fineX: Int = 0,         // 0 to 7 inc.
  var nametableY: Int = 0,    // 0 or 1
  var coarseY: Int = 0,       // 0 to 31 inc.
  var fineY: Int = 0          // 0 to 7 inc.
) {
  fun incrementX(): Coords {
    when (fineX) {
      (TILE_SIZE - 1) -> {
        fineX = 0
        when (coarseX) {
          (NUM_TILE_COLUMNS - 1) -> {
            coarseX = 0
            nametableX = 1 - nametableX   // Wraparound
          }
          else -> coarseX++
        }
      }
      else -> fineX++
    }
    return this
  }

  fun incrementY(): Coords {
    when (fineY) {
      (TILE_SIZE - 1) -> {
        fineY = 0
        when (coarseY) {
          (NUM_TILE_ROWS - 1) -> {
            coarseY = 0
            nametableY = 1 - nametableY   // Wraparound
          }
          (NUM_TILE_COLUMNS - 1) -> coarseY = 0   // Weird special case
          else -> coarseY++
        }
      }
      else -> fineY++
    }
    return this
  }
}
