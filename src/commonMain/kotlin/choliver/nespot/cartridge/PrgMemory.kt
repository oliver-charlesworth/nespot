package choliver.nespot.cartridge

import choliver.nespot.*

class PrgMemory(
  private val raw: ByteArray,
  private val bankSize: Int = raw.size,
  private val onSet: (addr: Address, data: Data) -> Unit = { _, _ -> }
) : Memory {
  private val numBanks = PRG_ROM_SIZE / bankSize
  private val pageMap = IntArray(numBanks) { it * bankSize }
  val bankMap = BankSetter()
  val ram = ByteArray(PRG_RAM_SIZE)

  inner class BankSetter {
    operator fun set(output: Int, underlying: Int) {
      pageMap[output] = underlying * bankSize
    }
  }

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

  private fun map(addr: Address) = (addr % bankSize) + pageMap[addr % PRG_ROM_SIZE / bankSize]
}
