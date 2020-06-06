package choliver.nespot.cartridge

import choliver.nespot.common.Address
import choliver.nespot.common.Data

interface Mapper {
  val prgData: ByteArray
  val chrData: ByteArray
  val prgBankSize: Int get() = prgData.size
  val chrBankSize: Int get() = chrData.size
  val persistRam: Boolean get() = false
  val irq get() = false
  fun Cartridge.onStartup() {}
  fun Cartridge.onPrgSet(addr: Address, data: Data) {}
  fun Cartridge.onChrGet(addr: Address) {}
  fun Cartridge.onChrSet(addr: Address, data: Data) {}
}
