package choliver.nespot.apu

import choliver.nespot.Memory

class Channels(
  val sq1: Channel<SquareSynth, SweepActive, EnvelopeActive>,
  val sq2: Channel<SquareSynth, SweepActive, EnvelopeActive>,
  val tri: Channel<TriangleSynth, SweepInactive, EnvelopeInactive>,
  val noi: Channel<NoiseSynth, SweepInactive, EnvelopeActive>,
  val dmc: Channel<DmcSynth, SweepInactive, EnvelopeInactive>
) {
  constructor(memory: Memory) : this(
    sq1 = Timer().let { timer ->
      Channel(
        synth = SquareSynth(),
        timer = timer,
        sweep = SweepActive(timer, negateWithOnesComplement = false),
        envelope = EnvelopeActive()
      )
    },
    sq2 = Timer().let { timer ->
      Channel(
        synth = SquareSynth(),
        timer = timer,
        sweep = SweepActive(timer, negateWithOnesComplement = true),
        envelope = EnvelopeActive()
      )
    },
    tri = Channel(
      synth = TriangleSynth(),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeInactive(1)
    ),
    noi = Channel(
      synth = NoiseSynth(),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeActive()
    ),
    dmc = Channel(
      synth = DmcSynth(memory = memory),
      timer = Timer(),
      sweep = SweepInactive(),
      envelope = EnvelopeInactive(1)
    )
  )
}
