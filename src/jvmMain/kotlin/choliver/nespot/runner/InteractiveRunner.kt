package choliver.nespot.runner

import choliver.nespot.backup.BackupManager
import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu.NextStep.RESET
import choliver.nespot.nes.Nes
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction.*
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

class InteractiveRunner(
  rom: Rom,
  private val fullScreen: Boolean
) {
  private val events = LinkedBlockingQueue<Event>()
  private var closed = false
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()
  private val nes = Nes(
    rom = rom,
    videoSink = screen.sink,
    audioSink = audio.sink
  )
  private val controllers = ControllerManager(onEvent = { events += it })
  private val backupManager = BackupManager(rom, nes.persistentRam, BACKUP_DIR)

  fun run() {
    backupManager.maybeRestore()
    screen.fullScreen = fullScreen
    screen.show()
    audio.start()
    controllers.start()

    try {
      while (!closed) {
        nes.step()
        consumeEvent()
      }
      backupManager.maybeSave()
    } catch (ex: Exception) {
      ex.printStackTrace(System.err)
    } finally {
      screen.hide()
      screen.close()
      audio.close()
      controllers.close()
    }
  }

  private fun consumeEvent() {
    when (val e = events.poll()) {
      is ControllerButtonDown -> nes.joypads.down(1, e.button)
      is ControllerButtonUp -> nes.joypads.up(1, e.button)
      is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is Joypad -> nes.joypads.down(1, action.button)
        is ToggleFullScreen -> screen.fullScreen = !screen.fullScreen
        is Reset -> nes.diagnostics.cpu.nextStep = RESET
      }
      is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is Joypad -> nes.joypads.up(1, action.button)
      }
      is Close -> closed = true
      is Error -> {
        e.cause.printStackTrace(System.err)
        closed = true
      }
    }
  }

  companion object {
    val BACKUP_DIR = File("backups")
  }
}
