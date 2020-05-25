package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu.NextStep.RESET
import choliver.nespot.nes.Nes
import choliver.nespot.persistence.BackupManager
import choliver.nespot.persistence.SnapshotManager
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction.*
import java.io.File
import java.util.concurrent.LinkedBlockingQueue


class InteractiveRunner(
  rom: Rom,
  private val snapshotFile: File?,
  private val fullScreen: Boolean
) {
  private val events = LinkedBlockingQueue<Event>()
  private var closed = false
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()
  private val nes = Nes(
    sampleRateHz = audio.sampleRateHz,
    rom = rom,
    videoSink = screen.sink,
    audioSink = audio.sink
  )
  private val controllers = ControllerManager(onEvent = { events += it })
  private val backupManager = BackupManager(rom, nes.persistentRam, BACKUP_DIR)
  private val snapshotManager = SnapshotManager(nes.diagnostics)

  fun run() {
    backupManager.maybeRestore()
    restore()

    screen.fullScreen = fullScreen
    screen.show()
    audio.start()
    controllers.start()

    try {
      while (!closed) {
        nes.step()
        consumeEvent()
      }
      maybeBackup()
    } catch (ex: Exception) {
      ex.printStackTrace(System.err)
    } finally {
      screen.hide()
      screen.exit()
      controllers.exit()
    }
  }

  private fun consumeEvent() {
    when (val e = events.poll()) {
      is ControllerButtonDown -> nes.joypads.down(1, e.button)
      is ControllerButtonUp -> nes.joypads.up(1, e.button)
      is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is Joypad -> nes.joypads.down(1, action.button)
        is ToggleFullScreen -> screen.fullScreen = !screen.fullScreen
        is Snapshot -> snapshotManager.snapshotToStdout()
        is Restore -> restore()
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

  private fun restore() {
    if (snapshotFile != null) {
      snapshotManager.restore(snapshotFile)
    } else {
      // TODO - reset
      nes.diagnostics.cpu.nextStep = RESET
    }
  }

  private fun maybeBackup() {
    // We don't want state from snapshot to overwrite main backup
    if (snapshotFile == null) {
      backupManager.maybeSave()
    }
  }

  companion object {
    val BACKUP_DIR = File("backups")
  }
}
