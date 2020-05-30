package choliver.nespot.apu

import choliver.nespot.Memory
import choliver.nespot.apu.FrameSequencer.Ticks

typealias SquareChannel = Channel<SquareSynth, SweepActive, EnvelopeActive>
typealias TriangleChannel = Channel<TriangleSynth, SweepInactive, EnvelopeInactive>
typealias NoiseChannel = Channel<NoiseSynth, SweepInactive, EnvelopeActive>
typealias DmcChannel = Channel<DmcSynth, SweepInactive, EnvelopeInactive>

class Channels(
  val seq: FrameSequencer,
  val sq1: SquareChannel,
  val sq2: SquareChannel,
  val tri: TriangleChannel,
  val noi: NoiseChannel,
  val dmc: DmcChannel
) {
  constructor(memory: Memory) : this(
    seq = FrameSequencer(),
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

  fun advance(numCycles: Int) {
    val ticks = seq.advance(numCycles)
    sq1.advance(numCycles, ticks)
    sq2.advance(numCycles, ticks)
    tri.advance(numCycles, ticks)
    noi.advance(numCycles, ticks)
    dmc.advance(numCycles, ticks)
  }

  fun Channel<*, *, *>.advance(numCycles: Int, ticks: Ticks) {
    if (ticks.quarter) {
      onQuarterFrame()
    }
    if (ticks.half) {
      onHalfFrame()
    }
    this.advance(numCycles)
  }
}
