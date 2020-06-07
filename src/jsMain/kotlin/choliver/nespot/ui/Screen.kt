package choliver.nespot.ui

import choliver.nespot.VISIBLE_HEIGHT
import choliver.nespot.VISIBLE_WIDTH
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import kotlin.browser.document
import kotlin.browser.window

class Screen {
  private val canvas = document.getElementById("target") as HTMLCanvasElement
  private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
  private var frame: Uint8ClampedArray? = null

  var fullScreen: Boolean = false
    set(value) {
      field = value
      if (value) {
        document.body!!.requestFullscreen()
      } else {
        document.exitFullscreen()
      }
    }

  init {
    window.onresize = { configureDom() }
    document.onfullscreenchange = { configureDom() }
    configureDom()
  }

  fun absorbFrame(frame: Uint8ClampedArray) {
    this.frame = frame
  }

  fun redraw() {
    frame?.let {
      ctx.putImageData(ImageData(it, VISIBLE_WIDTH, VISIBLE_HEIGHT), 0.0, 0.0)
    }
  }

  private fun configureDom() {
    with(document.body!!.style) {
      margin = "0"
      padding = "0"
      backgroundColor = "black"
    }

    val displayInfo = DisplayInfo(
      targetWidth = window.innerWidth.toDouble(),
      targetHeight = window.innerHeight.toDouble()
    )

    with(canvas) {
      width = displayInfo.sourceWidth.toInt()
      height = displayInfo.sourceHeight.toInt()
      with(style) {
        display = "block"
        marginLeft = "${displayInfo.marginHorizontal}px"
        marginRight = "${displayInfo.marginHorizontal}px"
        marginTop = "${displayInfo.marginVertical}px"
        marginBottom = "${displayInfo.marginVertical}px"
        padding = "0"
        transformOrigin = "0 0"
        transform = "scale(${displayInfo.scaleHorizontal}, ${displayInfo.scaleVertical})"
      }
    }
  }
}
