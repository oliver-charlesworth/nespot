package choliver.nespot.cartridge

import choliver.nespot.*

class PrgMemory(
  private val raw: ByteArray,
  private val bankSize: Int = raw.size,
  private val onSet: (addr: Address, data: Data) -> Unit = { _, _ -> }
) : Memory {
  val ram = ByteArray(PRG_RAM_SIZE)
  val bankMap = IntArray(PRG_ROM_SIZE / bankSize) { it }

  override fun get(addr: Address) = when {
    (addr >= BASE_PRG_ROM) -> raw[map(addr)]
    else -> ram[addr % PRG_RAM_SIZE]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_PRG_ROM) -> onSet(addr, data)
      else -> ram[addr % PRG_RAM_SIZE] = data.toByte()
    }
  }

  private fun map(addr: Address) = (addr % bankSize) + bankMap[addr % PRG_ROM_SIZE / bankSize] * bankSize
}
