package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

class ChrMemory(
  private val raw: ByteArray,
  bankSize: Int = raw.size,
  var mirroring: Mirroring = IGNORED
) : Memory {
  val bankMap = BankMap(bankSize = bankSize, addressSpaceSize = CHR_SIZE)
  private val vram = ByteArray(VRAM_SIZE)

  override fun get(addr: Address) = when {
    (addr >= BASE_VRAM) -> vram[vmap(addr)]    // This maps everything >= 0x4000 too
    else -> raw[bankMap.map(addr)]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_VRAM) -> vram[vmap(addr)] = data.toByte()   // This maps everything >= 0x4000 too
      else -> raw[bankMap.map(addr)] = data.toByte()
    }
  }

  private fun vmap(addr: Address) = when (mirroring) {
    FIXED_LOWER -> (addr % NAMETABLE_SIZE)
    FIXED_UPPER -> (addr % NAMETABLE_SIZE) + NAMETABLE_SIZE
    VERTICAL -> addr and 2047
    HORIZONTAL -> (addr and 1023) or ((addr and 2048) shr 1)
    IGNORED -> throw UnsupportedOperationException()
  }
}
