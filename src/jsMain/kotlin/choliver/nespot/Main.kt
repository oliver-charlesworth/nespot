package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint16Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.browser.document
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min


val canvas = document.getElementById("target") as HTMLCanvasElement

fun main() {
  console.log("Hello world")

  canvas.width = 256
  canvas.height = 240
  // TODO - stretch, etc.
  val scaleX = window.innerWidth / canvas.width
  val scaleY = window.innerHeight / canvas.height
  val scaleToFit = min(scaleX, scaleY)
  canvas.style.transformOrigin = "0 0" //scale from top left
  canvas.style.transform = "scale(${scaleToFit})"

  window.fetch("/smb3.nes").then { response ->
    response.arrayBuffer().then { buffer ->
      val b2 = Int8Array(buffer)
      val array = ByteArray(buffer.byteLength) { b2[it] }
      val ggg = GoGoGo(Rom.parse(array))
      ggg.start()
    }
  }
}

class GoGoGo(rom: Rom) {
  private val list = mutableListOf<IntArray>()
  private val joypads = Joypads()
  private val nes = Nes(
    rom = rom,
    joypads = joypads,
    onVideoBufferReady = { list += it }
  )

  fun start() {
    window.requestAnimationFrame { executeFrame() }
  }

  private fun executeFrame() {
    while (list.isEmpty()) {
      nes.step()
    }
    updateImage(list.removeAt(0))
    window.requestAnimationFrame { executeFrame() }
  }

  private fun updateImage(buffer: IntArray) {
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val img = ctx.getImageData(0.0, 0.0, 256.0, 240.0)
    val data = img.data.unsafeCast<Uint16Array>()

    var i = 0
    for (y in 0 until 240) {
      for (x in 0 until 256) {
        val pixel = buffer[i]
        data[i * 4 + 0] = ((pixel shr 8) and 0xFF).toShort()
        data[i * 4 + 1] = ((pixel shr 16) and 0xFF).toShort()
        data[i * 4 + 2] = ((pixel shr 24) and 0xFF).toShort()
        data[i * 4 + 3] = 255
        i++
      }
    }

    ctx.putImageData(img, 0.0, 0.0)
  }
}
