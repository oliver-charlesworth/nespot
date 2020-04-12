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

  private class InterceptingMemory(mem: Memory) : Memory by mem {
    private val _stores = mutableListOf<Pair<Address, Data>>()

    override fun store(addr: Address, data: Data) {
      _stores += (addr to data)
    }

    fun reset() {
      _stores.clear()
    }

    val stores get() = _stores.toList()
  }

  private val interceptor = InterceptingMemory(cpuMapper)

  private val cpu = Cpu(interceptor)

  val instrumentation = Instrumentation()

  init {
    instrumentation.reset()
  }

  inner class Instrumentation internal constructor() {
    fun reset() {
      cpu.reset()
    }

    fun step(): List<Pair<Address, Data>> {
      interceptor.reset()
      cpu.step()
      return interceptor.stores
    }

    fun peek(addr: Address) = cpuMapper.load(addr)

    val state get() = cpu.state

    fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)
  }
}
