package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.ppu.Ppu
import choliver.nes.sixfiveohtwo.Cpu
import choliver.nes.sixfiveohtwo.model.ProgramCounter

class Nes(rom: ByteArray) {
  private val cartridge = Cartridge(rom)

  private val cpuRam = Ram(CPU_RAM_SIZE)
  private val ppuRam = Ram(PPU_RAM_SIZE)

  private val ppuMapper = PpuMapper(ppuRam)

  private val ppu = Ppu(ppuMapper)

  private val cpuMapper = CpuMapper(
    prg = cartridge.prg,
    ram = cpuRam,
    ppu = ppu
  )

  private class InterceptingMemory(private val mem: Memory) : Memory by mem {
    private val _stores = mutableListOf<Pair<Address, Data>>()

    override fun store(addr: Address, data: Data) {
      mem.store(addr, data)
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

    fun nmi() {
      cpu.nmi()
    }

    fun irq() {
      cpu.irq()
    }

    fun step(): List<Pair<Address, Data>> {
      interceptor.reset()
      cpu.step()
      return interceptor.stores
    }

    fun peek(addr: Address) = cpuMapper.load(addr)

    fun peekV(addr: Address) = ppuRam.load(addr)  // TODO - use PPU mapper?

    val state get() = cpu.state

    fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)
  }

  companion object {
    const val CPU_RAM_SIZE = 2048
    const val PPU_RAM_SIZE = 2048

    const val ADDR_OAMDATA: Address = 0x2004
    const val ADDR_OAMDMA: Address = 0x4014
  }
}
