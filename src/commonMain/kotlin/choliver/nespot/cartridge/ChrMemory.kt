package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

class ChrMemory(
  private val raw: ByteArray,
  private val bankSize: Int = raw.size,
  var mirroring: Mirroring = IGNORED
) : Memory {
  init {
    if ((bankSize % PAGE_SIZE) != 0) {
      throw IllegalArgumentException("Bank size ${bankSize} not a multiple of page size ${PAGE_SIZE}")
    }
  }

  // We map at page granularity, rather than bank granularity
  private val pagesPerBank = bankSize / PAGE_SIZE
  private val pageMap = IntArray(NUM_PAGES) { it * PAGE_SIZE }
  val bankMap = IntArray(CHR_SIZE / bankSize) { it }
  private val vram = ByteArray(VRAM_SIZE)

  override fun get(addr: Address) = when {
    (addr >= BASE_VRAM) -> vram[vmap(addr)]    // This maps everything >= 0x4000 too
    else -> raw[map(addr)]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_VRAM) -> vram[vmap(addr)] = data.toByte()   // This maps everything >= 0x4000 too
      else -> raw[map(addr)] = data.toByte()
    }
  }

  private fun map(addr: Address) = (addr % bankSize) + bankMap[addr / bankSize] * bankSize

  private fun vmap(addr: Address) = when (mirroring) {
    FIXED_LOWER -> (addr % NAMETABLE_SIZE)
    FIXED_UPPER -> (addr % NAMETABLE_SIZE) + NAMETABLE_SIZE
    VERTICAL -> addr and 2047
    HORIZONTAL -> (addr and 1023) or ((addr and 2048) shr 1)
    IGNORED -> throw UnsupportedOperationException()
  }

  companion object {
    private const val PAGE_SIZE = 1024    // This should be no bigger than the minimum known bank size
    private const val NUM_PAGES = CHR_SIZE / PAGE_SIZE
  }
}
