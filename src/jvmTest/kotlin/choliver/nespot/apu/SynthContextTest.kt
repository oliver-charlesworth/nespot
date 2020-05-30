package choliver.nespot.apu

import choliver.nespot.apu.FrameSequencer.Ticks
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SynthContextTest {
  private val synth = mock<Synth>()
  private val sweep = mock<SweepActive>()
  private val envelope = mock<EnvelopeActive>()
  private val timer = mock<Timer>()
  private val ctx = SynthContext(
    synth = synth,
    sweep = sweep,
    envelope = envelope,
    timer = timer
  )

  @Test
  fun `applies level to synth output`() {
    whenever(sweep.mute) doReturn false
    whenever(synth.output) doReturn 5
    whenever(envelope.level) doReturn 3

    assertEquals(15, ctx.current)
  }

  @Test
  fun `mutes synth output`() {
    whenever(sweep.mute) doReturn true
    whenever(synth.output) doReturn 5
    whenever(envelope.level) doReturn 3

    assertEquals(0, ctx.current)
  }

  @Test
  fun `advances synth under timer control`() {
    whenever(timer.advance(any())) doReturn 3

    ctx.advance(2, Ticks(quarter = false, half = false))

    verify(synth).onTimer(3)
  }

  @Test
  fun `invokes envelope and synth on quarter frame`() {
    ctx.advance(0, Ticks(quarter = true, half = false))

    verify(synth).onQuarterFrame()
    verify(envelope).advance()
  }

  @Test
  fun `invokes sweep and synth on quarter frame`() {
    ctx.advance(0, Ticks(quarter = false, half = true))

    verify(synth).onHalfFrame()
    verify(sweep).advance()
  }
}
