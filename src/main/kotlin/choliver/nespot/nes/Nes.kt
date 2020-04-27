package choliver.nespot.nes

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.Ram
import choliver.nespot.apu.Apu
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu
import choliver.nespot.sixfiveohtwo.InstructionDecoder
import choliver.nespot.sixfiveohtwo.model.State
import java.nio.IntBuffer

class Nes(
  rom: ByteArray,
  videoBuffer: IntBuffer,
  audioBuffer: ByteArray,
  joypads: Joypads,
  onReset: () -> Unit = {},
  onNmi: () -> Unit = {},
  onIrq: () -> Unit = {},
  private val onStore: (Address, Data) -> Unit = { _: Address, _: Data -> }
) {
  interface Inspection {
    val state: State
    val endOfFrame: Boolean
    fun peek(addr: Address): Data
    fun peekV(addr: Address): Data
    fun fireReset()
    fun fireNmi()
    fun fireIrq()
    fun step()
    fun decodeAt(pc: Address): InstructionDecoder.Decoded
  }

  private val reset = InterruptSource(onReset)
  private val nmi = InterruptSource(onNmi)
  private val irq = InterruptSource(onIrq)

  private val cartridge = Cartridge(rom)

  private val apu = Apu(audioBuffer, cartridge.prg)

  private val cpuRam = Ram(CPU_RAM_SIZE)
  private val ppuRam = Ram(PPU_RAM_SIZE)

  private val ppuMapper = cartridge.chr.intercept(ppuRam)

  private val ppu = Ppu(
    memory = ppuMapper,
    videoBuffer = videoBuffer,
    onVbl = nmi::set
  )

  private val cpuMapper = CpuMapper(
    prg = cartridge.prg,
    ram = cpuRam,
    ppu = ppu,
    apu = apu,
    joypads = joypads
  )

  private val cpu = Cpu(
    object : Memory {
      override fun load(addr: Address) = cpuMapper.load(addr)
      override fun store(addr: Address, data: Data) {
        cpuMapper.store(addr, data)
        onStore(addr, data)
      }
    },
    pollReset = reset::poll,
    pollIrq = irq::poll,
    pollNmi = nmi::poll
  )

  private var numCycles = 0
  private var endOfFrame = false

  fun runToEndOfFrame() {
    do step() while (!endOfFrame)
  }

  private fun step() {
    endOfFrame = false
    numCycles += cpu.executeStep()
    if (numCycles >= NUM_CYCLES_PER_SCANLINE) {
      numCycles -= NUM_CYCLES_PER_SCANLINE

      ppu.executeScanline()

      if (0 == ppu.scanline) {
        apu.generate()
        endOfFrame = true
      }
    }
  }

  val inspection = object : Inspection {
    override val state get() = cpu.state
    override val endOfFrame get() = this@Nes.endOfFrame
    override fun peek(addr: Address) = cpuMapper.load(addr)
    override fun peekV(addr: Address) = ppuMapper.load(addr)
    override fun fireReset() = reset.set()
    override fun fireNmi() = nmi.set()
    override fun fireIrq() = irq.set()
    override fun step() = this@Nes.step()
    override fun decodeAt(pc: Address) = cpu.decodeAt(pc)
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
    const val ADDR_APU_STATUS: Address = 0x4015
    const val ADDR_JOYPADS: Address = 0x4016
    const val ADDR_JOYPAD1: Address = 0x4016
    const val ADDR_JOYPAD2: Address = 0x4017

    // See http://wiki.nesdev.com/w/index.php/Cycle_reference_chart#Clock_rates
    const val NUM_CYCLES_PER_SCANLINE = 113
  }
}
