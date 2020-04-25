package choliver.nespot.apu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

// TODO - non-linear mixing (used by SMB to set triangle/noise level)
internal class Mixer(
  private val sequencer: Sequencer,
  private val synths: List<SynthContext<*>>
) {
  private val alpha: Double
  private var state: Double = 0.0

  init {
    val omega = 2 * PI * 14e3 / SAMPLE_RATE_HZ
    alpha = cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)
  }

  fun take(): Int {
    val ticks = sequencer.take()
    val mixed = synths.sumByDouble { it.take(ticks) }

    // TODO - validate this filter
    val filtered = alpha * mixed + (1 - alpha) * state
    state = filtered
    return (filtered * 100).toInt()
  }
}
