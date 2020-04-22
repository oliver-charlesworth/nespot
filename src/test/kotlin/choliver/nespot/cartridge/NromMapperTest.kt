package choliver.nespot.cartridge

import choliver.nespot.cartridge.ChrMemory.ChrLoadResult
import choliver.nespot.cartridge.ChrMemory.ChrStoreResult
import choliver.nespot.cartridge.MapperConfig.Mirroring
import choliver.nespot.cartridge.MapperConfig.Mirroring.HORIZONTAL
import choliver.nespot.cartridge.MapperConfig.Mirroring.VERTICAL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NromMapperTest {

  @Test
  fun `returns null for unmapped addresses`() {
    val mapper = mapper()

    assertNull(mapper.prg.load(0x0000))
    assertNull(mapper.prg.load(0x5FFF))
  }

  @Test
  fun `maps 0x8000 to 0xFFFF for size-32768 PRG-ROM`() {
    val mapper = mapper(prgData = ByteArray(32768).apply {
      this[0] = 0x30
      this[16383] = 0x40
      this[16384] = 0x50
      this[32767] = 0x60
    })

    assertEquals(0x30, mapper.prg.load(0x8000))
    assertEquals(0x40, mapper.prg.load(0xBFFF))
    assertEquals(0x50, mapper.prg.load(0xC000))
    assertEquals(0x60, mapper.prg.load(0xFFFF))
  }

  @Test
  fun `maps 0x8000 to 0xBFFF and 0xC000 to 0xFFFF for size-16384 PRG-ROM`() {
    val mapper = mapper(prgData = ByteArray(16384).apply {
      this[0] = 0x30
      this[16383] = 0x40
    })

    assertEquals(0x30, mapper.prg.load(0x8000))
    assertEquals(0x40, mapper.prg.load(0xBFFF))
    assertEquals(0x30, mapper.prg.load(0xC000))
    assertEquals(0x40, mapper.prg.load(0xFFFF))
  }

  @Test
  fun `maps 0x0000 to 0x1FFF for CHR-ROM`() {
    val mapper = mapper(chrData = ByteArray(8192).apply {
      this[0] = 0x30
      this[8191] = 0x40
    })

    assertEquals(ChrLoadResult.Data(0x30), mapper.chr.load(0x0000))
    assertEquals(ChrLoadResult.Data(0x40), mapper.chr.load(0x1FFF))

    assertEquals(ChrStoreResult.None, mapper.chr.store(0x0000, 0x30))
    assertEquals(ChrStoreResult.None, mapper.chr.store(0x1FFF, 0x30))
  }

  @Test
  fun `vertically maps 0x2000 to 0x3EFF to VRAM for CHR-ROM`() {
    val mapper = mapper(mirroring = VERTICAL)

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

    cases.forEach { (source, target) ->
      assertEquals(ChrLoadResult.VramAddr(target), mapper.chr.load(source))
      assertEquals(ChrStoreResult.VramAddr(target), mapper.chr.store(source, 0x30))
    }
  }

  @Test
  fun `horizontally maps 0x2000 to 0x3EFF to VRAM for CHR-ROM`() {
    val mapper = mapper(mirroring = HORIZONTAL)

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

    cases.forEach { (source, target) ->
      assertEquals(ChrLoadResult.VramAddr(target), mapper.chr.load(source))
      assertEquals(ChrStoreResult.VramAddr(target), mapper.chr.store(source, 0x30))
    }
  }

  private fun mapper(
    prgData: ByteArray = ByteArray(32768),
    chrData: ByteArray = ByteArray(8192),
    mirroring: Mirroring = VERTICAL
  ) = NromMapper(MapperConfig(
    hasPersistentMem = false,
    mirroring = mirroring,
    trainerData = byteArrayOf(),
    prgData = prgData,
    chrData = chrData
  ))
}
