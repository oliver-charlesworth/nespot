package choliver.nespot.sixfiveohtwo.model

import choliver.nespot.MutableForPerfReasons

@MutableForPerfReasons
data class State(
  var regs: Regs = Regs(),
  var prevNmi: Boolean = false
)
