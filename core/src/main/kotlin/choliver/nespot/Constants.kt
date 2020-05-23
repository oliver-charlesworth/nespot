package choliver.nespot

const val SAMPLE_RATE_HZ = 44100

// See https://wiki.nesdev.com/w/index.php/Cycle_reference_chart
val MASTER_FREQ_HZ = Rational(945e6.toInt(), 4) / 11
val CPU_FREQ_HZ = MASTER_FREQ_HZ / 12

const val DOTS_PER_SCANLINE = 341
const val SCANLINES_PER_FRAME = 262
const val DOTS_PER_CYCLE = 3

val CYCLES_PER_SAMPLE = CPU_FREQ_HZ / Rational(SAMPLE_RATE_HZ)
