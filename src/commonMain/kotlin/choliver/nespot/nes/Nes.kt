package choliver.nespot.nes

import choliver.nespot.CPU_FREQ_HZ
import choliver.nespot.RAM_SIZE
import choliver.nespot.apu.Apu
import choliver.nespot.cartridge.Cartridge
import choliver.nespot.cartridge.Rom
import choliver.nespot.common.Address
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
  intercept: (Memory) -> Memory = { it }
) {
  private var steps = 0
  private var extraCycles = 0

  private val cartridge = Cartridge.create(rom, getStepCount = { steps })

  private val apu = Apu(
    cpuFreqHz = CPU_FREQ_HZ,
    memory = cartridge.prg,  // DMC can only read from PRG space
    audioSink = audioSink
  )

  private val cpuRam = Ram(RAM_SIZE)

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
    joypads = joypads,
    addExtraCycles = { extraCycles += it }
  )

  private val cpu = Cpu(
    intercept(cpuMapper),
    pollInterrupts = ::pollInterrupts
  )

  fun step(): Int {
    steps++
    extraCycles = 0
    val cycles = cpu.executeStep()
    val totalCycles = cycles + extraCycles
    apu.advance(totalCycles)
    ppu.advance(totalCycles)
    return totalCycles
  }

  private fun pollInterrupts() = (if (apu.irq || cartridge.irq) INTERRUPT_IRQ else 0) or (if (ppu.vbl) INTERRUPT_NMI else 0)

  inner class Diagnostics internal constructor() {
    val cpu = this@Nes.cpu.diagnostics
    val ppu = this@Nes.ppu.diagnostics
    fun step() = this@Nes.step()
    fun peek(addr: Address) = cpuMapper[addr]
    fun peekV(addr: Address) = cartridge.chr[addr]
  }

  val persistentRam = cartridge.persistentRam

  val diagnostics = Diagnostics()
}
