package choliver.nespot.worker

import choliver.nespot.CPU_FREQ_HZ
import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.math.ceil

class EmulatorWorker(rom: Rom) {
  private var cycles: Long = 0
  private var originSeconds: Double? = null

  private val nes = Nes(
    sampleRateHz = 48000,   // TODO
    rom = rom,
    videoSink = WorkerVideoSink(),
    audioSink = WorkerAudioSink()
  )

  init {
    self.onmessage = messageHandler(::handleMessage)
    restore()
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_EMULATE_UNTIL -> emulateUntil(payload as Double)
      MSG_BUTTON_DOWN -> nes.joypads.down(1, Button.valueOf(payload as String))
      MSG_BUTTON_UP -> nes.joypads.up(1, Button.valueOf(payload as String))
    }
  }

  private fun emulateUntil(timeSeconds: Double) {
    originSeconds = originSeconds ?: timeSeconds
    val target = ceil((timeSeconds - originSeconds!!) * CPU_FREQ_HZ.toDouble()).toInt()
    while (cycles < target) {
      cycles += nes.step()
    }
  }

  private fun restore() {
    nes.diagnostics.cpu.nextStep = Cpu.NextStep.RESET
  }

  companion object {
    fun createFor(romUrl: String) {
      self.fetch(romUrl).then { response ->
        response.arrayBuffer().then { buffer ->
          val b2 = Int8Array(buffer)
          val array = ByteArray(buffer.byteLength) { b2[it] }
          EmulatorWorker(Rom.parse(array))
        }
      }
    }
  }
}
