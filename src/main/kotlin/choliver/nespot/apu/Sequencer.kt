package choliver.nespot.apu

class Sequencer {
  data class Ticks(
    val quarter: Int,
    val half: Int
  )

  private val counter = Counter(FRAME_SEQUENCER_PERIOD_CYCLES / 4.0)
  private var iSeq = 0

  fun update(): Ticks {
    val ticks = counter.update()
    return if (ticks == 1) {
      iSeq = (iSeq + 1) % 5
      when (iSeq) {
        0 -> Ticks(quarter = 1, half = 1)
        1 -> Ticks(quarter = 1, half = 0)
        2 -> Ticks(quarter = 1, half = 1)
        3 -> Ticks(quarter = 1, half = 0)
        4 -> Ticks(quarter = 0, half = 0)
        else -> throw IllegalStateException() // Should never happen
      }
    } else Ticks(quarter = 0, half = 0)


  }


}
