package choliver.ppu

import javafx.application.Application
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.stage.Stage
import java.nio.ByteBuffer


class Wat2 : Application() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = launch(Wat2::class.java, *args)
  }

  override fun start(primaryStage: Stage) {
    primaryStage.title = "PixelWriter Test"
    primaryStage.scene = Scene(drawImageData())
    primaryStage.show()
  }

  private fun drawImageData(): Group {
    val root = Group()
    val data = ByteBuffer.allocateDirect(CANVAS_WIDTH * CANVAS_HEIGHT * 4)
    val pixelFormat = PixelFormat.getByteBgraPreInstance() // Mac native format
    val pixelBuffer = PixelBuffer(CANVAS_WIDTH, CANVAS_HEIGHT, data, pixelFormat)
    val img = WritableImage(pixelBuffer)

    for (y in 0 until CANVAS_HEIGHT) {
      for (x in 0 until CANVAS_WIDTH) {
        val r = y * 255 / CANVAS_HEIGHT
        val g = x * 255 / CANVAS_WIDTH

        data.put(0.toByte())
        data.put(g.toByte())
        data.put(r.toByte())
        data.put(255.toByte())
      }
    }
    data.position(0)


    pixelBuffer.updateBuffer { null }
    val imageView = ImageView(img)

    root.children.add(imageView)
    return root
  }
}
