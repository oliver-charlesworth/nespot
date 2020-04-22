package choliver.nespot.apu

const val SAMPLE_RATE_HZ = 44100f

// See https://wiki.nesdev.com/w/index.php/Cycle_reference_chart
const val CPU_FREQ_HZ = (236.25e6 / 11) / 12

const val CLOCKS_PER_SAMPLE = CPU_FREQ_HZ.toDouble() / SAMPLE_RATE_HZ
const val INT_CLOCKS_PER_SAMPLE = CLOCKS_PER_SAMPLE.toInt()
const val RESIDUAL_CLOCKS_PER_SAMPLE = CLOCKS_PER_SAMPLE - INT_CLOCKS_PER_SAMPLE

// See https://wiki.nesdev.com/w/index.php/APU_Frame_Counter
const val FRAME_SEQUENCER_PERIOD_CYCLES = 29830
// TODO - 5-step sequence
