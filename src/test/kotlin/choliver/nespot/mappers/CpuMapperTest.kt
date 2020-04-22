package choliver.nespot.mappers

import choliver.nespot.nes.Joypads
import choliver.nespot.Memory
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPAD1
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPAD2
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPADS
import choliver.nespot.nes.Nes.Companion.ADDR_OAMDMA
import choliver.nespot.ppu.Ppu
import choliver.nespot.ppu.Ppu.Companion.REG_OAMDATA
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CpuMapperTest {
  private val prg = mock<Memory>()
  private val ram = mock<Memory>()
  private val ppu = mock<Ppu>()
  private val joypads = mock<Joypads>()
  private val mapper = CpuMapper(
    prg = prg,
    ram = ram,
    ppu = ppu,
    joypads = joypads
  )

  @Test
  fun `maps to prg`() {
    whenever(prg.load(0x4020)) doReturn 0x10
    whenever(prg.load(0xFFFF)) doReturn 0x20

    assertEquals(0x10, mapper.load(0x4020))
    assertEquals(0x20, mapper.load(0xFFFF))

    mapper.store(0x4020, 0x30)
    mapper.store(0xFFFF, 0x40)

    verify(prg).store(0x4020, 0x30)
    verify(prg).store(0xFFFF, 0x40)
  }

  @Test
  fun `maps to ppu`() {
    whenever(ppu.readReg(0)) doReturn 0x10
    whenever(ppu.readReg(7)) doReturn 0x20

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
    whenever(ram.load(0x0000)) doReturn 0x10
    whenever(ram.load(0x07FF)) doReturn 0x20

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
  fun `maps to joypads`() {
    whenever(joypads.read1()) doReturn 0x10
    whenever(joypads.read2()) doReturn 0x20

    assertEquals(0x10, mapper.load(ADDR_JOYPAD1))
    assertEquals(0x20, mapper.load(ADDR_JOYPAD2))

    mapper.store(ADDR_JOYPADS, 0x30)

    verify(joypads).write(0x30)
  }

  @Test
  fun `performs oam dma`() {
    (0..255).forEach { whenever(ram.load(0x0500 + it)) doReturn (0xFF - it) }

    mapper.store(ADDR_OAMDMA, 0x05)

    (0..255).forEach {
      verify(ppu).writeReg(REG_OAMDATA, 0xFF - it)
    }
  }
}
