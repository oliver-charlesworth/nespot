package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.data
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NromMapperTest {

  @Nested
  inner class Prg {
    @Test
    fun `maps 0x8000 to 0xFFFF for size-32768`() {
      val mapper = mapper(prgData = ByteArray(32768).apply {
        this[0] = 0x30
        this[16383] = 0x40
        this[16384] = 0x50
        this[32767] = 0x60
      })

      assertEquals(0x30, mapper.prg[0x8000])
      assertEquals(0x40, mapper.prg[0xBFFF])
      assertEquals(0x50, mapper.prg[0xC000])
      assertEquals(0x60, mapper.prg[0xFFFF])
    }

    @Test
    fun `maps 0x8000 to 0xBFFF and 0xC000 to 0xFFFF for size-16384`() {
      val mapper = mapper(prgData = ByteArray(16384).apply {
        this[0] = 0x30
        this[16383] = 0x40
      })

      assertEquals(0x30, mapper.prg[0x8000])
      assertEquals(0x40, mapper.prg[0xBFFF])
      assertEquals(0x30, mapper.prg[0xC000])
      assertEquals(0x40, mapper.prg[0xFFFF])
    }
  }

  @Nested
  inner class Chr {
    @Test
    fun `maps 0x0000 to 0x1FFF`() {
      val mapper = mapper(chrData = ByteArray(8192).apply {
        this[0] = 0x30
        this[8191] = 0x40
      })
      val chr = mapper.chr(mock())

      assertEquals(0x30, chr[0x0000])
      assertEquals(0x40, chr[0x1FFF])
    }

    @Test
    fun `vertically maps 0x2000 to 0x3EFF to VRAM`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0400,
        0x27FF to 0x07FF,
        // Nametable 2
        0x2800 to 0x0000,
        0x2BFF to 0x03FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF,
        // Nametable 0 (source mirror)
        0x3000 to 0x0000,
        0x33FF to 0x03FF,
        // Nametable 1 (source mirror)
        0x3400 to 0x0400,
        0x37FF to 0x07FF,
        // Nametable 2 (source mirror)
        0x3800 to 0x0000,
        0x3BFF to 0x03FF,
        // Nametable 3 (source mirror)
        0x3C00 to 0x0400,
        0x3EFF to 0x06FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(VERTICAL, source = source, target = target) }
    }

    @Test
    fun `horizontally maps 0x2000 to 0x3EFF to VRAM`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0000,
        0x27FF to 0x03FF,
        // Nametable 2
        0x2800 to 0x0400,
        0x2BFF to 0x07FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF,
        // Nametable 0 (source mirror)
        0x3000 to 0x0000,
        0x33FF to 0x03FF,
        // Nametable 1 (source mirror)
        0x3400 to 0x0000,
        0x37FF to 0x03FF,
        // Nametable 2 (source mirror)
        0x3800 to 0x0400,
        0x3BFF to 0x07FF,
        // Nametable 3 (source mirror)
        0x3C00 to 0x0400,
        0x3EFF to 0x06FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(HORIZONTAL, source = source, target = target) }
    }

    private fun assertLoadAndStore(mirroring: Mirroring, source: Address, target: Address) {
      val mapper = mapper(mirroring = mirroring)
      val vram = mock<Memory>()
      val chr = mapper.chr(vram)

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram[target]) doReturn data

      assertEquals(data, chr[source])

      chr[source] = data
      verify(vram)[target] = data
    }
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(32768),
    chrData: ByteArray = ByteArray(8192),
    mirroring: Mirroring = VERTICAL
  ) = NromMapper(Rom(
    mirroring = mirroring,
    prgData = prgData,
    chrData = chrData
  ))
}
