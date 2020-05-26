package choliver.nespot.runner

import choliver.nespot.VISIBLE_HEIGHT
import choliver.nespot.VISIBLE_WIDTH
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import kotlin.browser.document

class BrowserScreen {
  val canvas = document.getElementById("target") as HTMLCanvasElement
  private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

  private var frame: Uint8ClampedArray? = null

  fun absorbFrame(frame: Uint8ClampedArray) {
    this.frame = frame
  }

  fun redraw() {
    frame?.let {
      ctx.putImageData(ImageData(it, VISIBLE_WIDTH, VISIBLE_HEIGHT), 0.0, 0.0)
    }
  }
}
