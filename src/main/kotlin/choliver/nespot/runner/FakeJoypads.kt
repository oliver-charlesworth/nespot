package choliver.nespot.runner

import choliver.nespot.Data
import choliver.nespot.data
import choliver.nespot.isBitSet
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Joypads.Button
import java.util.concurrent.LinkedBlockingQueue

class FakeJoypads : Joypads {
  private data class Update(
    val which: Int,
    val button: Button,
    val state: Boolean
  )

  private val updates = LinkedBlockingQueue<Update>()
  private val status = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private val copied = mutableMapOf(1 to 0.data(), 2 to 0.data())
  private var transparent = false

  override fun write(data: Data) {
    consumeUpdates()
    transparent = data.isBitSet(0)
    maybeCopy()
  }

  override fun read1() = read(1)

  override fun read2() = read(2)

  private fun read(which: Int): Int {
    consumeUpdates()
    return (copied[which]!! and 1)
      .also {
        copied[which] = copied[which]!! shr 1
        maybeCopy()
      }
  }

  private fun consumeUpdates() {
    val myUpdates = mutableListOf<Update>()
    updates.drainTo(myUpdates)
    myUpdates.forEach {
      val shift = it.button.idx
      val mask = (1 shl shift)
      val s = status[it.which] ?: throw IllegalArgumentException()  // Should never happen
      status[it.which] = if (it.state) (s or mask) else (s and mask.inv())
    }
    maybeCopy()
  }

  private fun maybeCopy() {
    if (transparent) {
      copied.clear()
      copied += status
    }
  }

  fun up(which: Int, button: Button) {
    updates.add(Update(which, button, false))
  }

  fun down(which: Int, button: Button) {
    updates.add(Update(which, button, true))
  }
}
