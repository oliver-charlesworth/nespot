package choliver.nes

import choliver.nes.Nes.Companion.ADDR_OAMDMA
import choliver.nes.cartridge.PrgMemory
import choliver.nes.ppu.Ppu
import choliver.nes.ppu.Ppu.Companion.REG_OAMDATA
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CpuMapperTest {
  @Test
  fun `maps to prg`() {
    val prg = mock<PrgMemory> {
      on { load(0x4020) } doReturn 0x10
      on { load(0xFFFF) } doReturn 0x20
    }
    val mapper = CpuMapper(prg = prg, ram = mock(), ppu = mock())

    assertEquals(0x10, mapper.load(0x4020))
    assertEquals(0x20, mapper.load(0xFFFF))

    mapper.store(0x4020, 0x30)
    mapper.store(0xFFFF, 0x40)

    verify(prg).store(0x4020, 0x30)
    verify(prg).store(0xFFFF, 0x40)
  }

  @Test
  fun `maps to ppu`() {
    val ppu = mock<Ppu> {
      on { readReg(0) } doReturn 0x10
      on { readReg(7) } doReturn 0x20
    }
    val mapper = CpuMapper(prg = mock(), ram = mock(), ppu = ppu)

    assertEquals(0x10, mapper.load(0x2000))
    assertEquals(0x20, mapper.load(0x2007))
    assertEquals(0x10, mapper.load(0x2008)) // First mirror
    assertEquals(0x20, mapper.load(0x3FFF)) // Last mirror

    mapper.store(0x2000, 0x30)
    mapper.store(0x2007, 0x40)
    mapper.store(0x2008, 0x50)  // First mirror
    mapper.store(0x3FFF, 0x60)  // Last mirror

    verify(ppu).writeReg(0, 0x30)
    verify(ppu).writeReg(7, 0x40)
    verify(ppu).writeReg(0, 0x50)
    verify(ppu).writeReg(7, 0x60)
  }

  @Test
  fun `maps to ram`() {
    val ram = mock<Memory> {
      on { load(0x0000) } doReturn 0x10
      on { load(0x07FF) } doReturn 0x20
    }
    val mapper = CpuMapper(prg = mock(), ram = ram, ppu = mock())

    assertEquals(0x10, mapper.load(0x0000))
    assertEquals(0x20, mapper.load(0x07FF))
    assertEquals(0x10, mapper.load(0x0800)) // First mirror
    assertEquals(0x20, mapper.load(0x1FFF)) // Last mirror

    mapper.store(0x0000, 0x30)
    mapper.store(0x07FF, 0x40)
    mapper.store(0x0800, 0x50)  // First mirror
    mapper.store(0x1FFF, 0x60)  // Last mirror

    verify(ram).store(0x0000, 0x30)
    verify(ram).store(0x07FF, 0x40)
    verify(ram).store(0x0000, 0x50)
    verify(ram).store(0x07FF, 0x60)
  }

  @Test
  fun `performs oam dma`() {
    val ppu = mock<Ppu>()
    val ram = mock<Memory> {
      (0..255).forEach { on { load(0x0500 + it) } doReturn (0xFF - it) }
    }
    val mapper = CpuMapper(
      prg = mock(),
      ram = ram,
      ppu = ppu
    )

    mapper.store(ADDR_OAMDMA, 0x05)

    (0..255).forEach {
      verify(ppu).writeReg(REG_OAMDATA, 0xFF - it)
    }
  }
}
