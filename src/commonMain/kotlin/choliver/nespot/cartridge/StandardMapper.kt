package choliver.nespot.cartridge

import choliver.nespot.Address
import choliver.nespot.Data
import choliver.nespot.Ram

class StandardMapper(
  private val config: Config
) : Mapper {
  // TODO - can we avoid the callback overhead if callbacks not set?
  override val prg = with(config) {
    PrgMemory(
      raw = prgData,
      bankSize = prgBankSize,
      onSet = { addr, data -> onPrgSet(addr, data) }
    )
  }

  // TODO - can we avoid the callback overhead if callbacks not set?
  override val chr = with(config) {
    ChrMemory(
      raw = chrData,
      bankSize = chrBankSize,
      onGet = { addr -> onChrGet(addr) },
      onSet = { addr, data -> onChrSet(addr, data) }
    )
  }

  override val irq get() = config.irq

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
    fun StandardMapper.onChrGet(addr: Address) {}
    fun StandardMapper.onChrSet(addr: Address, data: Data) {}
  }
}
