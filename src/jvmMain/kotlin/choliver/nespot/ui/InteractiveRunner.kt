package choliver.nespot.ui

import choliver.nespot.backup.BackupManager
import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu.NextStep.RESET
import choliver.nespot.nes.Nes
import choliver.nespot.ui.Event.*
import choliver.nespot.ui.KeyAction.*
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

class InteractiveRunner(
  rom: Rom,
  private val fullScreen: Boolean
) {
  @Volatile
  private var whichController = 1
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
      screen.close()
      audio.close()
      controllers.close()
    }
  }

  private fun consumeEvent() {
    when (val e = events.poll()) {
      is ControllerButtonDown -> nes.joypads.down(whichController, e.button)
      is ControllerButtonUp -> nes.joypads.up(whichController, e.button)
      is KeyDown -> {
        when (val action = KeyAction.fromKeyCode(e.code)) {
          is SetController1 -> whichController = 1
          is SetController2 -> whichController = 2
          is Joypad -> nes.joypads.down(whichController, action.button)
          is ToggleFullScreen -> screen.fullScreen = !screen.fullScreen
          is Reset -> nes.diagnostics.cpu.nextStep = RESET
          else -> Unit
        }
      }
      is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is Joypad -> nes.joypads.up(whichController, action.button)
        else -> Unit
      }
      is Close -> closed = true
      is Error -> {
        e.cause.printStackTrace(System.err)
        closed = true
      }
      null -> Unit
    }
  }

  companion object {
    val BACKUP_DIR = File("backups")
  }
}
