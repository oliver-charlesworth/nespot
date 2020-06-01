package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(rom: Rom) {
  private val events = LinkedBlockingQueue<Event>()
  private var closed = false
  private val screen = Screen(onEvent = { events += it })
  private val nes = Nes(
    rom = rom,
    videoSink = screen.sink
  )

  fun run() {
    screen.show()

    try {
      while (!closed) {
        nes.step()
        consumeEvent()
      }
    } catch (ex: Exception) {
      ex.printStackTrace(System.err)
    } finally {
      screen.hide()
      screen.exit()
    }
  }

  private fun consumeEvent() {
    when (val e = events.poll()) {
      is Event.ControllerButtonDown -> nes.joypads.down(1, e.button)
      is Event.ControllerButtonUp -> nes.joypads.up(1, e.button)
      is Event.Close -> closed = true
      is Event.Error -> {
        e.cause.printStackTrace(System.err)
        closed = true
      }
    }
  }
}
