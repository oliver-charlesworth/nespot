package choliver.nespot.apu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

// See https://wiki.nesdev.com/w/index.php/APU_Mixer
internal class Mixer(
  sampleRateHz: Int,
  private val channels: Channels
) {
  private val alpha: Float
  private var state: Float = 0.0f
  private var dc = 0f

  init {
    val omega = 2 * PI * 14e3 / sampleRateHz
    alpha = (cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)).toFloat()
  }

  fun sample() = cutDc(cutHf(mix()))

  private fun mix(): Float {
    val pulseSum = 0 +
      channels.sq1.output +
      channels.sq2.output

    val otherSum = 0 +
      channels.tri.output +
      channels.noi.output +
      channels.dmc.output

    val pulseOut = if (pulseSum == 0) 0.0f else {
      95.88f / ((8128.0f / pulseSum) + 100.0f)
    }
    val otherOut = if (otherSum == 0) 0.0f else {
      159.79f / ((1.0f / ((channels.tri.output / 8227.0f) + (channels.noi.output / 12241.0f) + (channels.dmc.output / 22638.0f))) + 100.0f)
    }
    return pulseOut + otherOut
  }

  // TODO - validate this filter
  private fun cutHf(sample: Float) = (alpha * sample + (1 - alpha) * state)
    .also { state = it }

  private fun cutDc(sample: Float) = (sample - dc)
    .also { dc = (DC_ALPHA * dc) + (1 - DC_ALPHA) * sample }

  companion object {
    private const val DC_ALPHA = 0.995f
  }
}
