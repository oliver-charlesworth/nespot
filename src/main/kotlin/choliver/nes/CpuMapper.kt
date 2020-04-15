package choliver.nes

import choliver.nes.Nes.Companion.ADDR_OAMDATA
import choliver.nes.Nes.Companion.ADDR_OAMDMA
import choliver.nes.cartridge.PrgMemory
import choliver.nes.ppu.Ppu

class CpuMapper(
  private val prg: PrgMemory,
  private val ram: Memory,
  private val ppu: Ppu
) : Memory {
  override fun load(addr: Address) = when {
    addr < 0x2000 -> ram.load(addr % 2048)
    addr < 0x4000 -> ppu.readReg(addr % 8)
    addr < 0x4020 -> 0x00 // TODO
    else -> prg.load(addr)!!
  }

  override fun store(addr: Address, data: Data) = when {
    addr < 0x2000 -> ram.store(addr % 2048, data)
    addr < 0x4000 -> ppu.writeReg(addr % 8, data)
    addr == ADDR_OAMDMA -> oamDma(page = data)
    addr < 0x4020 -> {} // TODO
    else -> prg.store(addr, data)
  }

  private fun oamDma(page: Data) {
    (0x00..0xFF).forEach { store(ADDR_OAMDATA, load(addr(hi = page, lo = it))) }
  }
}
