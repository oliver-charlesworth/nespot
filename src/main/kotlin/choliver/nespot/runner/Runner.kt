package choliver.nespot.runner

import choliver.nespot.FRAME_RATE_HZ
import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import choliver.nespot.persistence.BackupManager
import choliver.nespot.persistence.SnapshotManager
import choliver.nespot.runner.KeyAction.*
import choliver.nespot.runner.Screen.Event.*
import choliver.nespot.sixfiveohtwo.Cpu.NextStep.RESET
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.roundToInt
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class Runner : CliktCommand(name = "nespot") {
  private val raw by argument(name = "rom").file(mustExist = true, canBeDir = false)
  private val displayInfo by option("--info", "-i").flag()
  private val numPerfFrames by option("--perf", "-p").int()
  private val fullScreen by option("--fullscreen", "-f").flag()
  private val snapshotFile by option("--snapshot", "-s").file(mustExist = true, canBeDir = false)

  override fun run() {
    val rom = Rom.parse(raw.readBytes())

    if (displayInfo) {
      rom.printInfo()
    } else {
      Inner(rom).run()
    }
  }

  private inner class Inner(rom: Rom) {
    private val events = LinkedBlockingQueue<Screen.Event>()
    private var closed = false
    private val joypads = FakeJoypads()
    private val screen = Screen(onEvent = { events += it })
    private val audio = Audio(frameRateHz = FRAME_RATE_HZ)
    private val nes = Nes(
      rom = rom,
      videoBuffer = screen.buffer,
      audioBuffer = audio.buffer,
      joypads = joypads
    )
    private val backupManager = BackupManager(rom, nes.prgRam, BACKUP_DIR)
    private val snapshotManager = SnapshotManager(nes.diagnostics)

    fun run() {
      screen.fullScreen = fullScreen

      restore()

      if (numPerfFrames == null) {
        runNormally()
      } else {
        runPerfTest()
      }
    }

    private fun runNormally() {
      backupManager.maybeRestore()
      screen.show()
      audio.start()

      try {
        while (!closed) {
          measureNanoTime {
            nes.runToEndOfFrame()
            screen.redraw()
            audio.play()
            consumeEvents()
          }
        }
        backupManager.maybeSave()
      } finally {
        screen.hide()
        screen.exit()
      }
    }

    private fun consumeEvents() {
      val myEvents = mutableListOf<Screen.Event>()
      events.drainTo(myEvents)
      myEvents.forEach { e ->
        when (e) {
          is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
            is Joypad -> joypads.down(1, action.button)
            is ToggleFullScreen -> screen.fullScreen = !screen.fullScreen
            is Snapshot -> snapshotManager.snapshotToStdout()
            is Restore -> restore()
          }
          is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
            is Joypad -> joypads.up(1, action.button)
          }
          is Close -> closed = true
        }
      }
    }

    private fun restore() {
      if (snapshotFile != null) {
        snapshotManager.restore(snapshotFile!!)
      } else {
        // TODO - reset
        nes.diagnostics.cpu.nextStep = RESET
      }
    }

    private fun runPerfTest() {
      val runtimeMs = measureTimeMillis {
        repeat(numPerfFrames!!) { nes.runToEndOfFrame() }
      }
      println("Ran ${numPerfFrames!!} frames in ${runtimeMs} ms (${(numPerfFrames!! * 1000.0 / runtimeMs).roundToInt()} fps)")
    }
  }

  companion object {
    val BACKUP_DIR = File("backup")
  }
}

fun main(args: Array<String>) = Runner().main(args)
