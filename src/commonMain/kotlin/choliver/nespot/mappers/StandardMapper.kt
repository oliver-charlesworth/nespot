package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Ram
import choliver.nespot.cartridge.ChrMemory
import choliver.nespot.cartridge.Mapper
import choliver.nespot.cartridge.PrgMemory

class StandardMapper(
  stuff: Stuff
) : Mapper {
  override val irq = false
  override val persistentRam: Ram? = null

  override val prg = with(stuff) {
    PrgMemory(
      raw = prgData,
      bankSize = prgBankSize,
      onSet = { addr, data -> this@StandardMapper.onPrgSet(addr, data) }  // TODO - this might be expensive
    )
  }

  override val chr = ChrMemory(
    raw = stuff.chrData,
    bankSize = stuff.chrBankSize
  )

  init {
    with(stuff) {
      onStartup()
    }
  }

  interface Stuff {
    val prgData: ByteArray
    val chrData: ByteArray
    val prgBankSize: Int get() = prgData.size
    val chrBankSize: Int get() = chrData.size
    fun StandardMapper.onStartup() {}
    fun StandardMapper.onPrgSet(addr: Address, data: Data) {}
  }
}
