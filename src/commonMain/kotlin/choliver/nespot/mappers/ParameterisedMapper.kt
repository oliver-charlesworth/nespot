package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Ram
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.PrgMemory

class ParameterisedMapper(
  prgData: ByteArray,
  chrData: ByteArray,
  prgBankSize: Int = prgData.size,
  chrBankSize: Int = chrData.size,
  onPrgSet: ParameterisedMapper.(Address, Data) -> Unit = { _, _ -> }
) : Mapper {
  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = PrgMemory(
    raw = prgData,
    bankSize = prgBankSize,
    onSet = { addr, data -> onPrgSet(this, addr, data) }
  )

  override val chr = ChrMemory(
    raw = chrData,
    bankSize = chrBankSize
  )
}
