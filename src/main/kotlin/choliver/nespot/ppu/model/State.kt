package choliver.nespot.ppu.model

import choliver.nespot.Address
import choliver.nespot.Address8
import choliver.nespot.Data
import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class State(
  var bgEnabled: Boolean = false,
  var sprEnabled: Boolean = false,
  var bgLeftTileEnabled: Boolean = false,
  var sprLeftTileEnabled: Boolean = false,
  var largeSprites: Boolean = false,
  var bgPatternTable: Int = 0,  // 0 or 1
  var sprPatternTable: Int = 0, // 0 or 1
  var vblEnabled: Boolean = false,
  var greyscale: Boolean = false,
  var redEmphasized: Boolean = false,
  var greenEmphasized: Boolean = false,
  var blueEmphasized: Boolean = false,
  var coordsBacking: Coords = Coords(),
  var coords: Coords = Coords(),
  var scanline: Int = 0,
  var addrInc: Int = 1,
  var oamAddr: Address8 = 0x00,    // TODO - apparently this is reset to 0 during rendering
  var addr: Address = 0x0000,
  var readBuffer: Data = 0x00,
  var inVbl: Boolean = false,
  var w: Boolean = false,
  var sprite0Hit: Boolean = false,
  var spriteOverflow: Boolean = false
)
