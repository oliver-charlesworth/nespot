package choliver.nespot.ppu

import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class Coords(
  var xNametable: Int = 0,    // 0 or 1
  var xCoarse: Int = 0,       // 0 to 31 inc.
  var xFine: Int = 0,         // 0 to 7 inc.
  var yNametable: Int = 0,    // 0 or 1
  var yCoarse: Int = 0,       // 0 to 31 inc.
  var yFine: Int = 0          // 0 to 7 inc.
) {
  fun incrementX(): Coords {
    when (xFine) {
      (TILE_SIZE - 1) -> {
        xFine = 0
        when (xCoarse) {
          (NUM_TILE_COLUMNS - 1) -> {
            xCoarse = 0
            xNametable = 1 - xNametable   // Wraparound
          }
          else -> xCoarse++
        }
      }
      else -> xFine++
    }
    return this
  }

  fun incrementY(): Coords {
    when (yFine) {
      (TILE_SIZE - 1) -> {
        yFine = 0
        when (yCoarse) {
          (NUM_TILE_ROWS - 1) -> {
            yCoarse = 0
            yNametable = 1 - yNametable   // Wraparound
          }
          (NUM_TILE_COLUMNS - 1) -> yCoarse = 0   // Weird special case
          else -> yCoarse++
        }
      }
      else -> yFine++
    }
    return this
  }
}
