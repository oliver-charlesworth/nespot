package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.ppu.Ppu
import choliver.nes.sixfiveohtwo.Cpu
import choliver.nes.sixfiveohtwo.model.ProgramCounter

class Nes(rom: ByteArray) {
  private val cartridge = Cartridge(rom)

  private val cpuRam = Ram(2048)
  private val ppuRam = Ram(2048)

  // TODO - PPU mapper

  private val ppu = Ppu(ppuRam)

  private val cpuMapper = object : Memory {
    override fun load(addr: Address) = when {
      addr < 0x2000 -> cpuRam.load(addr % 2048)
      addr < 0x4000 -> ppu.readReg(addr % 8)
      else -> cartridge.prg.load(addr)!!
    }

    override fun store(addr: Address, data: Data) = when {
      addr < 0x2000 -> cpuRam.store(addr % 2048, data)
      addr < 0x4000 -> ppu.writeReg(addr % 8, data)
      else -> cartridge.prg.store(addr, data)
    }
  }

  private val cpu = Cpu(cpuMapper)

  internal val state get() = cpu.state

  internal fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)

  init {
    reset()
  }

  fun reset() {
    cpu.reset()
  }

  fun step() {
    cpu.step()
  }
}
