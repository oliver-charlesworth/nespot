package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.IGNORED

class ChrMemory(
  private val raw: ByteArray,
  private val bankSize: Int = raw.size,
  var mirroring: Mirroring = IGNORED
) : Memory {
  private val vram = ByteArray(VRAM_SIZE)
  val bankMap = IntArray(CHR_SIZE / bankSize) { it }

  override fun get(addr: Address) = when {
    (addr >= BASE_VRAM) -> vram[vramAddr(mirroring, addr)]    // This maps everything >= 0x4000 too
    else -> raw[map(addr)]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_VRAM) -> vram[vramAddr(mirroring, addr)] = data.toByte()   // This maps everything >= 0x4000 too
      else -> raw[map(addr)] = data.toByte()
    }
  }

  private fun map(addr: Address) = (addr % bankSize) + bankMap[addr / bankSize] * bankSize

  companion object {
    private const val CHR_SIZE = 8192
  }
}
