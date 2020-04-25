package choliver.nespot.apu

import choliver.nespot.apu.Sequencer.Ticks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoiseSynthTest {
  private val synth = NoiseSynth(cyclesPerSample = 4.toRational()).apply {
    volume = 1
    length = 1
    periodCycles = 4.toRational()
  }

  // TODO - should the frequency be halved (i.e. clocked by APU rather than CPU?)

  @Test
  fun `full-length sequence`() {
    // Sequence is too long to test the whole thing against golden vector
    val seq = synth.take(  32767)
    val seq2 = synth.take(32767)

    assertEquals(
      listOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0),
      seq.take(32)
    )
    assertEquals(16384, seq.count { it == 1 })  // Property of LFSR
    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun `short sequence`() {
    synth.mode = 1
    val seq = synth.take(  93)
    val seq2 = synth.take(93)

    assertEquals(seq, seq2) // Should repeat
  }

  @Test
  fun `different period`() {
    synth.mode = 1    // Use short sequence because easier to test
    val ref = synth.take(  93)

    synth.periodCycles = 16.toRational()
    val seq = synth.take(93 * 4)

    assertEquals(
      ref.flatMap { listOf(it).repeat(4) },
      seq
    ) // Long-period sequence should match the padded short-period sequence
  }

  @Test
  fun volume() {
    synth.volume = 5
    val seq = synth.take(  32767)

    assertEquals(setOf(0, 5), seq.distinct().toSet())
  }

  @Test
  fun length() {
    synth.mode = 1    // Use short sequence because easier to test
    synth.length = 32
    val seq = synth.take(93, Ticks(quarter = 0, half = 1))

    assertEquals(setOf(0, 1), seq.take(32).distinct().toSet())
    assertEquals(setOf(0), seq.drop(32).distinct().toSet())  // Everything else is zeroed
  }
}
