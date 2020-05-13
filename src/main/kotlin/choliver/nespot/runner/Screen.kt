package choliver.nespot.runner

import choliver.nespot.ppu.SCREEN_HEIGHT
import choliver.nespot.ppu.SCREEN_WIDTH
import choliver.nespot.runner.Event.*
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.ByteBuffer
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
  private lateinit var graphics: Graphics
  private val byteBuffer = ByteBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT * 4)
  private val intBuffer: IntBuffer = IntBuffer.allocate(SCREEN_WIDTH * SCREEN_HEIGHT)

  fun redraw(buffer: IntBuffer) {
    if (started) {
      intBuffer.position(0)
      buffer.position(0)
      intBuffer.put(buffer)

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
    frame.size = Dimension(
      SCREEN_WIDTH,
      SCREEN_HEIGHT
    )

    frame.add(object : JPanel() {
      override fun paintComponent(g: Graphics) {
        val image = BufferedImage(
          SCREEN_WIDTH,
          SCREEN_HEIGHT,
          BufferedImage.TYPE_INT_ARGB_PRE
        )

        val array = (image.raster.dataBuffer as DataBufferInt).data
        System.arraycopy(intBuffer.array(), 0, array, 0, SCREEN_WIDTH * SCREEN_HEIGHT)

        g.drawImage(image, 0, 0, null)
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
    graphics = frame.bufferStrategy.drawGraphics
    started = true
  }

  companion object {
    private const val SCALE = 4.0
    private const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
  }
}
