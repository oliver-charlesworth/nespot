package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.ppu.Ppu
import choliver.nes.ppu.SCREEN_HEIGHT
import choliver.nes.sixfiveohtwo.Cpu
import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import choliver.nes.sixfiveohtwo.model.State
import java.nio.IntBuffer

class Nes(
  rom: ByteArray,
  screen: IntBuffer
) {
  interface Hooks {
    val state: State
    fun peek(addr: Address): Data
    fun peekV(addr: Address): Data
    fun fireReset()
    fun fireNmi()
    fun fireIrq()
  }

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
    numCycles += cpu.runSteps(1)

    // TODO - where does this magic number come from?
    if (numCycles >= 124) {
      numCycles -= 124
      ppu.renderNextScanline()
    }
  }

  private inner class HooksImpl : Hooks {
    override val state get() = cpu.state
    override fun peek(addr: Address) = cpuMapper.load(addr)
    override fun peekV(addr: Address) = ppuMapper.load(addr)
    override fun fireReset() = reset.set()
    override fun fireNmi() = nmi.set()
    override fun fireIrq() = irq.set()
  }

  val instrumentation = Instrumentation(HooksImpl())

  inner class Instrumentation(private val hooks: Hooks) {
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

    fun reset() = hooks.fireReset()

    fun nmi()  = hooks.fireNmi()

    fun irq() = hooks.fireNmi()

    fun step(): List<Pair<Address, Data>> {
      interceptor.reset()
      this@Nes.step()
      return interceptor.stores
    }

    fun peek(addr: Address) = hooks.peek(addr)
    fun peekV(addr: Address) = hooks.peekV(addr)

    val state get() = hooks.state

    fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)
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
