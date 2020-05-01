package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class Runner : CliktCommand(name = "nespot") {
  private val raw by argument(name = "rom").file(mustExist = true, canBeDir = false)
  private val displayInfo by option("--info", "-i").flag()
  private val numPerfFrames by option("--perf", "-p").int()
  private val fullScreen by option("--fullscreen", "-f").flag()

  override fun run() {
    val rom = Rom.parse(raw.readBytes())

    if (displayInfo) {
      rom.printInfo()
    } else {
      Inner(rom).run()
    }
  }

  inner class Inner(rom: Rom) {
    private val isClosed = AtomicBoolean(false)
    private val joypads = FakeJoypads()
    private val screen = Screen(
      isFullScreen = fullScreen,
      onButtonDown = { joypads.down(1, it) },
      onButtonUp = { joypads.up(1, it) },
      onClose = { isClosed.set(true) }
    )
    private val audio = Audio(frameRateHz = 60)   // TODO - move to a constant somewhere
    private val nes = Nes(
      rom = rom,
      videoBuffer = screen.buffer,
      audioBuffer = audio.buffer,
      joypads = joypads
    )

    fun run() {
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
          nes.runToEndOfFrame()
          screen.redraw()
          audio.play()
        }
      }

      screen.exit()
    }

    private fun runPerfTest(nes: Nes) {
      val runtimeMs = measureTimeMillis {
        repeat(numPerfFrames!!) { nes.runToEndOfFrame() }
      }
      println("Ran ${numPerfFrames!!} frames in ${runtimeMs} ms (${(numPerfFrames!! * 1000.0 / runtimeMs).roundToInt()} fps)")
    }
  }
}

fun main(args: Array<String>) = Runner().main(args)
