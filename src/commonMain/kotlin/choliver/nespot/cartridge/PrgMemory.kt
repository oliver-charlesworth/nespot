package choliver.nespot.cartridge

import choliver.nespot.*

class PrgMemory(
  private val raw: ByteArray,
  bankSize: Int = raw.size,
  private val onSet: (addr: Address, data: Data) -> Unit = { _, _ -> }
) : Memory {
  init {
    if ((bankSize % PAGE_SIZE) != 0) {
      throw IllegalArgumentException("Bank size ${bankSize} not a multiple of page size ${PAGE_SIZE}")
    }
  }

  // We map at page granularity, rather than bank granularity
  private val pagesPerBank = bankSize / PAGE_SIZE
  private val pageMap = IntArray(NUM_PAGES) { it * PAGE_SIZE }
  val bankMap = BankSetter()
  val ram = ByteArray(PRG_RAM_SIZE)

  inner class BankSetter {
    operator fun set(idx: Int, underlying: Int) {
      for (i in 0 until pagesPerBank) {
        pageMap[idx * pagesPerBank + i] = (underlying * pagesPerBank + i) * PAGE_SIZE
      }
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

  private fun map(addr: Address) = (addr % PAGE_SIZE) + pageMap[addr / PAGE_SIZE % NUM_PAGES]

  companion object {
    private const val PAGE_SIZE = 1024    // This should be no bigger than the minimum known bank size
    private const val NUM_PAGES = PRG_ROM_SIZE / PAGE_SIZE
  }
}
