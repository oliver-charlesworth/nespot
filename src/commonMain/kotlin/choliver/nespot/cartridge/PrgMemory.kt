package choliver.nespot.cartridge

import choliver.nespot.*

// TODO - unify with RAM
class PrgMemory(
  private val raw: ByteArray,
  bankSize: Int = raw.size,
  private val onSet: (addr: Address, data: Data) -> Unit = { _, _ -> }
) : Memory {
  val bankMap = BankMap(bankSize = bankSize, addressSpaceSize = PRG_ROM_SIZE)
  val ram = ByteArray(PRG_RAM_SIZE)

  override fun get(addr: Address) = when {
    (addr >= BASE_PRG_ROM) -> raw[bankMap.map(addr - BASE_PRG_ROM)]
    else -> ram[addr % PRG_RAM_SIZE]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_PRG_ROM) -> onSet(addr, data)
      else -> ram[addr % PRG_RAM_SIZE] = data.toByte()
    }
  }
}
