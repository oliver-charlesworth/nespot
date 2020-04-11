package choliver.ppu

import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.stage.Stage
import java.nio.ByteBuffer


abstract class Wat2 : Application() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) = launch(Wat2::class.java, *args)
  }

  override fun start(primaryStage: Stage) {
    primaryStage.title = "Wat"
    primaryStage.scene = Scene(drawImageData())
    primaryStage.isResizable = false
    primaryStage.show()
  }

  private fun drawImageData(): Group {
    val root = Group()
    val data = ByteBuffer.allocateDirect(CANVAS_WIDTH * CANVAS_HEIGHT * 4)
    val pixelFormat = PixelFormat.getByteBgraPreInstance() // Mac native format
    val pixelBuffer = PixelBuffer(CANVAS_WIDTH, CANVAS_HEIGHT, data, pixelFormat)
    val img = WritableImage(pixelBuffer)

    populateData(data)
    data.position(0)

    pixelBuffer.updateBuffer { null }
    val imageView = ImageView(img)
    imageView.fitWidth = CANVAS_WIDTH * 4.0
    imageView.fitHeight = CANVAS_HEIGHT * 4.0

    root.children.add(imageView)
    return root
  }

  abstract fun populateData(data: ByteBuffer)
}
