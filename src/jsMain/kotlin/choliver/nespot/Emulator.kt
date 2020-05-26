package choliver.nespot

import choliver.nespot.cartridge.Rom
import choliver.nespot.cpu.Cpu
import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Nes
import kotlin.math.ceil

class Emulator(private val rom: Rom) {
  private lateinit var nes: Nes
  private var cycles: Long = 0
  private var originSeconds: Double? = null

  init {
    self.onmessage = messageHandler(::handleMessage)
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
      sampleRateHz = sampleRateHz,
      rom = rom,
      videoSink = EmulatorVideoSink(),
      audioSink = EmulatorAudioSink(sampleRateHz)
    )
    restore()
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
}
