package choliver.nespot

import org.khronos.webgl.set

class JsAudioPlayer {
  private val ctx = AudioContext()
  private var base = ctx.currentTime

  val sampleRateHz = ctx.sampleRate.toInt()

  val sink = object : AudioSink {
    override fun put(sample: Float) {}
  }

  private fun handleAudioBufferReady(buffer: FloatArray) {
    val target = ctx.createBuffer(1, buffer.size, ctx.sampleRate)
    val samples = target.getChannelData(0)
    buffer.forEachIndexed { idx, sample -> samples[idx] = 0.0f /*sample*/ }

    val source = ctx.createBufferSource()
    source.buffer = target
    source.connect(ctx.destination)
    source.start(base)

//    println("Lag: ${((base - audioCtx.currentTime) * 1000).toInt()}")

    base += buffer.size / ctx.sampleRate
  }

}
