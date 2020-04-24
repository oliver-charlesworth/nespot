package choliver.nespot.apu

interface Generator {
  fun generate(ticks: Sequencer.Ticks): Int
}
