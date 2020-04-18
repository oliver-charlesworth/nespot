package choliver.nes

import choliver.nes.cartridge.Cartridge
import choliver.nes.ppu.Ppu
import choliver.nes.sixfiveohtwo.Cpu
import choliver.nes.sixfiveohtwo.InstructionDecoder
import choliver.nes.sixfiveohtwo.model.ProgramCounter
import choliver.nes.sixfiveohtwo.model.State
import java.nio.IntBuffer

class Nes(
  rom: ByteArray,
  screen: IntBuffer,
  private val wat: Wat
) {
  interface Hooks {
    val state: State
    fun peek(addr: Address): Data
    fun peekV(addr: Address): Data
    fun fireReset()
    fun fireNmi()
    fun fireIrq()
    fun step()
    fun decodeAt(pc: ProgramCounter): InstructionDecoder.Decoded
  }

  interface Wat {
    fun onReset()
    fun onNmi()
    fun onIrq()
    fun onStore(addr: Address, data: Data)
  }

  private val reset = InterruptSource(wat::onReset)
  private val nmi = InterruptSource(wat::onNmi)
  private val irq = InterruptSource(wat::onIrq)

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

  private inner class InterceptingMemory(private val mem: Memory) : Memory by mem {
    override fun store(addr: Address, data: Data) {
      mem.store(addr, data)
      wat.onStore(addr, data)
    }
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

  val hooks = object : Hooks {
    override val state get() = cpu.state
    override fun peek(addr: Address) = cpuMapper.load(addr)
    override fun peekV(addr: Address) = ppuMapper.load(addr)
    override fun fireReset() = reset.set()
    override fun fireNmi() = nmi.set()
    override fun fireIrq() = irq.set()
    override fun step() = this@Nes.step()
    override fun decodeAt(pc: ProgramCounter) = cpu.decodeAt(pc)
  }

  private class InterruptSource(private val listener: () -> Unit) {
    private var b = false
    fun poll() = b.also { b = false }
    fun set() {
      b = true
      listener()
    }
  }

  // TODO - consolidate all the constants
  companion object {
    const val CPU_RAM_SIZE = 2048
    const val PPU_RAM_SIZE = 2048

    const val ADDR_OAMDATA: Address = 0x2004
    const val ADDR_OAMDMA: Address = 0x4014
  }
}
