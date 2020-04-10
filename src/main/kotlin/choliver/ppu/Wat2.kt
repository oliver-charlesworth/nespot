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


class Wat2 : Application() {
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

    for (y in 0 until CANVAS_HEIGHT) {

      // TODO (33 times)
      // - Read nametable entry
      // - Read attribute table entry
      // - Read low/high bytes from pattern table
      // - Combine into palette indices
      // - Read color


      // TODO - account for fine offset (requires one extra tile)
      for (xT in 0 until CANVAS_WIDTH / TILE_SIZE) {
        // - Read nametable entry
        // - Read attribute table entry
        // - Read low/high bytes from pattern table

        for (xP in 0 until 8) {
          // - Combine into palette indices
          // - Read color
        }
      }


      for (x in 0 until CANVAS_WIDTH) {
        val idx = (x / (CANVAS_WIDTH / 16)) + (y / (CANVAS_HEIGHT / 4)) * 16
        val color = COLORS[idx]

        data.putInt(color)
      }
    }
    data.position(0)


    pixelBuffer.updateBuffer { null }
    val imageView = ImageView(img)
    imageView.fitWidth = CANVAS_WIDTH * 2.0
    imageView.fitHeight = CANVAS_HEIGHT * 2.0

    root.children.add(imageView)
    return root
  }
}
