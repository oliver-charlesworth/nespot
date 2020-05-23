package choliver.nespot.apu

import choliver.nespot.Memory
import choliver.nespot.Rational

class Channels(
  val sq1: SynthContext<SquareSynth>,
  val sq2: SynthContext<SquareSynth>,
  val tri: SynthContext<TriangleSynth>,
  val noi: SynthContext<NoiseSynth>,
  val dmc: SynthContext<DmcSynth>
) {
  constructor(
    cyclesPerSample: Rational,
    memory: Memory
  ) : this(
    sq1 = SynthContext(cyclesPerSample, SquareSynth()),
    sq2 = SynthContext(cyclesPerSample, SquareSynth()),
    tri = SynthContext(cyclesPerSample, TriangleSynth()).apply {
      inhibitMute()
      fixEnvelope(1)
    },
    noi = SynthContext(cyclesPerSample, NoiseSynth()).apply {
      inhibitMute()
    },
    dmc = SynthContext(cyclesPerSample, DmcSynth(memory = memory)).apply {
      inhibitMute()
      fixEnvelope(1)
    }
  )

  companion object {
    private fun SynthContext<*>.fixEnvelope(level: Int) {
      envelope.directMode = true
      envelope.param = level
    }

    private fun SynthContext<*>.inhibitMute() {
      sweep.inhibitMute = true
    }
  }
}
