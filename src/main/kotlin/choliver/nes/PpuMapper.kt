package choliver.nes

class PpuMapper(
  private val ram: Memory
) : Memory {
  // TODO
  override fun load(addr: Address) = when {
    addr in 0x2000..0x3FFF -> ram.load(addr % 2048)
    else -> 0x00
  }

  // TODO
  override fun store(addr: Address, data: Data) = when {
    addr in 0x2000..0x3FFF -> ram.store(addr % 2048, data)
    else -> {}
  }
}
