package choliver.nespot.apu

interface Synth {
  var length: Int

  fun take(ticks: Sequencer.Ticks): Int
}
