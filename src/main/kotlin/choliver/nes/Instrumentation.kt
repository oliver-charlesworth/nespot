package choliver.nes

import choliver.nes.sixfiveohtwo.model.ProgramCounter

class Yeah(
  private val onReset: () -> Unit,
  private val onNmi: () -> Unit,
  private val onIrq: () -> Unit
) : Nes.Wat {
  private val _stores = mutableListOf<Pair<Address, Data>>()

  override fun onReset() = onReset.invoke()

  override fun onNmi() = onNmi.invoke()

  override fun onIrq() = onIrq.invoke()

  override fun onStore(addr: Address, data: Data) {
    _stores += (addr to data)
  }

  fun clearStores() {
    _stores.clear()
  }

  val stores = _stores.toList()
}

class Instrumentation(
  private val hooks: Nes.Hooks,
  private val yeah: Yeah
) {
  fun fireReset() = hooks.fireReset()

  fun fireNmi()  = hooks.fireNmi()

  fun fireIrq() = hooks.fireNmi()

  fun step(): List<Pair<Address, Data>> {
    yeah.clearStores()
    hooks.step()
    return yeah.stores
  }

  fun peek(addr: Address) = hooks.peek(addr)
  fun peekV(addr: Address) = hooks.peekV(addr)

  val state get() = hooks.state

  fun decodeAt(pc: ProgramCounter) = hooks.decodeAt(pc)
}
