package choliver.nespot.apu

import choliver.nespot.apu.FrameSequencer.Ticks
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SynthContextTest {
  private val synth = mock<Synth>()
  private val sweep = mock<Sweep>()
  private val envelope = mock<Envelope>()
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

    verify(synth).onTimer(3)
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