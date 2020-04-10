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
    private const val IMAGE_WIDTH = 100
    private const val IMAGE_HEIGHT = 100
    private const val PATCH_WIDTH = 10
    private const val PATCH_HEIGHT = 10

    @JvmStatic
    fun main(args: Array<String>) = launch(Wat2::class.java, *args)
  }

  override fun start(primaryStage: Stage) {
    primaryStage.title = "PixelWriter Test"
    primaryStage.scene = Scene(
      drawImageData(),
      400.0,
      400.0
    )
    primaryStage.show()
  }

  private fun drawImageData(): Group {
    val root = Group()
    val data = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 4)
    val pixelFormat = PixelFormat.getByteBgraPreInstance() // Mac native format
    val pixelBuffer = PixelBuffer(100, 100, data, pixelFormat)
    val img = WritableImage(pixelBuffer)

    for (y in 0 until IMAGE_HEIGHT) {
      for (x in 0 until IMAGE_WIDTH) {
        val r = (y % PATCH_HEIGHT) * 255 / PATCH_HEIGHT
        val g = (x % PATCH_WIDTH) * 255 / PATCH_WIDTH

        val on = ((x / PATCH_WIDTH) % 2 == 0) xor ((y / PATCH_HEIGHT) % 2 == 0)

        if (on) {
          data.put(0.toByte())
          data.put(g.toByte())
          data.put(r.toByte())
          data.put(255.toByte())
        } else {
          data.put(0.toByte())
          data.put(0.toByte())
          data.put(0.toByte())
          data.put(0.toByte())
        }
      }
    }
    data.position(0)


    pixelBuffer.updateBuffer { Rectangle2D(0.0, 0.0, IMAGE_WIDTH.toDouble(), IMAGE_HEIGHT.toDouble()) }
    val imageView = ImageView(img)

    root.children.add(imageView)
    return root
  }
}
