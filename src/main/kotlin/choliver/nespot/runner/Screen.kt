package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.ppu.TILE_SIZE
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import java.nio.ByteBuffer
import java.nio.IntBuffer


class Screen(
  private val title: String = "NESpot",
  private var isFullScreen: Boolean = false,
  private val onButtonDown: (Button) -> Unit = {},
  private val onButtonUp: (Button) -> Unit = {},
  private val onClose: () -> Unit = {}
) {
  companion object {
    private const val SCALE = 4.0
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }

  private var isStarted = false
  private lateinit var stage: Stage
  private lateinit var imageView: ImageView
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
      initImageView()
      initStage()
      configureStageAndImage()
    }
    isStarted = true
  }

  private fun initImageView() {
    imageView = ImageView(WritableImage(pixelBuffer)).apply {
      // Crop top and bottom tile, per http://wiki.nesdev.com/w/index.php/Overscan
      viewport = Rectangle2D(
        0.0,
        TILE_SIZE.toDouble(),
        SCREEN_WIDTH.toDouble(),
        (SCREEN_HEIGHT - 2 * TILE_SIZE).toDouble()
      )
    }
  }

  private fun initStage() {
    val switchModeCombo = KeyCodeCombination(KeyCode.F, KeyCombination.META_DOWN, KeyCombination.SHIFT_DOWN)
    stage = Stage()
    stage.fullScreenExitKeyCombination = KeyCombination.NO_MATCH
    stage.title = title
    stage.scene = Scene(Group().apply { children.add(imageView) }, Color.BLACK)
    stage.scene.addEventFilter(KeyEvent.KEY_PRESSED) {
      if (switchModeCombo.match(it)) {
        isFullScreen = !isFullScreen
        configureStageAndImage()
      } else {
        codeToButton(it)?.let(onButtonDown)
      }
    }
    stage.scene.addEventFilter(KeyEvent.KEY_RELEASED) {
      codeToButton(it)?.let(onButtonUp)
    }
    stage.setOnCloseRequest {
      it.consume()
      hide()
      onClose()
    }
  }

  private fun configureStageAndImage() {
    if (isFullScreen) {
      configureForFullScreen()
    } else {
      configureForWindowed()
    }
  }

  private fun configureForFullScreen() {
    val bounds = Screen.getPrimary().visualBounds

    val ratioSource = (SCREEN_WIDTH.toDouble() / SCREEN_HEIGHT.toDouble()) * RATIO_STRETCH
    val ratioTarget = bounds.width / bounds.height

    with(imageView) {
      if (ratioSource > ratioTarget) {
        x = 0.0
        y = (bounds.height - (bounds.width / ratioSource)) / 2
        fitWidth = bounds.width
        fitHeight = bounds.width / ratioSource
      } else {
        x = (bounds.width - (bounds.height * ratioSource)) / 2
        y = 0.0
        fitWidth = bounds.height * ratioSource
        fitHeight = bounds.height
      }
    }

    stage.isFullScreen = true
  }

  private fun configureForWindowed() {
    with(imageView) {
      x = 0.0
      y = 0.0
      fitWidth = SCREEN_WIDTH * RATIO_STRETCH * SCALE
      fitHeight = (SCREEN_HEIGHT - 2 * TILE_SIZE) * SCALE
    }
    with(stage) {
      sizeToScene()
      isResizable = false
      isFullScreen = false
    }
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
