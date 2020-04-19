package choliver.nes

import choliver.nes.cartridge.ChrMemory
import choliver.nes.cartridge.ChrMemory.ChrLoadResult
import choliver.nes.ppu.Ppu.Companion.BASE_NAMETABLES

class PpuMapper(
  private val chr: ChrMemory,
  private val ram: Memory
) : Memory {
  // TODO - this hardcoded mapping is wrong
  override fun load(addr: Address) = when {
    addr < BASE_NAMETABLES -> (chr.load(addr) as ChrLoadResult.Data).data
    addr < 0x4000 -> ram.load(addr % 2048)
    else -> 0x00
  }

  // TODO - this hardcoded mapping is wrong
  override fun store(addr: Address, data: Data) = when {
    addr < BASE_NAMETABLES -> { chr.store(addr, data); Unit }
    addr < 0x4000 -> ram.store(addr % 2048, data)
    else -> {}
  }
}
