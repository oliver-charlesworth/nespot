package choliver.nespot

import choliver.nespot.cartridge.Rom
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.browser.window
import kotlin.math.PI
import kotlin.math.sin

private val ctx = AudioContext()
private var time = ctx.currentTime

fun main() {
  window.fetch("/smb3.nes").then { response ->
    response.arrayBuffer().then { buffer ->
      val b2 = Int8Array(buffer)
      val array = ByteArray(buffer.byteLength) { b2[it] }
      JsRunner(Rom.parse(array)).run()
    }
  }

//  ohyeah()
}

fun ohyeah() {
  val freqHz = 440.0f
  val lengthMs = 10
  val lookaheadMs = 30
  val length = (ctx.sampleRate * lengthMs / 1000.0).toInt()
  while (time - ctx.currentTime < (lookaheadMs / 1000.0)) {
    val buffer = ctx.createBuffer(1, length, ctx.sampleRate)
    val samples = buffer.getChannelData(0)
    for (i in 0 until length) {
      val t = time + i / ctx.sampleRate
      samples[i] = sin(2 * PI * freqHz * t).toFloat()
    }

    val source = ctx.createBufferSource()
    source.buffer = buffer
    source.connect(ctx.destination)
    source.onended = {
      ohyeah()
    }
    source.start(time)
    time += length / ctx.sampleRate
  }
}

