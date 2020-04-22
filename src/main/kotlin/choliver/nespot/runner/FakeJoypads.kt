package choliver.nespot.runner

import choliver.nespot.Data
import choliver.nespot.nes.Joypads
import choliver.nespot.data
import choliver.nespot.isBitSet

class FakeJoypads : Joypads {
  private val status = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private val copied = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private var transparent = false

  override fun write(data: Data) {
    transparent = data.isBitSet(0)
    maybeCopy()
  }

  override fun read1() = read(1)

  override fun read2() = read(2)

  private fun read(which: Int) = (copied[which]!! and 1)
    .also {
      copied[which] = copied[which]!! shr 1
      maybeCopy()
    }

  fun up(which: Int, button: Joypads.Button) {
    update(which, button, false)
  }

  fun down(which: Int, button: Joypads.Button) {
    update(which, button, true)
  }

  private fun update(which: Int, button: Joypads.Button, state: Boolean) {
    val shift = button.idx
    val mask = (1 shl shift)
    val s = status[which] ?: throw IllegalArgumentException()  // Should never happen
    status[which] = if (state) (s or mask) else (s and mask.inv())
    maybeCopy()
  }

  private fun maybeCopy() {
    if (transparent) {
      copied.clear()
      copied += status
    }
  }
}
