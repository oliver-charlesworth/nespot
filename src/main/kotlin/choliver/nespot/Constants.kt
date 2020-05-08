package choliver.nespot

const val SAMPLE_RATE_HZ = 44100

// See https://wiki.nesdev.com/w/index.php/Cycle_reference_chart
val MASTER_FREQ_HZ = Rational(945e6.toInt(), 4) / 11
val CPU_FREQ_HZ = MASTER_FREQ_HZ / 12

// TODO - model skipping one dot every other frame
const val DOTS_PER_SCANLINE = 341
const val SCANLINES_PER_FRAME = 262
val DOTS_PER_FRAME = Rational(DOTS_PER_SCANLINE * SCANLINES_PER_FRAME) - Rational(1, 2)
val CYCLES_PER_FRAME = DOTS_PER_FRAME / 3
val CYCLES_PER_SCANLINE = CYCLES_PER_FRAME / SCANLINES_PER_FRAME

val CYCLES_PER_SAMPLE = CPU_FREQ_HZ / Rational(SAMPLE_RATE_HZ)
