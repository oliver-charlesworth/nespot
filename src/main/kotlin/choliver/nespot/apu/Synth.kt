package choliver.nespot.apu

interface Synth {
  var length: Int

  fun take(counterTicks: Int, seqTicks: Sequencer.Ticks): Int
}
