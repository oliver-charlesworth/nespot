package choliver.nespot.apu

class Divider(private val ratio: Int, private val counter: Counter) {
  private var i = 0

  // TODO - handle multiple ticks
  fun update(): Int {
    i += counter.take()
    return if (i == ratio) {
      i = 0
      1
    } else {
      0
    }
  }
}
