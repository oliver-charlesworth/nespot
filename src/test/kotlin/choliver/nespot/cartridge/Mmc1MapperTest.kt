package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.cartridge.Mmc1Mapper.Companion.BASE_SR
import choliver.nespot.data
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class Mmc1MapperTest {
  @Nested
  inner class PrgRam {
    private val mapper = Mmc1Mapper(MapperConfig(chrData = ByteArray(0)))

    @Test
    fun `load and store`() {
      mapper.prg.store(0x6000, 0x30) // Lowest mapped address
      mapper.prg.store(0x7FFF, 0x40) // Highest mapped address

      assertEquals(0x30, mapper.prg.load(0x6000))
      assertEquals(0x40, mapper.prg.load(0x7FFF))
    }
  }

  @Nested
  inner class PrgRom {
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

    @Test
    fun `bank-selection wraps`() {
      configure(mode = 0, bank = 6 + 8, data = mapOf(
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
    fun `starts up on max bank`() {
      configure(mode = 3, bank = null, data = mapOf(
        (7 * 16384) + 0 to 0x30,
        (7 * 16384) + 16383 to 0x40
      ))

      assertLoads(mapOf(
        0x8000 to 0x30,
        0xBFFF to 0x40
      ))
    }

    private fun configure(mode: Int, bank: Int?, data: Map<Int, Data>) {
      data.forEach { (addr, data) -> prgData[addr] = data.toByte() }
      mapper.writeReg(0, mode shl 2)
      if (bank != null) {
        mapper.writeReg(3, bank)
      }
    }

    private fun assertLoads(expected: Map<Address, Data>) {
      expected.forEach { (addr, data) -> assertEquals(data, mapper.prg.load(addr)) }
    }
  }

  @Nested
  inner class ChrRam {
    private val mapper = Mmc1Mapper(MapperConfig(chrData = ByteArray(0)))

    @Test
    fun `load and store`() {
      val chr = mapper.chr.intercept(mock())

      chr.store(0x0000, 0x30) // Lowest mapped address
      chr.store(0x1FFF, 0x40) // Highest mapped address

      assertEquals(0x30, chr.load(0x0000))
      assertEquals(0x40, chr.load(0x1FFF))
    }
  }

  @Nested
  inner class ChrRom {
    private val chrData = ByteArray(8 * 4096)
    private val mapper = Mmc1Mapper(MapperConfig(chrData = chrData))

    @Test
    fun `8k mode`() {
      // bank1 set to something weird to prove we ignore it
      configure(mode = 0, bank0 = 6, bank1 = 3, data = mapOf(
        (6 * 4096) + 0 to 0x30,
        (6 * 4096) + 4095 to 0x40,
        (6 * 4096) + 4096 to 0x50,
        (6 * 4096) + 8191 to 0x60
      ))

      assertLoads(mapOf(
        0x0000 to 0x30,
        0x0FFF to 0x40,
        0x1000 to 0x50,
        0x1FFF to 0x60
      ))
    }

    @Test
    fun `4k mode`() {
      configure(mode = 1, bank0 = 6, bank1 = 3, data = mapOf(
        (6 * 4096) + 0 to 0x30,
        (6 * 4096) + 4095 to 0x40,
        (3 * 4096) + 0 to 0x50,
        (3 * 4096) + 4095 to 0x60
      ))

      assertLoads(mapOf(
        0x0000 to 0x30,
        0x0FFF to 0x40,
        0x1000 to 0x50,
        0x1FFF to 0x60
      ))
    }

    @Test
    fun `bank-selection wraps`() {
      configure(mode = 1, bank0 = 6 + 8, bank1 = 3 + 8, data = mapOf(
        (6 * 4096) + 0 to 0x30,
        (6 * 4096) + 4095 to 0x40,
        (3 * 4096) + 0 to 0x50,
        (3 * 4096) + 4095 to 0x60
      ))

      assertLoads(mapOf(
        0x0000 to 0x30,
        0x0FFF to 0x40,
        0x1000 to 0x50,
        0x1FFF to 0x60
      ))
    }

    private fun configure(mode: Int, bank0: Int, bank1: Int, data: Map<Int, Data>) {
      data.forEach { (addr, data) -> chrData[addr] = data.toByte() }
      mapper.writeReg(0, mode shl 4)
      mapper.writeReg(1, bank0)
      mapper.writeReg(2, bank1)
    }

    private fun assertLoads(expected: Map<Address, Data>) {
      val chr = mapper.chr.intercept(mock())
      expected.forEach { (addr, data) -> assertEquals(data, chr.load(addr)) }
    }
  }

  @Nested
  inner class Vram {
    @Test
    fun `single-screen - nametable 0`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0000,
        0x23FF to 0x03FF,
        // Nametable 1
        0x2400 to 0x0000,
        0x27FF to 0x03FF,
        // Nametable 2
        0x2800 to 0x0000,
        0x2BFF to 0x03FF,
        // Nametable 3
        0x2C00 to 0x0000,
        0x2FFF to 0x03FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 0, source = source, target = target) }
    }

    @Test
    fun `single-screen - nametable 1`() {
      val cases = mapOf(
        // Nametable 0
        0x2000 to 0x0400,
        0x23FF to 0x07FF,
        // Nametable 1
        0x2400 to 0x0400,
        0x27FF to 0x07FF,
        // Nametable 2
        0x2800 to 0x0400,
        0x2BFF to 0x07FF,
        // Nametable 3
        0x2C00 to 0x0400,
        0x2FFF to 0x07FF
      )

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 1, source = source, target = target) }
    }

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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 2, source = source, target = target) }
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

      cases.forEach { (source, target) -> assertLoadAndStore(mode = 3, source = source, target = target) }
    }

    private fun assertLoadAndStore(mode: Int, source: Address, target: Address) {
      val mapper = Mmc1Mapper(MapperConfig(chrData = ByteArray(8192)))
      val vram = mock<Memory>()
      val chr = mapper.chr.intercept(vram)
      mapper.writeReg(0, mode)

      val data = (target + 23).data() // Arbitrary payload
      whenever(vram.load(target)) doReturn data

      assertEquals(data, chr.load(source))

      chr.store(source, data)
      verify(vram).store(target, data)
    }
  }

  private fun Mmc1Mapper.writeReg(idx: Int, data: Data) {
    val d = data and 0x1F
    val addr = BASE_SR or ((idx and 0x03) shl 13)
    prg.store(addr, 0x80)   // Reset
    prg.store(addr, d shr 0)
    prg.store(addr, d shr 1)
    prg.store(addr, d shr 2)
    prg.store(addr, d shr 3)
    prg.store(addr, d shr 4)
  }
}
