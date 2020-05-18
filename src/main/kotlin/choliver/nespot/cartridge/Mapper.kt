package choliver.nespot.cartridge

import choliver.nespot.Memory
import choliver.nespot.Ram


/**
 * Loosely encapsulates the notion of a cartridge mapper.
 *
 * Takes on more responsibility for perf reasons - in particular it must implement the 2kB VRAM.
 */
interface Mapper {
  val irq: Boolean
  val persistentRam: Ram?
  val prg: Memory
  val chr: Memory
}
