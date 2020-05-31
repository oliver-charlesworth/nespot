package choliver.nespot.cartridge

import choliver.nespot.Address

class BankSetter(
  bankSize: Int,
  addressSpaceSize: Int
) {
  init {
    if ((bankSize % PAGE_SIZE) != 0) {
      throw IllegalArgumentException("Bank size ${bankSize} not a multiple of page size ${PAGE_SIZE}")
    }
  }

  private val pagesPerBank = bankSize / PAGE_SIZE
  private val pageMap = IntArray(addressSpaceSize / PAGE_SIZE) { it * PAGE_SIZE }

  operator fun set(idx: Int, underlying: Int) {
    for (i in 0 until pagesPerBank) {
      pageMap[idx * pagesPerBank + i] = (underlying * pagesPerBank + i) * PAGE_SIZE
    }
  }

  internal fun map(addr: Address) = (addr % PAGE_SIZE) + pageMap[addr / PAGE_SIZE]

  companion object {
    private const val PAGE_SIZE = 1024    // This should be no bigger than the minimum known bank size
  }
}
