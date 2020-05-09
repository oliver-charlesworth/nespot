package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Nes
import choliver.nespot.persistence.BackupManager
import choliver.nespot.persistence.SnapshotManager
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.KeyAction.*
import choliver.nespot.sixfiveohtwo.Cpu.NextStep.RESET
import java.io.File
import java.util.concurrent.LinkedBlockingQueue


class InteractiveRunner(
  rom: Rom,
  private val snapshotFile: File?,
  private val fullScreen: Boolean
) {
  private val events = LinkedBlockingQueue<Event>()
  private var closed = false
  private val joypads = FakeJoypads()
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()
  private val nes = Nes(
    rom = rom,
    joypads = joypads,
    onAudioBufferReady = { events += Audio(it) },
    onVideoBufferReady = { events += Video(it) }
  )
  private val backupManager = BackupManager(rom, nes.prgRam, BACKUP_DIR)
  private val snapshotManager = SnapshotManager(nes.diagnostics)

  fun run() {
    backupManager.maybeRestore()
    restore()

    screen.fullScreen = fullScreen
    screen.show()
    audio.start()

    try {
      while (!closed) {
        nes.step()
        consumeEvent()
      }
      backupManager.maybeSave()
    } finally {
      screen.hide()
      screen.exit()
    }
  }

  private fun consumeEvent() {
    when (val e = events.poll()) {
      is Audio -> audio.play(e.buffer)
      is Video -> screen.redraw(e.buffer)
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
      snapshotManager.restore(snapshotFile)
    } else {
      // TODO - reset
      nes.diagnostics.cpu.nextStep = RESET
    }
  }

  companion object {
    val BACKUP_DIR = File("backups")
  }
}
