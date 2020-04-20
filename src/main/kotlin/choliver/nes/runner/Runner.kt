package choliver.nes.runner

import choliver.nes.Nes
import choliver.nes.debugger.FakeJoypads
import choliver.nes.debugger.Screen
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.timerTask

class Runner(rom: ByteArray) {
  constructor(romPath: String) : this(File(romPath).readBytes())

  private var nmiOccurred = false
  private val latch = CountDownLatch(1)

  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) },
    onClose = { latch.countDown() }
  )
  private val nes = Nes(
    rom,
    screen.buffer,
    joypads,
    onNmi = { nmiOccurred = true }
  )

  fun start() {
    screen.show()
    nes.inspection.fireReset()

    val timer = Timer()
    timer.scheduleAtFixedRate(timerTask {
      while (!nmiOccurred) {
        nes.inspection.step()
      }
      nmiOccurred = false
      screen.redraw()
    }, 0, 17)

    latch.await()
    timer.cancel()
    screen.exit()
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Runner(romPath = args[0]).start()
  }
}
