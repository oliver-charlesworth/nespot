package choliver.nespot.ui

import choliver.nespot.*
import choliver.nespot.ui.KeyAction.Joypad
import choliver.nespot.ui.KeyAction.ToggleFullScreen
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.Worker
import org.w3c.dom.events.KeyboardEvent
import kotlin.browser.document
import kotlin.browser.window

class Ui(
  private val worker: Worker,
  private val romPath: String
) {
  private val audio = AudioPlayer()
  private val screen = Screen()

  init {
    worker.onmessage = messageHandler(::handleMessage)
    document.onkeydown = ::handleKeyDown
    document.onkeyup = ::handleKeyUp
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_ALIVE -> handleWorkerAlive()
      MSG_VIDEO_FRAME -> screen.absorbFrame(Uint8ClampedArray(payload as ArrayBuffer))
      MSG_AUDIO_CHUNK -> audio.absorbChunk(Float32Array(payload as ArrayBuffer))
    }
  }

  private fun handleWorkerAlive() {
    postMessage(MSG_CONFIGURE, Config(
      romPath = romPath,
      sampleRateHz = audio.sampleRateHz
    ))
    window.requestAnimationFrame(::executeFrame) // Start animation loop
  }

  private fun handleKeyDown(e: KeyboardEvent) {
    when (val action = KeyAction.fromKeyCode(e.code)) {
      is ToggleFullScreen -> screen.fullScreen = !screen.fullScreen
      is Joypad -> postMessage(MSG_BUTTON_DOWN, action.button.name)
    }
  }

  private fun handleKeyUp(e: KeyboardEvent) {
    when (val action = KeyAction.fromKeyCode(e.code)) {
      is Joypad -> postMessage(MSG_BUTTON_UP, action.button.name)
    }
  }

  // Every browser frame, we draw the latest completed emulator output, and schedule the emulator to catch up.
  // The emulator generates frames asynchronously, so we don't necessarily draw every emulator frame.
  // It also generates audio asynchronously - we schedule every audio chunk to be played.
  private fun executeFrame(timeMs: Double) {
    window.requestAnimationFrame(::executeFrame)
    screen.redraw()
    postMessage(MSG_EMULATE_UNTIL, timeMs / 1000)
  }

  private fun postMessage(type: String, payload: Any?) {
    worker.postMessage(arrayOf(type, payload))
  }
}


