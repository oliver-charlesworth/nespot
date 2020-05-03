package choliver.nespot.cartridge

import choliver.nespot.Memory

interface Mapper {
  val irq: Boolean
  val prg: Memory
  fun chr(vram: Memory): Memory
}
