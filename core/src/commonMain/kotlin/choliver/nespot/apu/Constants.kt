package choliver.nespot.apu

// See https://wiki.nesdev.com/w/index.php/APU_Frame_Counter
internal const val FRAME_SEQUENCER_4_STEP_PERIOD_CYCLES = 29830
internal const val FRAME_SEQUENCER_5_STEP_PERIOD_CYCLES = 37282

// See http://wiki.nesdev.com/w/index.php/APU_Length_Counter
internal val LENGTH_TABLE = listOf(
  10,
  254,
  20,
  2,
  40,
  4,
  80,
  6,
  160,
  8,
  60,
  10,
  14,
  12,
  26,
  14,
  12,
  16,
  24,
  18,
  48,
  20,
  96,
  22,
  192,
  24,
  72,
  26,
  16,
  28,
  32,
  30
)
