package choliver.nes

import choliver.nes.cartridge.ChrMemory
import choliver.nes.cartridge.ChrMemory.ChrLoadResult

class PpuMapper(
  private val chr: ChrMemory,
  private val ram: Memory
) : Memory {
  // TODO - this hardcoded mapping is wrong
  override fun load(addr: Address) = when {
    addr < 0x2000 -> (chr.load(addr) as ChrLoadResult.Data).data
    addr < 0x4000 -> ram.load(addr % 2048)
    else -> 0x00
  }

  // TODO - this hardcoded mapping is wrong
  override fun store(addr: Address, data: Data) = when {
    addr < 0x2000 -> { chr.store(addr, data); Unit }
    addr < 0x4000 -> ram.store(addr % 2048, data)
    else -> {}
  }
}
