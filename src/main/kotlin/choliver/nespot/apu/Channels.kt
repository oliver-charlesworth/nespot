package choliver.nespot.apu

class Channels(
  val sq1: SynthContext<SquareSynth>,
  val sq2: SynthContext<SquareSynth>,
  val tri: SynthContext<TriangleSynth>,
  val noi: SynthContext<NoiseSynth>,
  val dmc: SynthContext<DmcSynth>
)
