package choliver.nespot.nes

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.addr
import choliver.nespot.apu.Apu
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPAD1
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPAD2
import choliver.nespot.nes.Nes.Companion.ADDR_JOYPADS
import choliver.nespot.nes.Nes.Companion.ADDR_OAMDATA
import choliver.nespot.nes.Nes.Companion.ADDR_OAMDMA
import choliver.nespot.ppu.Ppu

class CpuMapper(
  private val prg: Memory,
  private val ram: Memory,
  private val ppu: Ppu,
  private val apu: Apu,
  private val joypads: Joypads
) : Memory {
  override fun load(addr: Address) = when {
    addr < 0x2000 -> ram.load(addr % 2048)
    addr < 0x4000 -> ppu.readReg(addr % 8)
    addr == ADDR_JOYPAD1 -> joypads.read1()
    addr == ADDR_JOYPAD2 -> joypads.read2()
    addr < 0x4020 -> 0x00.also { println("Trying to read APU status") } // TODO - APU status register is readable
    else -> prg.load(addr)
  }

  override fun store(addr: Address, data: Data) = when {
    addr < 0x2000 -> ram.store(addr % 2048, data)
    addr < 0x4000 -> ppu.writeReg(addr % 8, data)
    addr == ADDR_OAMDMA -> oamDma(page = data)
    addr == ADDR_JOYPADS -> joypads.write(data)
    addr < 0x4020 -> apu.writeReg(addr - 0x4000, data)
    else -> prg.store(addr, data)
  }

  private fun oamDma(page: Data) {
    (0x00..0xFF).forEach { store(ADDR_OAMDATA, load(addr(hi = page, lo = it))) }
  }
}
