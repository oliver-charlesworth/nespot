package choliver.nespot.cartridge

import choliver.nespot.Memory

interface ChrMemory {
  /** Return a [Memory] instance that intercepts load/store and maps to RAM vs. cartridge stuff. */
  fun intercept(ram: Memory): Memory
}
