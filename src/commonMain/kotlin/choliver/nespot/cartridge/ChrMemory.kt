package choliver.nespot.cartridge

import choliver.nespot.*
import choliver.nespot.cartridge.Rom.Mirroring
import choliver.nespot.cartridge.Rom.Mirroring.*

class ChrMemory(
  private val raw: ByteArray,
  bankSize: Int = raw.size
) : Memory {
  private val vram = ByteArray(VRAM_SIZE)

  val bankMap = BankMap(bankSize = bankSize, addressSpaceSize = CHR_SIZE)
  private val vbankMap = BankMap(
    bankSize = NAMETABLE_SIZE,
    addressSpaceSize = VRAM_SIZE * 4     // Whole space is mirrored
  )

  var mirroring: Mirroring = IGNORED
    set(value) {
      field = value
      for (base in 0 until 8 step 4) {
        when (value) {
          FIXED_LOWER -> {
            vbankMap[base + 0] = 0
            vbankMap[base + 1] = 0
            vbankMap[base + 2] = 0
            vbankMap[base + 3] = 0
          }
          FIXED_UPPER -> {
            vbankMap[base + 0] = 1
            vbankMap[base + 1] = 1
            vbankMap[base + 2] = 1
            vbankMap[base + 3] = 1
          }
          VERTICAL -> {
            vbankMap[base + 0] = 0
            vbankMap[base + 1] = 1
            vbankMap[base + 2] = 0
            vbankMap[base + 3] = 1
          }
          HORIZONTAL -> {
            vbankMap[base + 0] = 0
            vbankMap[base + 1] = 0
            vbankMap[base + 2] = 1
            vbankMap[base + 3] = 1
          }
          IGNORED -> Unit
        }
      }
    }

  override fun get(addr: Address) = when {
    (addr >= BASE_VRAM) -> vram[vbankMap.map(addr - BASE_VRAM)]
    else -> raw[bankMap.map(addr)]
  }.data()

  override fun set(addr: Address, data: Data) {
    when {
      (addr >= BASE_VRAM) -> vram[vbankMap.map(addr - BASE_VRAM)] = data.toByte()
      else -> raw[bankMap.map(addr)] = data.toByte()
    }
  }
}
