package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data

interface ChrMemory {
  fun load(addr: Address): ChrLoadResult
  fun store(addr: Address, data: Data): ChrStoreResult

  sealed class ChrLoadResult {
    data class Data(val data: choliver.nespot.Data) : ChrLoadResult()
    data class VramAddr(val addr: Address) : ChrLoadResult()
  }

  sealed class ChrStoreResult {
    object None : ChrStoreResult()
    data class VramAddr(val addr: Address) : ChrStoreResult()
  }
}
