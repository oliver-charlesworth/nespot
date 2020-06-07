package choliver.nespot.ui

import choliver.nespot.*
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
        VISIBLE_WIDTH.toDouble(),
        VISIBLE_HEIGHT.toDouble()
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
    configureImageView(DisplayInfo(targetWidth = bounds.width, targetHeight = bounds.height))
    stage.scene.cursor = Cursor.NONE
    stage.isFullScreen = true
  }

  private fun configureForWindowed() {
    configureImageView(DisplayInfo(scale = SCALE))
    with(stage) {
      sizeToScene()
      scene.cursor = Cursor.DEFAULT
      isResizable = false
      isFullScreen = false
    }
  }

  private fun configureImageView(displayInfo: DisplayInfo) {
    with(imageView) {
      x = displayInfo.marginHorizontal
      y = displayInfo.marginVertical
      fitWidth = displayInfo.resultWidth
      fitHeight = displayInfo.resultHeight
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
  }
}
