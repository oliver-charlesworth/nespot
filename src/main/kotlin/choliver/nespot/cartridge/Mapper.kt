package choliver.nespot.cartridge

import choliver.nespot.Memory

interface Mapper {
  val prg: Memory
  fun chr(vram: Memory): Memory
}
