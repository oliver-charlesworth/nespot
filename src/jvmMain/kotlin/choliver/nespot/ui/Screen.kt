package choliver.nespot.ui

import choliver.nespot.SCREEN_HEIGHT
import choliver.nespot.SCREEN_WIDTH
import choliver.nespot.TILE_SIZE
import choliver.nespot.nes.VideoSink
import choliver.nespot.nes.VideoSink.ColorPackingMode.BGRA
import choliver.nespot.ui.Event.*
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Cursor
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
import java.io.Closeable
import java.nio.ByteBuffer

class Screen(
  private val title: String = "NESpot",
  private val onEvent: (e: Event) -> Unit = {}
) : Closeable {
  private val pressedButtons = mutableSetOf<KeyCode>()
  var fullScreen = false
    set(value) {
      field = value
      onFxThread { configureStageAndImage() }
    }
  private var started = false
  private lateinit var stage: Stage
  private lateinit var imageView: ImageView

  val sink get() = object : VideoSink {
    private val bufferA = ByteBuffer.allocateDirect(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
    private val bufferB = ByteBuffer.allocateDirect(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
    private var buffer = bufferA
    private var bufferInt = buffer.asIntBuffer()

    override val colorPackingMode = BGRA

    override fun put(color: Int) {
      bufferInt.put(color)

      if (!bufferInt.hasRemaining()) {
        val bufferToCommit = buffer
        buffer = if (buffer === bufferA) bufferB else bufferA
        bufferInt = buffer.asIntBuffer()
        onFxThread {
          imageView.image = WritableImage(PixelBuffer(
            SCREEN_WIDTH,
            SCREEN_HEIGHT,
            bufferToCommit,
            PixelFormat.getByteBgraPreInstance() // Mac native format
          ))
        }
      }
    }
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

  override fun close() {
    hide()
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
    imageView = ImageView().apply {
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
    stage.scene.addEventFilter(KeyEvent.KEY_PRESSED) {
      if (it.code !in pressedButtons) {
        onEvent(KeyDown(it.code))
        pressedButtons += it.code
      }
    }
    stage.scene.addEventFilter(KeyEvent.KEY_RELEASED) {
      onEvent(KeyUp(it.code))
      pressedButtons -= it.code
    }
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

    stage.scene.cursor = Cursor.NONE
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
      scene.cursor = Cursor.DEFAULT
      isResizable = false
      isFullScreen = false
    }
  }

  private fun onFxThread(block: () -> Unit) {
    if (started) {
      Platform.runLater {
        try {
          block()
        } catch (e: Exception) {
          onEvent(Error(e))
          throw e
        }
      }
    }
  }

  companion object {
    private const val SCALE = 4.0
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}
