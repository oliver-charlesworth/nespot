package choliver.nespot.apu

import choliver.nespot.Memory

class Channels(
  val sq1: SynthContext<SquareSynth, SweepActive, EnvelopeActive>,
  val sq2: SynthContext<SquareSynth, SweepActive, EnvelopeActive>,
  val tri: SynthContext<TriangleSynth, SweepInactive, EnvelopeInactive>,
  val noi: SynthContext<NoiseSynth, SweepInactive, EnvelopeActive>,
  val dmc: SynthContext<DmcSynth, SweepInactive, EnvelopeInactive>
) {
  constructor(memory: Memory) : this(
    sq1 = Timer().let { timer ->
      SynthContext(
        synth = SquareSynth(),
        timer = timer,
        sweep = SweepActive(timer, negateWithOnesComplement = false),
        envelope = EnvelopeActive()
      )
    },
    sq2 = Timer().let { timer ->
      SynthContext(
        synth = SquareSynth(),
        timer = timer,
        sweep = SweepActive(timer, negateWithOnesComplement = true),
        envelope = EnvelopeActive()
      )
    },
    tri = SynthContext(
      synth = TriangleSynth(),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeInactive(1)
    ),
    noi = SynthContext(
      synth = NoiseSynth(),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeActive()
    ),
    dmc = SynthContext(
      synth = DmcSynth(memory = memory),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeInactive(1)
    )
  )
}
