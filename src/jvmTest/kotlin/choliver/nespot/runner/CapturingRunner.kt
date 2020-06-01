package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Nes
import choliver.nespot.runner.Event.*
import choliver.nespot.runner.TimestampedEvent.Event.*
import choliver.nespot.runner.TimestampedEvent.Event.Close
import java.util.concurrent.LinkedBlockingQueue

class CapturingRunner(
  rom: Rom,
  private val prerecorded: List<TimestampedEvent>? = null
) {
  private val events = LinkedBlockingQueue<Event>()
  private val screen = Screen(onEvent = { events += it })
  private val audio = AudioPlayer()
  private val sink = CapturingVideoSink(screen.sink)
  private val nes = Nes(
    rom = rom,
    videoSink = sink,
    audioSink = audio.sink
  )
  private var closed = false
  private var timestamp = 0L
  private var idxPrerecorded = 0
  val recording = mutableListOf<TimestampedEvent>()

  fun run(): List<TimestampedEvent> {
    screen.show()
    audio.start()
    try {
      while (!closed) {
        nes.step()
        if (prerecorded != null) {
          inject(prerecorded)
        } else {
          consumeEvent()
        }
        timestamp++
      }
    } finally {
      screen.hide()
      screen.close()
      audio.close()
    }

    return recording
  }

  private fun inject(prerecorded: List<TimestampedEvent>) {
    if (prerecorded[idxPrerecorded].timestamp == timestamp) {
      when (val e = prerecorded[idxPrerecorded].event) {
        is ButtonDown -> onButtonDown(e.button)
        is ButtonUp -> onButtonUp(e.button)
        is Snapshot -> onSnapshot()
        is Close -> onClose()
      }
      idxPrerecorded++
    }
  }

  private fun consumeEvent() {
    when (val e = this@CapturingRunner.events.poll()) {
      is KeyDown -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is KeyAction.Joypad -> onButtonDown(action.button)
        is KeyAction.Snapshot -> onSnapshot()
      }
      is KeyUp -> when (val action = KeyAction.fromKeyCode(e.code)) {
        is KeyAction.Joypad -> onButtonUp(action.button)
      }
      is Event.Close -> onClose()
      is Error -> onClose()
    }
  }

  private fun onButtonDown(button: Joypads.Button) {
    recording += TimestampedEvent(timestamp, ButtonDown(button))
    nes.joypads.down(1, button)
  }

  private fun onButtonUp(button: Joypads.Button) {
    recording += TimestampedEvent(timestamp, ButtonUp(button))
    nes.joypads.up(1, button)
  }

  private fun onSnapshot() {
    recording += TimestampedEvent(timestamp, Snapshot(sink.snapshot))
  }

  private fun onClose() {
    recording += TimestampedEvent(timestamp, Close)
    closed = true
  }
}
