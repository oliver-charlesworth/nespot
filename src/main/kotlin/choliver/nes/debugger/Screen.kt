package choliver.nes.debugger

import choliver.nes.ppu.CANVAS_HEIGHT
import choliver.nes.ppu.CANVAS_WIDTH
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.stage.Stage
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch


class Screen {
  companion object {
    const val SCALE = 4
  }

  private val latch = CountDownLatch(1)
  val buffer: ByteBuffer = ByteBuffer.allocateDirect(CANVAS_WIDTH * CANVAS_HEIGHT * 4 * SCALE * SCALE)
  private val pixelBuffer = PixelBuffer(
    CANVAS_WIDTH * SCALE,
    CANVAS_HEIGHT * SCALE,
    buffer,
    PixelFormat.getByteBgraPreInstance() // Mac native format
  )

  fun redraw() {
    buffer.position(0)
    Platform.runLater { pixelBuffer.updateBuffer { null } }
  }

  fun start() {
    Platform.startup {
      val stage = Stage()
      stage.title = "Wat"
      stage.scene = Scene(Group().apply {
        children.add(ImageView(WritableImage(pixelBuffer)))
      })
      stage.isResizable = false
      stage.setOnCloseRequest { latch.countDown() }
      stage.show()
    }
  }

  fun await() {
    latch.await()
    Platform.exit()
  }
}
