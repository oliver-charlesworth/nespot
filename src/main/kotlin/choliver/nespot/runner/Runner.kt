package choliver.nespot.runner

import choliver.nespot.nes.Nes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class Runner : CliktCommand() {
  private val rom by argument().file(mustExist = true, canBeDir = false)
  private val numPerfFrames by option("--perf", "-p").int()

  private val isClosed = AtomicBoolean(false)
  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) },
    onClose = { isClosed.set(true) }
  )
  private val audio = Audio(frameRateHz = 60)   // TODO - move to a constant somewhere

  override fun run() {
    val nes = Nes(
      rom = rom.readBytes(),
      videoBuffer = screen.buffer,
      audioBuffer = audio.buffer,
      joypads = joypads
    )
    nes.inspection.fireReset()

    if (numPerfFrames == null) {
      runNormally(nes)
    } else {
      runPerfTest(nes)
    }
  }

  private fun runNormally(nes: Nes) {
    screen.show()
    audio.start()

    while (!isClosed.get()) {
      measureNanoTime {
        nes.runFrame()
        screen.redraw()
        audio.play()
      }
    }

    screen.exit()
  }

  private fun runPerfTest(nes: Nes) {
    val runtimeMs = measureTimeMillis {
      repeat(numPerfFrames!!) { nes.runFrame() }
    }

    println("Ran ${numPerfFrames!!} frames in ${runtimeMs} ms (${(numPerfFrames!! * 1000.0 / runtimeMs).roundToInt()} fps)")
  }
}

fun main(args: Array<String>) = Runner().main(args)
