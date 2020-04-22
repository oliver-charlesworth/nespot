package choliver.nespot.runner

import choliver.nespot.nes.Nes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.timerTask
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

class Runner : CliktCommand() {
  private val rom by argument().file(mustExist = true, canBeDir = false)
  private val numPerfFrames by option("--perf", "-p").int()

  private var nmiOccurred = false
  private val latch = CountDownLatch(1)
  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) },
    onClose = { latch.countDown() }
  )

  override fun run() {
    val nes = Nes(
      rom.readBytes(),
      screen.buffer,
      joypads,
      onNmi = { nmiOccurred = true }
    )
    nes.inspection.fireReset()

    if (numPerfFrames == null) {
      runNormally(nes)
    } else {
      runPerfTest(nes)
    }
  }

  private fun runNormally(nes: Nes) {
    val timer = Timer()
    screen.show()

    timer.schedule(timerTask {
      runFrame(nes)
      screen.redraw()
    }, 0, 17)


    latch.await()
    timer.cancel()
    screen.exit()
  }

  private fun runPerfTest(nes: Nes) {
    val runtimeMs = measureTimeMillis {
      repeat(numPerfFrames!!) { runFrame(nes) }
    }

    println("Ran ${numPerfFrames!!} frames in ${runtimeMs} ms (${(numPerfFrames!! * 1000.0 / runtimeMs).roundToInt()} fps)")
  }

  private fun runFrame(nes: Nes) {
    while (!nmiOccurred) {
      nes.inspection.step()
    }
    nmiOccurred = false
  }
}

fun main(args: Array<String>) = Runner().main(args)
