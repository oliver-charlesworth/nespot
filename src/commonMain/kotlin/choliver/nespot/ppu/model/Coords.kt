package choliver.nespot.ppu.model

import choliver.nespot.MutableForPerfReasons
import choliver.nespot.ppu.NUM_TILE_COLUMNS
import choliver.nespot.ppu.NUM_TILE_ROWS
import choliver.nespot.ppu.TILE_SIZE

@MutableForPerfReasons
data class Coords(
  var nametable: Int = 0,     // 0 to 3 inc.
  var xCoarse: Int = 0,       // 0 to 31 inc.
  var xFine: Int = 0,         // 0 to 7 inc.
  var yCoarse: Int = 0,       // 0 to 31 inc.
  var yFine: Int = 0          // 0 to 7 inc.
) {
  // TODO - move behaviour out of model type

  fun incrementX(): Coords {
    when (xFine) {
      (TILE_SIZE - 1) -> {
        xFine = 0
        incrementXByTile()
      }
      else -> xFine++
    }
    return this
  }

  fun incrementXByTile(): Coords {
    when (xCoarse) {
      (NUM_TILE_COLUMNS - 1) -> {
        xCoarse = 0
        nametable = nametable xor 1   // Wraparound
      }
      else -> xCoarse++
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
            nametable = nametable xor 2   // Wraparound
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
