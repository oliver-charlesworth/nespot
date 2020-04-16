package choliver.nes.ppu

import choliver.nes.Memory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.IntBuffer
import kotlin.random.Random

class RendererTest {
  // TODO - correct base addresses
  // TODO - nametable calculation
  // TODO - scrolling / offset
  // TODO - universal background colour
  // TODO - conditional rendering
  // TODO - all metatile value offsets (0, 2, 4, 6)

  private val colors = (0..63).toList()
  private val paletteEntries = (0..15).map { it + 5 }

  private val memory = mock<Memory>()
  private val palette = mock<Memory> {
    paletteEntries.forEachIndexed { idx, data ->
      on { load(idx) } doReturn data
    }
  }
  private val oam = mock<Memory>()
  private val renderer = Renderer(
    memory = memory,
    palette = palette,
    oam = oam,
    colors = colors
  )

  private val buffer = IntBuffer.allocate(SCREEN_WIDTH)
  private val random = Random(12345)

  private val nametableAddr = 0x2000
  private val bgPatternTableAddr = 0x1000
  private val sprPatternTableAddr = 0x0000

  // Chosen scanline
  private val yTile = 14
  private val yPixel = 4

  @Test
  fun `renders background`() {
    // One scanline for each pattern (note - no zeros in this test case)
    val patternEntries = List(NUM_PATTERNS) { List(TILE_SIZE) { random.nextInt(1, 4) } }

    // One tile-row's worth of nametable and attribute entries
    val nametableEntries = List(NUM_TILE_COLUMNS) { random.nextInt(0, NUM_PATTERNS) }
    val attrEntries = List(NUM_METATILE_COLUMNS) { random.nextInt(0, 4) }  // TODO - 2 and 4 are magic numbers

    initNametableMemory(nametableEntries)
    initAttributeMemory(attrEntries)
    initPatternMemory(patternEntries)

    render()

    val expected = (0 until SCREEN_WIDTH).map { x ->
      val xMetaTile = x / METATILE_SIZE
      val xTile = x / TILE_SIZE
      val xPixel = x % TILE_SIZE

      val yeah = patternEntries[nametableEntries[xTile]][xPixel]
      colors[paletteEntries[yeah + attrEntries[xMetaTile] * 4]]
    }

    assertEquals(expected, buffer.array().toList())
  }

  @Test
  fun `renders background patterns for palette #0`() {
    val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)

    initPatternEntryMemory(0, pattern)

    render()

    val expected = (0 until SCREEN_WIDTH).map { x ->
      colors[paletteEntries[pattern[x % TILE_SIZE]]]
    }

    assertEquals(expected, buffer.array().toList())
  }

  @Test
  fun `renders background patterns for higher palettes using universal background color`() {
    val pattern = listOf(0, 1, 2, 3, 2, 3, 0, 1)
    val attrEntries = List(NUM_METATILE_COLUMNS) { 1 }  // Arbitrary non-zero palette #

    initAttributeMemory(attrEntries)
    initPatternEntryMemory(0, pattern)

    render()

    val expected = (0 until SCREEN_WIDTH).map { x ->
      colors[paletteEntries[pattern[x % TILE_SIZE].let { if (it == 0) 0 else (it + 4) }]]
    }

    assertEquals(expected, buffer.array().toList())
  }


  private fun render() {
    renderer.renderScanlineTo(
      buffer,
      ctx = Renderer.Context(
        nametableAddr = nametableAddr,
        bgPatternTableAddr = bgPatternTableAddr,
        sprPatternTableAddr = sprPatternTableAddr
      ),
      y = (yTile * TILE_SIZE) + yPixel
      )
  }

  private fun initNametableMemory(nametableEntries: List<Int>) {
    nametableEntries.forEachIndexed { idx, data ->
      whenever(memory.load(nametableAddr + (yTile * NUM_TILE_COLUMNS) + idx)) doReturn data
    }
  }

  private fun initAttributeMemory(attrEntries: List<Int>) {
    attrEntries.chunked(2).forEachIndexed { idx, data ->
      val attr = if ((yTile % 4) / 2 == 0) {
        (data[1] shl 2) or (data[0] shl 0)
      } else {
        (data[1] shl 6) or (data[0] shl 4)
      }
      whenever(memory.load(
        nametableAddr + (NUM_TILE_COLUMNS * NUM_TILE_ROWS) + ((yTile / 4) * (NUM_METATILE_COLUMNS / 2)) + idx)
      ) doReturn attr
      }
  }

  private fun initPatternMemory(patternEntries: List<List<Int>>) {
    patternEntries.forEachIndexed(this::initPatternEntryMemory)
  }

  private fun initPatternEntryMemory(idx: Int, entry: List<Int>) {
    // Calculate bit-planes
    var lo = 0
    var hi = 0
    entry.forEach {
      lo = (lo shl 1) or (it and 1)
      hi = (hi shl 1) or ((it / 2) and 1)
    }

    // Separate bit planes
    whenever(memory.load(bgPatternTableAddr + (idx * PATTERN_SIZE_BYTES) + yPixel)) doReturn lo
    whenever(memory.load(bgPatternTableAddr + (idx * PATTERN_SIZE_BYTES) + yPixel + TILE_SIZE)) doReturn hi
  }
}
