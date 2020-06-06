package choliver.nespot.nes

import choliver.nespot.CPU_FREQ_HZ
import choliver.nespot.apu.Apu
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import choliver.nespot.cpu.Cpu
import choliver.nespot.cpu.Cpu.Companion.INTERRUPT_IRQ
import choliver.nespot.cpu.Cpu.Companion.INTERRUPT_NMI
import choliver.nespot.memory.Memory
import choliver.nespot.memory.Ram
import choliver.nespot.ppu.Ppu

class Nes(
  rom: Rom,
  videoSink: VideoSink = object : VideoSink {},
  audioSink: AudioSink = object : AudioSink {},
  private val onStore: ((Address, Data) -> Unit)? = null
) {
  private var steps = 0

  private val cartridge = Cartridge.create(rom, getStepCount = { steps })

  private val apu = Apu(
    cpuFreqHz = CPU_FREQ_HZ,
    memory = cartridge.prg,  // DMC can only read from PRG space
    audioSink = audioSink
  )

  private val cpuRam = Ram(CPU_RAM_SIZE)

  private val ppu = Ppu(
    memory = cartridge.chr,
    videoSink = videoSink
  )

  val joypads = Joypads()

  private val cpuMapper = CpuMapper(
    prg = cartridge.prg,
    ram = cpuRam,
    ppu = ppu,
    apu = apu,
    joypads = joypads
  )

  private val cpu = Cpu(
    maybeIntercept(cpuMapper),
    pollInterrupts = ::pollInterrupts
  )

  fun step(): Int {
    val cycles = cpu.executeStep()
    apu.advance(cycles)
    ppu.advance(cycles)
    steps++
    return cycles
  }

  private fun pollInterrupts() = (if (apu.irq || cartridge.irq) INTERRUPT_IRQ else 0) or (if (ppu.vbl) INTERRUPT_NMI else 0)

  private fun maybeIntercept(memory: Memory) = if (onStore != null) {
    val onStore = onStore
    object : Memory {
      override fun get(addr: Address) = memory[addr]
      override fun set(addr: Address, data: Data) {
        memory[addr] = data
        onStore(addr, data)
      }
    }
  } else {
    memory
  }

  inner class Diagnostics internal constructor() {
    val cpu = this@Nes.cpu.diagnostics
    val ppu = this@Nes.ppu.diagnostics
    fun step() = this@Nes.step()
    fun peek(addr: Address) = cpuMapper[addr]
    fun peekV(addr: Address) = cartridge.chr[addr]
  }

  val persistentRam = cartridge.persistentRam

  val diagnostics = Diagnostics()

  // TODO - consolidate all the constants
  companion object {
    const val CPU_RAM_SIZE = 2048

    const val ADDR_OAMDATA: Address = 0x2004
    const val ADDR_OAMDMA: Address = 0x4014
    const val ADDR_APU_STATUS: Address = 0x4015
    const val ADDR_JOYPADS: Address = 0x4016
    const val ADDR_JOYPAD1: Address = 0x4016
    const val ADDR_JOYPAD2: Address = 0x4017
  }
}
