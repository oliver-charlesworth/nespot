package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.cartridge.Mmc1Mapper.Companion.SR_RANGE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class Mmc1MapperTest {

  // TODO - something about startup state?
  // TODO - shift register mechanics (address range, internal address range, reset, internal address only on last write)
  // TODO - mirroring modes
  // TODO - CHR-ROM modes

  @Nested
  inner class Prg {
    private val prgData = ByteArray(8 * 16384)
    private val mapper = Mmc1Mapper(MapperConfig(prgData = prgData))

    @ParameterizedTest
    @ValueSource(ints = [0, 1])
    fun `32k mode`(mode: Int) {
      configure(mode = mode, bank = 6, data = mapOf(
        (6 * 16384) + 0 to 0x30,
        (6 * 16384) + 16383 to 0x40,
        (6 * 16384) + 16384 to 0x50,
        (6 * 16384) + 32767 to 0x60
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40,
        0xC000 to 0x50,
        0xFFFF to 0x60
      ))
    }

    @Test
    fun `variable upper`() {
      configure(mode = 2, bank = 6, data = mapOf(
        // First bank (fixed)
        0 to 0x30,
        16383 to 0x40,
        // Variable bank
        (6 * 16384) + 0 to 0x50,
        (6 * 16384) + 16383 to 0x60
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40,
        0xC000 to 0x50,
        0xFFFF to 0x60
      ))
    }

    @Test
    fun `variable lower`() {
      configure(mode = 3, bank = 6, data = mapOf(
        // Variable bank
        (6 * 16384) + 0 to 0x30,
        (6 * 16384) + 16383 to 0x40,
        // Last bank (fixed)
        (7 * 16384) + 0 to 0x50,
        (7 * 16384) + 16383 to 0x60
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40,
        0xC000 to 0x50,
        0xFFFF to 0x60
      ))
    }

    private fun configure(mode: Int, bank: Int, data: Map<Int, Data>) {
      data.forEach { (addr, data) -> prgData[addr] = data.toByte() }
      mapper.writeReg(0, mode shl 2)
      mapper.writeReg(3, bank)
    }

    private fun assertLoads(expected: Map<Address, Data>) {
      expected.forEach { (addr, data) -> assertEquals(data, mapper.prg.load(addr)) }
    }
  }

  private fun Mmc1Mapper.writeReg(idx: Int, data: Data) {
    val d = data and 0x1F
    val addr = SR_RANGE.first or ((idx and 0x03) shl 13)
    prg.store(addr, 0x80)   // Reset
    prg.store(addr, d shr 0)
    prg.store(addr, d shr 1)
    prg.store(addr, d shr 2)
    prg.store(addr, d shr 3)
    prg.store(addr, d shr 4)
  }
}
