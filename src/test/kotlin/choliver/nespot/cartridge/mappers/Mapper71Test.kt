package choliver.nespot.cartridge.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.Rom.Mirroring.VERTICAL
import choliver.nespot.cartridge.mappers.Mapper71.Companion.BASE_BANK_SELECT
import choliver.nespot.data
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class Mapper71Test {
  @Nested
  inner class PrgRom {
    private val prgData = ByteArray(8 * 16384)
    private val mapper = mapper(prgData = prgData)

    @Test
    fun `fixed upper`() {
      configure(bank = 6, data = mapOf(
        (7 * 16384) + 0 to 0x30,
        (7 * 16384) + 16383 to 0x40
      ))

      assertLoads(mapOf(
        0xC000 to 0x30,
        0xFFFF to 0x40
      ))
    }

    @Test
    fun `variable lower`() {
      configure(bank = 6, data = mapOf(
        (6 * 16384) + 0 to 0x30,
        (6 * 16384) + 16383 to 0x40
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40
      ))
    }

    @Test
    fun `bank-selection wraps`() {
      configure(bank = 6 + 8, data = mapOf(
        (6 * 16384) + 0 to 0x30,
        (6 * 16384) + 16383 to 0x40
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40
      ))
    }

    @Test
    fun `starts up on min bank`() {
      configure(bank = null, data = mapOf(
        (0 * 16384) + 0 to 0x30,
        (0 * 16384) + 16383 to 0x40
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40
      ))
    }

    private fun configure(bank: Int?, data: Map<Int, Data>) {
      data.forEach { (addr, data) -> prgData[addr] = data.toByte() }
      if (bank != null) {
        mapper.prg.store(BASE_BANK_SELECT, bank)
      }
    }

    private fun assertLoads(expected: Map<Address, Data>) {
      expected.forEach { (addr, data) -> assertEquals(data, mapper.prg.load(addr)) }
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = mapper()

    @Test
    fun `load and store`() {
      val chr = mapper.chr(mock())

      chr.store(0x0000, 0x30) // Lowest mapped address
      chr.store(0x1FFF, 0x40) // Highest mapped address

      assertEquals(0x30, chr.load(0x0000))
      assertEquals(0x40, chr.load(0x1FFF))
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `vertical mirroring`() {
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
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(VERTICAL, source = source, target = target) }
    }

    @Test
    fun `horizontal mirroring`() {
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
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(HORIZONTAL, source = source, target = target) }
    }

    private fun assertLoadAndStore(mirroring: Mirroring, source: Address, target: Address) {
      val mapper = mapper(mirroring = mirroring, chrData = ByteArray(8192))
      val vram = mock<Memory>()
      val chr = mapper.chr(vram)

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram.load(target)) doReturn data

      assertEquals(data, chr.load(source))

      chr.store(source, data)
      verify(vram).store(target, data)
    }
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(32768),
    chrData: ByteArray = ByteArray(8192),
    mirroring: Mirroring = VERTICAL
  ) = Mapper71(Rom(
    mirroring = mirroring,
    prgData = prgData,
    chrData = chrData
  ))
}
