package choliver.nespot.runner

import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.ppu.TILE_SIZE
import choliver.nespot.runner.Event.*
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.IntBuffer
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE


class Screen(
  private val title: String = "NESpot",
  private val onEvent: (e: Event) -> Unit = {}
) {
  var fullScreen = false
  private var started = false
  private lateinit var frame: JFrame
  private val image = BufferedImage(
    SCREEN_WIDTH,
    SCREEN_HEIGHT,
    BufferedImage.TYPE_INT_ARGB_PRE
  )
  private val imageData = (image.raster.dataBuffer as DataBufferInt).data

  fun redraw(buffer: IntBuffer) {
    if (started) {
      System.arraycopy(buffer.array(), 0, imageData, 0, SCREEN_WIDTH * SCREEN_HEIGHT)
      frame.repaint()
    }
  }

  fun show() {
    if (!started) {
      start()
    }

//    frame.isVisible = true
  }

  fun hide() {
    frame.isVisible = false
  }

  fun exit() {
    frame.dispose()
  }

  private fun start() {
    frame = JFrame()
    frame.size = Dimension(OVERALL_WIDTH, OVERALL_HEIGHT)

    frame.add(object : JPanel() {
      private var prev = 0L

      override fun paintComponent(g: Graphics) {
        val now = System.currentTimeMillis()
        println("Frame time = ${now - prev} ms")
        prev = now
        g.drawImage(
          image
            .getSubimage(0, TILE_SIZE, SCREEN_WIDTH, SCREEN_HEIGHT - 2 * TILE_SIZE)
            .getScaledInstance(OVERALL_WIDTH, OVERALL_HEIGHT, Image.SCALE_DEFAULT),
          0,
          0,
          null
        )
      }
    })

    frame.defaultCloseOperation = DO_NOTHING_ON_CLOSE
    frame.isResizable = false
    frame.isVisible = true
    frame.title = title

    frame.addWindowListener(object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        hide()
        onEvent(Close)
      }
    })

    frame.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) = onEvent(KeyDown(e.keyCode))
      override fun keyReleased(e: KeyEvent) = onEvent(KeyUp(e.keyCode))
    })

    frame.createBufferStrategy(2)
    started = true
  }

  companion object {
    private const val SCALE = 4.0
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good

    private const val OVERALL_WIDTH = (SCREEN_WIDTH * SCALE * RATIO_STRETCH).toInt()
    private const val OVERALL_HEIGHT = ((SCREEN_HEIGHT - 2 * TILE_SIZE) * SCALE).toInt()

  }
}
