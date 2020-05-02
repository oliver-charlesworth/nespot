package choliver.nespot.ppu.model

import choliver.nespot.Address
import choliver.nespot.Address8
import choliver.nespot.Data
import choliver.nespot.MutableForPerfReasons
import choliver.nespot.ppu.Renderer

@MutableForPerfReasons
data class State(
  val rendererIn: Renderer.Input = Renderer.Input(
    bgEnabled = false,
    sprEnabled = false,
    bgLeftTileEnabled = false,
    sprLeftTileEnabled = false,
    largeSprites = false,
    bgPatternTable = 0,
    sprPatternTable = 0,
    coords = Coords(),
    scanline = 0
  ),
  var rendererOut: Renderer.Output = Renderer.Output(sprite0Hit = false, spriteOverflow = false),
  var addrInc: Int = 1,
  var isVblEnabled: Boolean = false,
  var isGreyscale: Boolean = false,
  var isRedEmphasized: Boolean = false,
  var isGreenEmphasized: Boolean = false,
  var isBlueEmphasized: Boolean = false,
  var coords: Coords = Coords(),
  var oamAddr: Address8 = 0x00,    // TODO - apparently this is reset to 0 during rendering
  var addr: Address = 0x0000,
  var readBuffer: Data = 0x00,
  var inVbl: Boolean = false,
  var w: Boolean = false
)
