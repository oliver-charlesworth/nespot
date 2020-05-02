package choliver.nespot.runner

import choliver.nespot.observable
import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.ppu.TILE_SIZE
import choliver.nespot.runner.Screen.Event.*
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelBuffer
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import java.nio.ByteBuffer
import java.nio.IntBuffer


class Screen(
  private val title: String = "NESpot",
  private val onEvent: (e: Event) -> Unit = {}
) {
  sealed class Event {
    data class KeyDown(val code: KeyCode) : Event()
    data class KeyUp(val code: KeyCode) : Event()
    object Close : Event()
  }

  var fullScreen by observable(false) { onFxThread { configureStageAndImage() } }
  private var started = false
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
    onFxThread { pixelBuffer.updateBuffer { null } }
  }

  fun show() {
    if (!started) {
      start()
    }
    onFxThread {
      stage.show()
      stage.toFront()
    }
  }

  fun hide() {
    onFxThread { stage.hide() }
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
    started = true
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
    stage = Stage()
    stage.fullScreenExitKeyCombination = KeyCombination.NO_MATCH
    stage.title = title
    stage.scene = Scene(Group().apply { children.add(imageView) }, Color.BLACK)
    stage.scene.addEventFilter(KeyEvent.KEY_PRESSED) { onEvent(KeyDown(it.code)) }
    stage.scene.addEventFilter(KeyEvent.KEY_RELEASED) { onEvent(KeyUp(it.code)) }
    stage.setOnCloseRequest {
      it.consume()
      hide()
      onEvent(Close)
    }
  }

  private fun configureStageAndImage() {
    if (fullScreen) {
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

  private fun onFxThread(block: () -> Unit) {
    if (started) { Platform.runLater(block) }
  }

  companion object {
    private const val SCALE = 4.0
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}
