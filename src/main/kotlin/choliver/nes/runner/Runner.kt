package choliver.nes.runner

import choliver.nes.Nes
import choliver.nes.debugger.FakeJoypads
import choliver.nes.debugger.Screen
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.timerTask

class Runner : CliktCommand() {
  private val rom by argument().file(mustExist = true, canBeDir = false)
  private val headless by option().flag(default = false)

  override fun run() {
    var nmiOccurred = false
    val latch = CountDownLatch(1)

    val joypads = FakeJoypads()
    val screen = Screen(
      onButtonDown = { joypads.down(1, it) },
      onButtonUp = { joypads.up(1, it) },
      onClose = { latch.countDown() }
    )
    val nes = Nes(
      rom.readBytes(),
      screen.buffer,
      joypads,
      onNmi = { nmiOccurred = true }
    )
    val numFrames = AtomicInteger(0)

    if (!headless) {
      screen.show()
    }

    nes.inspection.fireReset()

    val timer = Timer()
    timer.scheduleAtFixedRate(timerTask {
      while (!nmiOccurred) {
        nes.inspection.step()
      }
      nmiOccurred = false

      if (!headless) {
        screen.redraw()
      }
      numFrames.incrementAndGet()
    }, 0, 17)

    latch.await()
    timer.cancel()

    if (!headless) {
      screen.exit()
    }
    println("Ran ${numFrames.get()} frames")
  }
}

fun main(args: Array<String>) = Runner().main(args)
