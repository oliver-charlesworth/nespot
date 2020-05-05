package choliver.nespot.apu

import kotlin.math.max

class LengthCounter {
  private var _remaining = 0
  private var _length = 0

  val remaining
    get() = _remaining

  var length
    get() = _length
    set(value) {
      if (enabled) {
        _length = value
        _remaining = value
      }
    }

  var enabled = false
    set(value) {
      field = value
      _length = 0
      _remaining = 0
    }

  fun decrement() {
    _remaining = max(_remaining - 1, 0)
  }

  fun restart() {
    _remaining = _length
  }
}
