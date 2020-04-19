package choliver.nes.runner

import choliver.nes.Nes
import choliver.nes.debugger.FakeJoypads
import choliver.nes.debugger.Screen
import java.util.*
import kotlin.concurrent.timerTask

class Runner(
  rom: ByteArray
) {
  private val screen = Screen()
  private val joypads = FakeJoypads()
  private val nes = Nes(
    rom,
    screen.buffer,
    joypads,
    onNmi = { screen.redraw() }
  )

  fun start() {
    Timer().scheduleAtFixedRate(timerTask {
      // TODO - need some notion of Nes.runFrame()
    }, 0, 250)
  }
}
