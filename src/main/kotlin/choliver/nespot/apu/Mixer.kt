package choliver.nespot.apu

import choliver.nespot.SAMPLE_RATE_HZ
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

// TODO - non-linear mixing (used by SMB to set triangle/noise level)
// See https://wiki.nesdev.com/w/index.php/APU_Mixer
internal class Mixer(
  private val sequencer: FrameSequencer,
  private val channels: Channels
) {
  private val alpha: Float
  private var state: Float = 0.0f

  init {
    val omega = 2 * PI * 14e3 / SAMPLE_RATE_HZ
    alpha = (cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)).toFloat()
  }

  fun take(): Float {
    val ticks = sequencer.take()

    val pulse1Out = channels.sq1.take(ticks)
    val pulse2Out = channels.sq2.take(ticks)
    val triangleOut = channels.tri.take(ticks)
    val noiseOut = channels.noi.take(ticks)
    val dmcOut = channels.dmc.take(ticks)

    val pulseSum = pulse1Out + pulse2Out
    val otherSum = triangleOut + noiseOut + dmcOut

    val pulseOut = if (pulseSum == 0) 0.0f else {
      95.88f / ((8128.0f / pulseSum) + 100.0f)
    }
    val otherOut = if (otherSum == 0) 0.0f else {
      159.79f / ((1.0f / ((triangleOut / 8227.0f) + (noiseOut / 12241.0f) + (dmcOut / 22638.0f))) + 100.0f)
    }
    val mixed = pulseOut + otherOut

    // TODO - validate this filter
    val filtered = alpha * mixed + (1 - alpha) * state
    state = filtered
    return filtered
  }
}
