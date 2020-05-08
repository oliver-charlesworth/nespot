package choliver.nespot.cartridge

import choliver.nespot.Memory
import choliver.nespot.Ram

interface Mapper {
  val irq: Boolean
  val prgRam: Ram?
  val prg: Memory
  fun chr(vram: Memory): Memory
}
