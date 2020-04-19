package choliver.nes.runner

import choliver.nes.Nes
import choliver.nes.debugger.FakeJoypads
import choliver.nes.debugger.Screen
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledExecutorService
import kotlin.concurrent.timerTask
import kotlin.system.measureTimeMillis

class Runner(
  rom: ByteArray
) {
  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) }
  )
  private var nmiOccurred = false
  private val nes = Nes(
    rom,
    screen.buffer,
    joypads,
    onNmi = {
      screen.redraw()
      nmiOccurred = true
    }
  )

  fun start() {
    screen.show()
    nes.inspection.fireReset()

    Timer().scheduleAtFixedRate(timerTask {
      while (!nmiOccurred) {
        nes.inspection.step()
      }
      nmiOccurred = false
    }, 0, 17)
  }
}
