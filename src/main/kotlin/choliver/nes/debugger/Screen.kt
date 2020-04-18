package choliver.nes.debugger

import choliver.nes.ppu.SCREEN_HEIGHT
import choliver.nes.ppu.SCREEN_WIDTH
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.stage.Stage
import java.nio.ByteBuffer
import java.nio.IntBuffer


class Screen(private val onClose: () -> Unit = {}) {
  companion object {
    private const val SCALE = 4.0
  }

  private var isStarted = false
  private lateinit var stage: Stage
  private val _buffer = ByteBuffer.allocateDirect(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
  val buffer: IntBuffer = _buffer.asIntBuffer()
  private val pixelBuffer = PixelBuffer(
    SCREEN_WIDTH,
    SCREEN_HEIGHT,
    _buffer,
    PixelFormat.getByteBgraPreInstance() // Mac native format
  )

  fun redraw() {
    if (isStarted) {
      Platform.runLater { pixelBuffer.updateBuffer { null } }
    }
  }

  fun show() {
    if (!isStarted) {
      start()
    }
    Platform.runLater {
      stage.show()
      stage.toFront()
    }
  }

  fun hide() {
    if (isStarted) {
      Platform.runLater { stage.hide() }
    }
  }

  private fun start() {
    Platform.setImplicitExit(false)
    Platform.startup {
      stage = Stage()
      stage.title = "Wat"
      stage.scene = Scene(Group().apply {
        children.add(ImageView(WritableImage(pixelBuffer)).apply {
          fitWidth = SCREEN_WIDTH * SCALE
          fitHeight = SCREEN_HEIGHT * SCALE
        })
      })
      stage.isResizable = false
      stage.setOnCloseRequest {
        it.consume();
        hide()
        onClose()
      }
    }
    isStarted = true
  }
}
