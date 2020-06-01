package choliver.nespot.emulator

import choliver.nespot.*
import choliver.nespot.cartridge.Rom
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.math.ceil

class Emulator {
  private lateinit var nes: Nes
  private var cycles: Long = 0
  private var originSeconds: Double? = null

  init {
    self.onmessage = messageHandler(::handleMessage)
    self.postMessage(arrayOf(MSG_ALIVE, null))
  }

  private fun handleMessage(type: String, payload: Any?) {
    when (type) {
      MSG_CONFIGURE -> initNes(Config(payload))
      MSG_EMULATE_UNTIL -> emulateUntil(payload as Double)
      MSG_BUTTON_DOWN -> nes.joypads.down(1, Button.valueOf(payload as String))
      MSG_BUTTON_UP -> nes.joypads.up(1, Button.valueOf(payload as String))
    }
  }

  private fun initNes(config: Config) {
    fetchRom(config.romPath) { rom ->
      nes = Nes(
        rom = rom,
        videoSink = EmulatorVideoSink(),
        audioSink = EmulatorAudioSink(config.sampleRateHz)
      )
    }
  }

  private fun fetchRom(romPath: String, onFulfilled: (Rom) -> Unit) {
    self.fetch(romPath).then { response ->
      response.arrayBuffer().then { buffer ->
        val bufferInt8 = Int8Array(buffer)
        val rom = Rom.parse(ByteArray(buffer.byteLength) { bufferInt8[it] })
        onFulfilled(rom)
      }
    }
  }

  private fun emulateUntil(timeSeconds: Double) {
    originSeconds = originSeconds ?: timeSeconds
    val target = ceil((timeSeconds - originSeconds!!) * CPU_FREQ_HZ.toDouble()).toInt()
    while (cycles < target) {
      cycles += nes.step()
    }
  }
}
