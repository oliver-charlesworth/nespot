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
import choliver.nespot.sixfiveohtwo.utils._0
import java.nio.IntBuffer

class Nes(
  rom: Rom,
  videoBuffer: IntBuffer,
  audioBuffer: FloatArray,
  joypads: Joypads,
  private val onStore: (Address, Data) -> Unit = { _: Address, _: Data -> }
) {
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
    pollReset = { _0 },
    pollIrq = { apu.irq || cartridge.irq },
    pollNmi = ppu::vbl
  )

  private val sequencer = Sequencer(cpu, apu, ppu)

  fun runToEndOfFrame() {
    sequencer.runToEndOfFrame()
  }

  inner class Diagnostics internal constructor() {
    val sequencer = this@Nes.sequencer.diagnostics
    val cpu = this@Nes.cpu.diagnostics
    val ppu = this@Nes.ppu.diagnostics
    val ram = this@Nes.cpuRam
    val vram = this@Nes.ppuRam
    fun peek(addr: Address) = cpuMapper[addr]
    fun peekV(addr: Address) = ppuMapper[addr]
  }

  val prgRam = cartridge.prgRam

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
