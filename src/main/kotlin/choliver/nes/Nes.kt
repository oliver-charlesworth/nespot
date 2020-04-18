package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.ppu.Ppu
import choliver.nes.ppu.SCREEN_HEIGHT
import choliver.nes.sixfiveohtwo.Cpu
import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import java.nio.IntBuffer

class Nes(
  rom: ByteArray,
  screen: IntBuffer
) {
  private val reset = InterruptSource()
  private val irq = InterruptSource()
  private val nmi = InterruptSource()

  private val cartridge = Cartridge(rom)

  private val cpuRam = Ram(CPU_RAM_SIZE)
  private val ppuRam = Ram(PPU_RAM_SIZE)

  private val ppuMapper = PpuMapper(
    chr = cartridge.chr,
    ram = ppuRam
  )

  private val ppu = Ppu(
    memory = ppuMapper,
    screen = screen,
    onVbl = nmi::set
  )

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

  private val cpu = Cpu(
    interceptor,
    pollReset = reset::poll,
    pollIrq = irq::poll,
    pollNmi = nmi::poll
  )

  private var numCycles = 0

  private fun step() {
    // TODO - where does this magic number come from?
    if (numCycles >= 124) {
      numCycles -= 124
      ppu.renderNextScanline()
    }
    numCycles += cpu.runSteps(1)
  }

  val instrumentation = Instrumentation()

  inner class Instrumentation {
    // TODO - ugh mutable
    var onReset: () -> Unit = {}
    var onNmi: () -> Unit = {}
    var onIrq: () -> Unit = {}

    init {
      // Callbacks are mutable, so we have to invoke them via lambdas
      reset.addListener { onReset() }
      nmi.addListener { onNmi() }
      irq.addListener { onIrq() }
    }

    fun reset() {
      reset.set()
    }

    fun nmi() {
      nmi.set()
    }

    fun irq() {
      irq.set()
    }

    fun step(): List<Pair<Address, Data>> {
      interceptor.reset()
      this@Nes.step()
      return interceptor.stores
    }

    fun peek(addr: Address) = cpuMapper.load(addr)
    fun peekV(addr: Address) = ppuRam.load(addr)  // TODO - use PPU mapper?

    val state get() = cpu.state

    // TODO - combine
    fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)
    fun calcAddr(instruction: Instruction) = cpu.calcAddr(instruction)
  }

  private class InterruptSource {
    private val listeners = mutableListOf<() -> Unit>()
    private var b = false
    fun poll() = b.also { b = false }
    fun set() {
      b = true
      listeners.forEach { it() }
    }
    fun addListener(listener: () -> Unit) { listeners += listener } // TODO - eliminate this mutable weirdness
  }

  // TODO - consolidate all the constants
  companion object {
    const val CPU_RAM_SIZE = 2048
    const val PPU_RAM_SIZE = 2048

    const val ADDR_OAMDATA: Address = 0x2004
    const val ADDR_OAMDMA: Address = 0x4014
  }
}
