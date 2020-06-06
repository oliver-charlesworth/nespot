package choliver.nespot.nes

import choliver.nespot.apu.Apu
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.common.addr
import choliver.nespot.memory.Memory
import choliver.nespot.nes.Nes.Companion.ADDR_APU_STATUS
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
  override fun get(addr: Address) = when {
    addr >= 0x4020 -> prg[addr]
    addr < 0x2000 -> ram[addr % 2048]
    addr < 0x4000 -> ppu.readReg(addr % 8)
    addr == ADDR_JOYPAD1 -> joypads.read1()
    addr == ADDR_JOYPAD2 -> joypads.read2()
    addr == ADDR_APU_STATUS -> apu.readStatus()
    else -> throw RuntimeException()   // Should never happen
  }

  override fun set(addr: Address, data: Data) = when {
    addr < 0x2000 -> ram[addr % 2048] = data
    addr < 0x4000 -> ppu.writeReg(addr % 8, data)
    addr == ADDR_OAMDMA -> oamDma(page = data)
    addr == ADDR_JOYPADS -> joypads.write(data)
    addr < 0x4020 -> apu.writeReg(addr - 0x4000, data)
    else -> prg[addr] = data
  }

  private fun oamDma(page: Data) {
    (0x00..0xFF).forEach { this[ADDR_OAMDATA] = this[addr(hi = page, lo = it)] }
  }
}
