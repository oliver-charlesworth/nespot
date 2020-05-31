package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.IGNORED

class BoringChr(
  private val raw: ByteArray,
  private val bankSize: Int = raw.size,
  var mirroring: Mirroring = IGNORED
) : Memory {
  private val vram = ByteArray(VRAM_SIZE)
  val bankMap = IntArray(raw.size / bankSize) { it }

  override fun get(addr: Address) = when {
    (addr >= BASE_VRAM) -> vram[vramAddr(mirroring, addr)]    // This maps everything >= 0x4000 too
    else -> raw[chrAddr(addr)]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_VRAM) -> vram[vramAddr(mirroring, addr)] = data.toByte()   // This maps everything >= 0x4000 too
      else -> raw[chrAddr(addr)] = data.toByte()
    }
  }

  private fun chrAddr(addr: Address) = (addr % bankSize) + bankMap[addr / bankSize] * bankSize
}
