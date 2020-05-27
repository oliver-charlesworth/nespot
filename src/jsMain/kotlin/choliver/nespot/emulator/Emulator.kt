package choliver.nespot.emulator

import choliver.nespot.*
import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.math.ceil

class Emulator(private val rom: Rom) {
  private lateinit var nes: Nes
  private var cycles: Long = 0
  private var originSeconds: Double? = null

  init {
    self.onmessage = messageHandler(::handleMessage)
    self.postMessage(arrayOf(MSG_ALIVE, null))
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_SET_SAMPLE_RATE -> initNes(payload as Int)
      MSG_EMULATE_UNTIL -> emulateUntil(payload as Double)
      MSG_BUTTON_DOWN -> nes.joypads.down(1, Button.valueOf(payload as String))
      MSG_BUTTON_UP -> nes.joypads.up(1, Button.valueOf(payload as String))
    }
  }

  private fun initNes(sampleRateHz: Int) {
    nes = Nes(
      rom = rom,
      videoSink = EmulatorVideoSink(),
      audioSink = EmulatorAudioSink(sampleRateHz)
    )
    nes.diagnostics.cpu.nextStep = Cpu.NextStep.RESET
  }

  private fun emulateUntil(timeSeconds: Double) {
    originSeconds = originSeconds ?: timeSeconds
    val target = ceil((timeSeconds - originSeconds!!) * CPU_FREQ_HZ.toDouble()).toInt()
    while (cycles < target) {
      cycles += nes.step()
    }
  }

  companion object {
    fun createFor(romUrl: String) {
      self.fetch(romUrl).then { response ->
        response.arrayBuffer().then { buffer ->
          val b2 = Int8Array(buffer)
          val array = ByteArray(buffer.byteLength) { b2[it] }
          Emulator(Rom.parse(array))
        }
      }
    }
  }
}
