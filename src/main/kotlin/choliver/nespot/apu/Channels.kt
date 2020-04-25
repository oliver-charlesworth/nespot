package choliver.nespot.apu

internal class Channels(
  val pulse1: SynthContext<PulseSynth>,
  val pulse2: SynthContext<PulseSynth>,
  val triangle: SynthContext<TriangleSynth>,
  val noise: SynthContext<NoiseSynth>,
  val dmc: SynthContext<DmcSynth>
)
