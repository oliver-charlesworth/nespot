package choliver.nespot.apu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class ChannelTest {
  private val synth = mock<Synth>()
  private val sweep = mock<SweepActive>()
  private val envelope = mock<EnvelopeActive>()
  private val timer = mock<Timer>()
  private val channel = Channel(
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

    assertEquals(15, channel.output)
  }

  @Test
  fun `mutes synth output`() {
    whenever(sweep.mute) doReturn true
    whenever(synth.output) doReturn 5
    whenever(envelope.level) doReturn 3

    assertEquals(0, channel.output)
  }

  @Test
  fun `advances synth under timer control`() {
    whenever(timer.advance(any())) doReturn 3

    channel.advance(2)

    verify(synth).onTimer(3)
  }

  @Test
  fun `invokes envelope and synth on quarter frame`() {
    channel.onQuarterFrame()

    verify(synth).onQuarterFrame()
    verify(envelope).advance()
  }

  @Test
  fun `invokes sweep and synth on half frame`() {
    channel.onHalfFrame()

    verify(synth).onHalfFrame()
    verify(sweep).advance()
  }
}
