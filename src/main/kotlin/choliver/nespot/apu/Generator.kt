package choliver.nespot.apu

interface Generator {
  fun take(ticks: Sequencer.Ticks): Int
}
