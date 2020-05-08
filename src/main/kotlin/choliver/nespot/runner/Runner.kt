package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import choliver.nespot.persistence.BackupManager
import choliver.nespot.persistence.SnapshotManager
import choliver.nespot.runner.KeyAction.*
import choliver.nespot.runner.Screen.Event
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
    private val events = LinkedBlockingQueue<Event>()
    private var closed = false
    private var redraw = false
    private var play = false
    private val joypads = FakeJoypads()
    private val screen = Screen(onEvent = { events += it })
    private val audio = Audio()
    private val nes = Nes(
      rom = rom,
      videoBuffer = screen.buffer,
      audioBuffer = audio.buffer,
      joypads = joypads,
      onAudioBufferReady = { play = true },
      onVideoBufferReady = { redraw = true }
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
          nes.step()
          maybeRedraw()
          maybePlay()
          consumeEvent()
        }
        backupManager.maybeSave()
      } finally {
        screen.hide()
        screen.exit()
      }
    }

    private fun maybeRedraw() {
      if (redraw) {
        redraw = false
        screen.redraw()
      }
    }

    private fun maybePlay() {
      if (play) {
        play = false
        audio.play()
      }
    }

    private fun consumeEvent() {
      when (val e = events.poll()) {
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
        repeat(numPerfFrames!!) {
          while (!redraw) { nes.step() }
          redraw = false
        }
      }
      println("Ran ${numPerfFrames!!} frames in ${runtimeMs} ms (${(numPerfFrames!! * 1000.0 / runtimeMs).roundToInt()} fps)")
    }
  }

  companion object {
    val BACKUP_DIR = File("backups")
  }
}

fun main(args: Array<String>) = Runner().main(args)
