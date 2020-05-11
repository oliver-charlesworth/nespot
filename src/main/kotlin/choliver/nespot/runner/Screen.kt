package choliver.nespot.runner

import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.ppu.TILE_SIZE
import choliver.nespot.runner.Event.*
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.geometry.Rectangle2D
import javafx.scene.Cursor
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
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
  var fullScreen = false
    set(value) {
      field = value
      onFxThread { configureStageAndImage() }
    }
  private var started = false
  private lateinit var stage: Stage
  private lateinit var img: WritableImage
  private lateinit var imageView: ImageView
  private val byteBuffer = ByteBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
  private val intBuffer: IntBuffer = byteBuffer.asIntBuffer()

  private var prev: Long = 0
  private var yes = true

  fun redraw(buffer: IntBuffer) {
    val now = System.currentTimeMillis()
    println("Screen::redraw (${now - prev} ms)")
    prev = now

    intBuffer.position(0)
    buffer.position(0)
    intBuffer.put(buffer)
    onFxThread {
      if (yes) {
        img.pixelWriter.setPixels(
          0, 0, SCREEN_WIDTH, SCREEN_HEIGHT,
          PixelFormat.getByteBgraPreInstance(),
          byteBuffer.array(),
          0, SCREEN_WIDTH * 4
        )
      }
      yes = !yes
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

  fun exit() {
    Platform.exit()
  }

  private fun start() {
    Platform.setImplicitExit(false)
    Platform.startup {
      initImageView()
      initStage()
      configureStageAndImage()
      object : AnimationTimer() {
        private var prev: Long = 0
        override fun handle(now: Long) {
          val now = System.currentTimeMillis()
          println("AnimationTimer::handle (${now - prev} ms)")
          prev = now

        }

      }.start()
    }
    started = true
  }

  private fun initImageView() {
    img = WritableImage(SCREEN_WIDTH, SCREEN_HEIGHT)
    imageView = ImageView(img).apply {
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
