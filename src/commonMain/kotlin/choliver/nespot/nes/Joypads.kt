package choliver.nespot.nes

import choliver.nespot.Data
import choliver.nespot.data
import choliver.nespot.isBitSet

class Joypads {
  // See http://wiki.nesdev.com/w/index.php/Standard_controller#Report
  @Suppress("unused")
  enum class Button(val idx: Int) {
    A(0),
    B(1),
    SELECT(2),
    START(3),
    UP(4),
    DOWN(5),
    LEFT(6),
    RIGHT(7)
  }

  private val status = mutableListOf(0.data(), 0.data())
  private val copied = mutableListOf(0.data(), 0.data())
  private var transparent = false

  fun write(data: Data) {
    transparent = data.isBitSet(0)
    maybeCopy()
  }

  fun read1() = read(1)

  fun read2() = read(2)

  private fun read(which: Int) = (copied[which - 1] and 1)
    .also {
      copied[which - 1] = copied[which - 1] shr 1
      maybeCopy()
    }

  fun up(which: Int, button: Button) {
    update(which, button, false)
  }

  fun down(which: Int, button: Button) {
    update(which, button, true)
  }

  private fun update(which: Int, button: Button, state: Boolean) {
    val shift = button.idx
    val mask = (1 shl shift)
    val s = status[which - 1]
    status[which - 1] = if (state) (s or mask) else (s and mask.inv())
    maybeCopy()
  }

  private fun maybeCopy() {
    if (transparent) {
      copied[0] = status[0]
      copied[1] = status[1]
    }
  }
}
