package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Ram

class StandardMapper(
  config: Config
) : Mapper {
  override val prg = with(config) {
    PrgMemory(
      raw = prgData,
      bankSize = prgBankSize,
      onSet = { addr, data -> this@StandardMapper.onPrgSet(addr, data) }  // TODO - this might be expensive
    )
  }

  override val chr = ChrMemory(
    raw = config.chrData,
    bankSize = config.chrBankSize
  )

  override val irq = false

  override val persistentRam = if (config.persistRam) {
    Ram.backedBy(prg.ram)
  } else null

  init {
    with(config) {
      onStartup()
    }
  }

  interface Config {
    val prgData: ByteArray
    val chrData: ByteArray
    val prgBankSize: Int get() = prgData.size
    val chrBankSize: Int get() = chrData.size
    val persistRam: Boolean get() = false
    val irq get() = false
    fun StandardMapper.onStartup() {}
    fun StandardMapper.onPrgSet(addr: Address, data: Data) {}
  }
}
