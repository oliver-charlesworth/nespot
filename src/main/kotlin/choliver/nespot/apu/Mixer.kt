package choliver.nespot.apu

import choliver.nespot.SAMPLE_RATE_HZ
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

// TODO - non-linear mixing (used by SMB to set triangle/noise level)
// See https://wiki.nesdev.com/w/index.php/APU_Mixer
internal class Mixer(
  private val sequencer: Sequencer,
  private val channels: Channels
) {
  private val alpha: Double
  private var state: Double = 0.0

  init {
    val omega = 2 * PI * 14e3 / SAMPLE_RATE_HZ
    alpha = cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)
  }

  fun take(): Int {
    val ticks = sequencer.take()

    val pulse1Out = channels.sq1.take(ticks)
    val pulse2Out = channels.sq2.take(ticks)
    val triangleOut = channels.tri.take(ticks)
    val noiseOut = channels.noi.take(ticks)
    val dmcOut = channels.dmc.take(ticks)

    val pulseSum = pulse1Out + pulse2Out
    val otherSum = triangleOut + noiseOut + dmcOut

    val pulseOut = if (pulseSum == 0) 0.0 else {
      95.88 / ((8128.0 / pulseSum) + 100.0)
    }
    val otherOut = if (otherSum == 0) 0.0 else {
      159.79 / ((1.0 / ((triangleOut / 8227.0) + (noiseOut / 12241.0) + (dmcOut / 22638.0))) + 100.0)
    }
    val mixed = pulseOut + otherOut

    // TODO - validate this filter
    val filtered = alpha * mixed + (1 - alpha) * state
    state = filtered
    return (filtered * 100).toInt()
  }
}
