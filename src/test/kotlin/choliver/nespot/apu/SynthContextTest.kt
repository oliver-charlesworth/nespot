package choliver.nespot.apu

import choliver.nespot.apu.FrameSequencer.Ticks
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SynthContextTest {
  private val synth = mock<Synth>()
  private val sweep = mock<Sweep>()
  private val envelope = mock<Envelope>()
  private val timer = mock<Counter>()
  private val ctx = SynthContext(
    synth = synth,
    sweep = sweep,
    envelope = envelope,
    timer = timer
  )

  @Test
  fun `prevents length setting if disabled`() {
    ctx.enabled = false
    ctx.length = 3

    verify(synth, never()).length = 3
  }

  @Test
  fun `allows length setting if enabled`() {
    ctx.enabled = true
    ctx.length = 3

    verify(synth).length = 3
  }

  @Test
  fun `sets length to zero when disabled`() {
    ctx.enabled = false

    verify(synth).length = 0
  }

  @Test
  fun `applies level to synth output`() {
    whenever(sweep.mute) doReturn false
    whenever(synth.output) doReturn 5
    whenever(envelope.level) doReturn 3

    assertEquals(15, ctx.take(Ticks(quarter = false, half = false)))
  }

  @Test
  fun `mutes synth output`() {
    whenever(sweep.mute) doReturn true
    whenever(synth.output) doReturn 5
    whenever(envelope.level) doReturn 3

    assertEquals(0, ctx.take(Ticks(quarter = false, half = false)))
  }

  @Test
  fun `advances synth under timer control`() {
    whenever(timer.take()) doReturn 3

    ctx.take(Ticks(quarter = false, half = false))

    verify(synth, times(3)).onTimer()
  }

  @Test
  fun `invokes envelope and synth on quarter frame`() {
    ctx.take(Ticks(quarter = true, half = false))

    verify(synth).onQuarterFrame()
    verify(envelope).advance()
  }

  @Test
  fun `invokes sweep and synth on quarter frame`() {
    ctx.take(Ticks(quarter = false, half = true))

    verify(synth).onHalfFrame()
    verify(sweep).advance()
  }
}
