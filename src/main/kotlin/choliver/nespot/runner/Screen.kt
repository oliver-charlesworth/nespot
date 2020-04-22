package choliver.nespot.runner

import choliver.nespot.Joypads.Button
import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import javafx.application.Platform
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import java.nio.ByteBuffer
import java.nio.IntBuffer


class Screen(
  private val title: String = "NESpot",
  private val onButtonDown: (Button) -> Unit = {},
  private val onButtonUp: (Button) -> Unit = {},
  private val onClose: () -> Unit = {}
) {
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

  fun exit() {
    Platform.exit()
  }

  private fun start() {
    Platform.setImplicitExit(false)
    Platform.startup {
      stage = Stage()
      stage.title = title
      stage.scene = Scene(Group().apply {
        children.add(ImageView(WritableImage(pixelBuffer)).apply {
          fitWidth = SCREEN_WIDTH * SCALE
          fitHeight = SCREEN_HEIGHT * SCALE
        })
      })
      stage.scene.addEventFilter(KeyEvent.KEY_PRESSED) {codeToButton(it)?.let(onButtonDown) }
      stage.scene.addEventFilter(KeyEvent.KEY_RELEASED) { codeToButton(it)?.let(onButtonUp) }
      stage.isResizable = false
      stage.setOnCloseRequest {
        it.consume()
        hide()
        onClose()
      }
    }
    isStarted = true
  }

  private fun codeToButton(it: KeyEvent) = when (it.code) {
    KeyCode.Z -> Button.A
    KeyCode.X -> Button.B
    KeyCode.CLOSE_BRACKET -> Button.START
    KeyCode.OPEN_BRACKET -> Button.SELECT
    KeyCode.LEFT -> Button.LEFT
    KeyCode.RIGHT -> Button.RIGHT
    KeyCode.UP -> Button.UP
    KeyCode.DOWN -> Button.DOWN
    else -> null
  }
}
