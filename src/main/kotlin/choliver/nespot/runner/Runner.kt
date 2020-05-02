package choliver.nespot.runner

import choliver.nespot.FRAME_RATE_HZ
import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import choliver.nespot.runner.Runner.Event.KeyDown
import choliver.nespot.runner.Runner.Event.KeyUp
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import javafx.scene.input.KeyCode
import java.util.concurrent.LinkedBlockingQueue
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

  private sealed class Event {
    data class KeyDown(val code: KeyCode) : Event()
    data class KeyUp(val code: KeyCode) : Event()
  }

  private inner class Inner(rom: Rom) {
    private val events = LinkedBlockingQueue<Event>()
    private val isClosed = AtomicBoolean(false)
    private val joypads = FakeJoypads()
    private val screen = Screen(
      onKeyDown = { events += KeyDown(it) },
      onKeyUp = { events += KeyUp(it) },
      onClose = { isClosed.set(true) }
    )
    private val audio = Audio(frameRateHz = FRAME_RATE_HZ)
    private val nes = Nes(
      rom = rom,
      videoBuffer = screen.buffer,
      audioBuffer = audio.buffer,
      joypads = joypads
    )

    fun run() {
      screen.fullScreen = fullScreen
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
          consumeEvents()
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

    private fun consumeEvents() {
      val myEvents = mutableListOf<Event>()
      events.drainTo(myEvents)
      myEvents.forEach { e ->
        when (e) {
          is KeyDown -> {
            when (e.code) {
              KeyCode.F -> screen.fullScreen = !screen.fullScreen
              else -> codeToButton(e.code)?.let { joypads.down(1, it) }
            }
          }
          is KeyUp -> codeToButton(e.code)?.let { joypads.up(1, it) }
        }
      }
    }

    private fun codeToButton(code: KeyCode) = when (code) {
      KeyCode.Z -> Button.A
      KeyCode.X -> Button.B
      KeyCode.CLOSE_BRACKET -> Button.START
      KeyCode.OPEN_BRACKET -> Button.SELECT
      KeyCode.LEFT -> Button.LEFT
      KeyCode.RIGHT -> Button.RIGHT
      KeyCode.UP -> Button.UP
      KeyCode.DOWN -> Button.DOWN
      else -> null
    }
  }
}

fun main(args: Array<String>) = Runner().main(args)
