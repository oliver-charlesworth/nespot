package choliver.nespot.nes

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.Ram
import choliver.nespot.apu.Apu
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.ppu.Ppu
import choliver.nespot.sixfiveohtwo.Cpu
import java.nio.IntBuffer

class Nes(
  rom: Rom,
  videoBuffer: IntBuffer,
  audioBuffer: ByteArray,
  joypads: Joypads,
  onReset: () -> Unit = {},
  onNmi: () -> Unit = {},
  onIrq: () -> Unit = {},
  private val onStore: (Address, Data) -> Unit = { _: Address, _: Data -> }
) {
  private val reset = InterruptSource(onReset)
  private val nmi = InterruptSource(onNmi)
  private val irq = InterruptSource(onIrq)

  private val cartridge = Cartridge(rom)

  private val apu = Apu(
    buffer = audioBuffer,
    memory = cartridge.prg  // DMC can only read from PRG space
  )

  private val cpuRam = Ram(CPU_RAM_SIZE)
  private val ppuRam = Ram(PPU_RAM_SIZE)

  private val ppuMapper = cartridge.chr(ppuRam)

  private val ppu = Ppu(
    memory = ppuMapper,
    videoBuffer = videoBuffer
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
      override fun get(addr: Address) = cpuMapper[addr]
      override fun set(addr: Address, data: Data) {
        cpuMapper[addr] = data
        onStore(addr, data)
      }
    },
    pollReset = reset::poll,
    pollIrq = apu::irq, // TODO - wire up to debugger (in both directions)
    pollNmi = ppu::vbl  // TODO - wire up to debugger (in both directions)
  )

  private val sequencer = Sequencer(cpu, apu, ppu)

  fun runToEndOfFrame() {
    sequencer.runToEndOfFrame()
  }

  private class InterruptSource(private val listener: () -> Unit) {
    private var b = false
    fun poll() = b.also { b = false }
    fun set() {
      b = true
      listener()
    }
  }

  inner class Diagnostics internal constructor() {
    val sequencer = this@Nes.sequencer.diagnostics
    val cpu = this@Nes.cpu.diagnostics
    val ppu = this@Nes.ppu.diagnostics
    val ram = this@Nes.cpuRam
    val vram = this@Nes.ppuRam
    fun peek(addr: Address) = cpuMapper[addr]
    fun peekV(addr: Address) = ppuMapper[addr]
    fun fireReset() = reset.set()
    fun fireNmi() = nmi.set()
    fun fireIrq() = irq.set()
  }

  val diagnostics = Diagnostics()

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
  }
}
