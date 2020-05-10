package choliver.nespot.cpu.model

import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class State(
  var regs: Regs = Regs(),
  var prevNmi: Boolean = false
)
