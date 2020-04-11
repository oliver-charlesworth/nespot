package choliver.nes.cartridge

import choliver.sixfiveohtwo.model.UInt16
import choliver.sixfiveohtwo.model.UInt8

interface ChrMemory {
  fun load(addr: UInt16): ChrLoadResult
  fun store(addr: UInt16, data: UInt8): ChrStoreResult

  sealed class ChrLoadResult {
    data class Data(val data: UInt8) : ChrLoadResult()
    data class VramAddr(val addr: UInt16) : ChrLoadResult()
  }

  sealed class ChrStoreResult {
    object None : ChrStoreResult()
    data class VramAddr(val addr: UInt16) : ChrStoreResult()
  }
}
