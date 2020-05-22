package choliver.nespot.runner

import choliver.nespot.Data
import choliver.nespot.data
import choliver.nespot.isBitSet
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Joypads.Button

class FakeJoypads : Joypads {
  private val status = mutableListOf(0.data(), 0.data())
  private val copied = mutableListOf(0.data(), 0.data())
  private var transparent = false

  override fun write(data: Data) {
    transparent = data.isBitSet(0)
    maybeCopy()
  }

  override fun read1() = read(1)

  override fun read2() = read(2)

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
