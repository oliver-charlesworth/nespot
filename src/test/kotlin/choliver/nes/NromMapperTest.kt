package choliver.nes

import choliver.nes.Cartridge.*
import choliver.nes.Cartridge.Mirroring.VERTICAL
import choliver.sixfiveohtwo.model.u16
import choliver.sixfiveohtwo.model.u8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IndexOutOfBoundsException

class NromMapperTest {

  @Test
  fun `rejects unmapped addresses`() {
    val mapper = mapper()

    assertThrows<IndexOutOfBoundsException> { mapper.prg.load(0x0000.u16()) }
    assertThrows<IndexOutOfBoundsException> { mapper.prg.load(0x5FFF.u16()) }

    // PRG-RAM unsupported for now
    assertThrows<IndexOutOfBoundsException> { mapper.prg.load(0x6000.u16()) }
    assertThrows<IndexOutOfBoundsException> { mapper.prg.load(0x7FFF.u16()) }
  }

  @Test
  fun `maps 0x8000 to 0xFFFF for size-32768 PRG-ROM`() {
    val mapper = mapper(prgData = ByteArray(32768).apply {
      this[0] = 0x30
      this[16383] = 0x40
      this[16384] = 0x50
      this[32767] = 0x60
    })

    assertEquals(0x30.u8(), mapper.prg.load(0x8000.u16()))
    assertEquals(0x40.u8(), mapper.prg.load(0xBFFF.u16()))
    assertEquals(0x50.u8(), mapper.prg.load(0xC000.u16()))
    assertEquals(0x60.u8(), mapper.prg.load(0xFFFF.u16()))
  }

  @Test
  fun `maps 0x8000 to 0xBFFF and 0xC000 to 0xFFFF for size-16384 PRG-ROM`() {
    val mapper = mapper(prgData = ByteArray(16384).apply {
      this[0] = 0x30
      this[16383] = 0x40
    })

    assertEquals(0x30.u8(), mapper.prg.load(0x8000.u16()))
    assertEquals(0x40.u8(), mapper.prg.load(0xBFFF.u16()))
    assertEquals(0x30.u8(), mapper.prg.load(0xC000.u16()))
    assertEquals(0x40.u8(), mapper.prg.load(0xFFFF.u16()))
  }

  @Test
  fun `maps 0x0000 to 0x1FFF for size-8192 CHR-ROM`() {
    val mapper = mapper(chrData = ByteArray(8192).apply {
      this[0] = 0x30
      this[8191] = 0x40
    })

    assertEquals(0x30.u8(), mapper.chr.load(0x0000.u16()))
    assertEquals(0x40.u8(), mapper.chr.load(0x1FFF.u16()))
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(32768),
    chrData: ByteArray = ByteArray(8192)
  ) = NromMapper(Stuff(
    hasPersistentMem = false,
    mirroring = VERTICAL,
    trainerData = byteArrayOf(),
    prgData = prgData,
    chrData = chrData
  ))
}
