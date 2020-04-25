package choliver.nespot.apu

interface Synth {
  fun take(ticks: Sequencer.Ticks): Int
}
