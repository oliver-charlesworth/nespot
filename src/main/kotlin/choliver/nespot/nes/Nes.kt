package choliver.nespot.nes

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Memory
import choliver.nespot.Ram
import choliver.nespot.apu.Apu
import choliver.nespot.cartridge.Rom
import choliver.nespot.cartridge.createMapper
import choliver.nespot.cpu.Cpu
import choliver.nespot.cpu.utils._0
import choliver.nespot.ppu.Ppu
import java.nio.IntBuffer

class Nes(
  rom: Rom,
  joypads: Joypads,
  onAudioBufferReady: (FloatArray) -> Unit = {},
  onVideoBufferReady: (IntBuffer) -> Unit = {},
  private val onStore: (Address, Data) -> Unit = { _: Address, _: Data -> }
) {
  private val mapper = createMapper(rom)

  private val apu = Apu(
    memory = mapper.prg,  // DMC can only read from PRG space
    onAudioBufferReady = onAudioBufferReady
  )

  private val cpuRam = Ram(CPU_RAM_SIZE)
  private val ppuRam = Ram(PPU_RAM_SIZE)

  private val ppuMapper = mapper.chr(ppuRam)

  private val ppu = Ppu(
    memory = ppuMapper,
    onVideoBufferReady = onVideoBufferReady
  )

  private val cpuMapper = CpuMapper(
    prg = mapper.prg,
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
    pollIrq = { apu.irq || mapper.irq },
    pollNmi = ppu::vbl
  )

  fun step() {
    val cycles = cpu.executeStep()
    apu.advance(cycles)
    ppu.advance(cycles)
  }

  inner class Diagnostics internal constructor() {
    val cpu = this@Nes.cpu.diagnostics
    val ppu = this@Nes.ppu.diagnostics
    val ram = this@Nes.cpuRam
    val vram = this@Nes.ppuRam
    fun step() = this@Nes.step()
    fun peek(addr: Address) = cpuMapper[addr]
    fun peekV(addr: Address) = ppuMapper[addr]
  }

  val persistentRam = mapper.persistentRam

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
