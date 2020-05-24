package choliver.nespot.apu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt


// See https://wiki.nesdev.com/w/index.php/APU_Mixer
internal class Mixer(
  sampleRateHz: Int,
  private val sequencer: FrameSequencer,
  private val channels: Channels
) {
  private val alpha: Float
  private var state: Float = 0.0f

  init {
    val omega = 2 * PI * 14e3 / sampleRateHz
    alpha = (cos(omega) - 1 + sqrt(cos(omega) * cos(omega) - 4 * cos(omega) + 3)).toFloat()
  }

  fun take(): Float {
    val ticks = sequencer.take()

    val sq1 = channels.sq1.take(ticks)
    val sq2 = channels.sq2.take(ticks)
    val tri = channels.tri.take(ticks)
    val noi = channels.noi.take(ticks)
    val dmc = channels.dmc.take(ticks)

    val pulseSum = sq1 + sq2
    val otherSum = tri + noi + dmc

    val pulseOut = if (pulseSum == 0) 0.0f else {
      95.88f / ((8128.0f / pulseSum) + 100.0f)
    }
    val otherOut = if (otherSum == 0) 0.0f else {
      159.79f / ((1.0f / ((tri / 8227.0f) + (noi / 12241.0f) + (dmc / 22638.0f))) + 100.0f)
    }
    val mixed = pulseOut + otherOut

    // TODO - validate this filter
    val filtered = alpha * mixed + (1 - alpha) * state
    state = filtered
    return filtered
  }
}