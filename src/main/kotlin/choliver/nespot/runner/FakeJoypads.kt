package choliver.nespot.runner

import choliver.nespot.Data
import choliver.nespot.nes.Joypads
import choliver.nespot.data
import choliver.nespot.isBitSet

class FakeJoypads : Joypads {
  private val status = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private val statusLatched = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private var transparent = false

  override fun write(data: Data) {
    maybeLatch()
    transparent = data.isBitSet(0)
  }

  override fun read1() = read(1)

  override fun read2() = read(2)

  private fun read(which: Int): Data {
    maybeLatch()
    return (statusLatched[which]!! and 1)
      .also { statusLatched[which] = statusLatched[which]!! shr 1 }
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
  }

  private fun maybeLatch() {
    if (transparent) {
      statusLatched.clear()
      statusLatched += status
    }
  }
}
