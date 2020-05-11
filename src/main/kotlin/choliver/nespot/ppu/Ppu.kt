package choliver.nespot.ppu

import choliver.nespot.*
import choliver.nespot.ppu.model.State
import java.nio.IntBuffer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class Ppu(
  private val memory: Memory,
  private val oam: Memory = Ram(256),
  private val palette: Memory = Palette(),
  private val renderer: Renderer = Renderer(memory, palette, oam),
  private val onVideoBufferReady: (IntBuffer) -> Unit
) {
  // TODO - should these just be IntArrays?  What's the difference?
  private val bufferA = IntBuffer.allocate(SCREEN_HEIGHT * SCREEN_WIDTH)
  private val bufferB = IntBuffer.allocate(SCREEN_HEIGHT * SCREEN_WIDTH)
  private var buffer = bufferA

  private var state = State()

  val vbl get() = state.inVbl && state.vblEnabled

  fun readReg(reg: Int) = when (reg) {
    REG_PPUSTATUS -> state.readStatus()
    REG_OAMDATA -> state.readOamData()
    REG_PPUDATA -> state.readData()
    else -> 0x00
  }

  fun writeReg(reg: Int, data: Data) {
    when (reg) {
      REG_PPUCTRL1 -> state.writeCtrl1(data)
      REG_PPUCTRL2 -> state.writeCtrl2(data)
      REG_OAMADDR -> state.writeOamAddr(data)
      REG_OAMDATA -> state.writeOamData(data)
      REG_PPUSCROLL -> state.writeScroll(data)
      REG_PPUADDR -> state.writeAddr(data)
      REG_PPUDATA -> state.writeData(data)
    }
  }

  // See http://wiki.nesdev.com/w/images/d/d1/Ntsc_timing.png
  // This is implemented as a state machine to minimise use of conditionals.
  // Each state is represented as a lambda, responsible for specifying the next state (nextAction) and
  // dot time (nextDot).
  // The lambdas have return type of Object to avoid Kotlin generating bridge methods
  fun advance(numCycles: Int) {
    repeat(numCycles * DOTS_PER_CYCLE) {
      if (state.dot++ == nextDot) {
        nextAction()
      }
    }
  }

  private val actionRender: () -> Object = {
    renderer.loadAndRenderBackground(state)
    renderer.renderSprites(state)
    renderer.commitToBuffer(state, buffer)

    nextDot = 256
    nextAction = actionEvaluate
    dummy
  }

  private val actionEvaluate: () -> Object = {
    renderer.evaluateSprites(state)

    nextDot = 257
    nextAction = actionUpdateCoordsDuringRender
    dummy
  }

  private val actionUpdateCoordsDuringRender: () -> Object = {
    state.updateCoordsForScanline()

    nextDot = 320
    nextAction = actionLoadSpritesDuringRender
    dummy
  }

  private val actionLoadSpritesDuringRender: () -> Object = {
    renderer.loadSprites(state)

    nextDot = 340
    nextAction = actionEndOfLine
    dummy
  }

  private val actionSetVbl: () -> Object = {
    state.inVbl = true
    onVideoBufferReady(buffer)
    buffer = if (buffer === bufferA) bufferB else bufferA

    nextDot = 340
    nextAction = actionEndOfLine
    dummy
  }

  private val actionClearFlags: () -> Object = {
    state.inVbl = false
    state.sprite0Hit = false
    state.spriteOverflow = false

    nextDot = 255
    nextAction = actionPreRender
    dummy
  }

  private val actionPreRender: () -> Object = {
    renderer.loadAndRenderBackground(state) // This happens even on this line

    nextDot = 257
    nextAction = actionUpdateCoordsDuringPreRender
    dummy
  }

  private val actionUpdateCoordsDuringPreRender: () -> Object = {
    state.updateCoordsForScanline()

    nextDot = 280
    nextAction = actionUpdateCoordsForNextFrame
    dummy
  }

  private val actionUpdateCoordsForNextFrame: () -> Object = {
    state.coords = state.coordsBacking.copy()

    nextDot = 320
    nextAction = actionLoadSpritesDuringPreRender
    dummy
  }

  private val actionLoadSpritesDuringPreRender: () -> Object = {
    renderer.loadSprites(state)  // This happens even though we haven't evaluated sprites

    nextDot = 340
    nextAction = actionEndOfLine
    dummy
  }

  private val actionEndOfLine: () -> Object = {
    state.dot = 0
    state.scanline = (state.scanline + 1) % SCANLINES_PER_FRAME

    when {
      (state.scanline < SCREEN_HEIGHT) -> {
        nextDot = 255
        nextAction = actionRender
      }
      (state.scanline == SCREEN_HEIGHT + 1) -> {
        nextDot = 1
        nextAction = actionSetVbl
      }
      (state.scanline == SCANLINES_PER_FRAME - 1) -> {
        nextDot = 1
        nextAction = actionClearFlags
      }
      (state.scanline == 0) -> {
        nextDot = 255
        nextAction = actionRender
      }
    }
    dummy
  }

  private val dummy = Object()
  // TODO - these need to be included in State (and serialised) somehow
  private var nextDot = 255
  private var nextAction: () -> Object = actionRender

  private fun State.updateCoordsForScanline() {
    if (bgEnabled || sprEnabled) {
      coords.xFine = coordsBacking.xFine
      coords.xCoarse = coordsBacking.xCoarse
      coords.nametable = (coords.nametable and 0b10) or (coordsBacking.nametable and 0b01)
      coords.incrementY()
    }
  }

  private fun State.readStatus(): Int {
    val ret = 0 +
      (if (inVbl) 0x80 else 0x00) +
      (if (sprite0Hit) 0x40 else 0x00) +
      (if (spriteOverflow) 0x20 else 0x00)

    // Reset stuff
    inVbl = false
    w = false
    return ret
  }

  private fun State.writeScroll(data: Data) {
    val fine = (data and 0b00000111)
    val coarse = (data and 0b11111000) shr 3

    with(coordsBacking) {
      if (!w) {
        xCoarse = coarse
        xFine = fine
      } else {
        yCoarse = coarse
        yFine = fine
      }
    }
    w = !w
  }

  private fun State.writeAddr(data: Data) {
    // Address and scroll settings share registers, so also update coords accordingly.
    // Super Mario Bros split scroll breaks if we don't do this.
    // See http://wiki.nesdev.com/w/index.php/PPU_scrolling#Summary
    if (!w) {
      addr = addr(lo = addr.lo(), hi = data and 0b00111111)
      with(coordsBacking) {
        yCoarse = ((data and 0b00000011) shl 3) or (yCoarse and 0b00111)
        nametable = (data and 0b00001100) shr 2
        yFine = (data and 0b00110000) shr 4  // Lose the top bit
      }
    } else {
      addr = addr(lo = data, hi = addr.hi())
      with(coordsBacking) {
        xCoarse = (data and 0b00011111)
        yCoarse = ((data and 0b11100000) shr 5) or (yCoarse and 0b11000)
      }
      coords = coordsBacking.copy()   // Propagate immediately
    }
    w = !w
  }

  private fun State.readData(): Data {
    val ret = when {
      (addr < BASE_PALETTE) -> readBuffer.also { readBuffer = memory[addr] }
      else -> palette[addr and 0x001F]
    }
    addr = (addr + addrInc) and 0x7FFF
    return ret
  }

  private fun State.writeData(data: Data) {
    when {
      addr < BASE_PALETTE -> memory[addr] = data
      else -> palette[addr and 0x1F] = data
    }
    addr = (addr + addrInc) and 0x7FFF
  }

  private fun State.writeOamAddr(data: Data) {
    oamAddr = data
  }

  private fun State.readOamData(): Data {
    val ret = oam[oamAddr]
    oamAddr = (oamAddr + 1).addr8()
    return ret
  }

  private fun State.writeOamData(data: Data) {
    oam[oamAddr] = data
    oamAddr = (oamAddr + 1).addr8()
  }

  private fun State.writeCtrl1(data: Data) {
    addrInc = if (data.isBitSet(2)) 32 else 1
    sprPatternTable = if (data.isBitSet(3)) 1 else 0
    bgPatternTable = if (data.isBitSet(4)) 1 else 0
    largeSprites = data.isBitSet(5)
    // TODO - is master/slave important?
    vblEnabled = data.isBitSet(7)
    coordsBacking.nametable = data and 0b11
  }

  private fun State.writeCtrl2(data: Data) {
    greyscale = data.isBitSet(0)
    bgLeftTileEnabled = data.isBitSet(1)
    sprLeftTileEnabled = data.isBitSet(2)
    bgEnabled = data.isBitSet(3)
    sprEnabled = data.isBitSet(4)
    redEmphasized = data.isBitSet(5)
    greenEmphasized = data.isBitSet(6)
    blueEmphasized = data.isBitSet(7)
  }

  inner class Diagnostics internal constructor() {
    var state
      get() = this@Ppu.state
      set(value) { this@Ppu.state = value }
    val renderer = this@Ppu.renderer.diagnostics
    val oam = this@Ppu.oam
    val palette = this@Ppu.palette
  }

  val diagnostics = Diagnostics()

  @Suppress("unused")
  companion object {
    // http://wiki.nesdev.com/w/index.php/PPU_registers
    const val REG_PPUCTRL1 = 0
    const val REG_PPUCTRL2 = 1
    const val REG_PPUSTATUS = 2
    const val REG_OAMADDR = 3
    const val REG_OAMDATA = 4
    const val REG_PPUSCROLL = 5
    const val REG_PPUADDR = 6
    const val REG_PPUDATA = 7

    const val BASE_PATTERN_TABLES: Address = 0x0000
    const val BASE_NAMETABLES: Address = 0x2000
    const val BASE_PATTERNS: Address = 0x0000
    const val BASE_PALETTE: Address = 0x3F00

    const val NAMETABLE_SIZE_BYTES = 0x0400
    const val PATTERN_TABLE_SIZE_BYTES = 0x1000
  }
}
