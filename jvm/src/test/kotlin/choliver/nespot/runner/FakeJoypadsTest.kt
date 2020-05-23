package choliver.nespot.runner

import choliver.nespot.Data
import choliver.nespot.nes.Joypads
import choliver.nespot.nes.Joypads.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FakeJoypadsTest {
  private val joypads = FakeJoypads()

  @Test
  fun `state change before sampling is visible`() {
    joypads.down(1, Button.A)
    transparent()
    opaque()

    assertStatus(Joypads::read1, Button.A)
  }

  @Test
  fun `state change after sampling not visible`() {
    transparent()
    opaque()
    joypads.down(1, Button.A)

    assertStatus(Joypads::read1)
  }

  @Test
  fun `state change during sampling is visible`() {
    transparent()
    joypads.down(1, Button.A)
    opaque()

    assertStatus(Joypads::read1, Button.A)
  }

  @Test
  fun `reading without sampling results in zero`() {
    joypads.down(1, Button.A)

    assertStatus(Joypads::read1)
  }

  @Test
  fun `reading after already read results in zero`() {
    joypads.down(1, Button.A)
    transparent()
    opaque()

    assertStatus(Joypads::read1, Button.A)
    assertStatus(Joypads::read1)    // Additional read has wiped out state
  }

  @Test
  fun `final state before sampling is persisted`() {
    joypads.down(1, Button.A)
    joypads.down(1, Button.B)
    joypads.up(1, Button.A)
    transparent()
    opaque()

    assertStatus(Joypads::read1, Button.B)
  }

  @Test
  fun `final state during sampling is persisted`() {
    transparent()
    joypads.down(1, Button.A)
    joypads.down(1, Button.B)
    joypads.up(1, Button.A)
    opaque()

    assertStatus(Joypads::read1, Button.B)
  }

  @Test
  fun `all buttons on joypad 1 work`() {
    assertAllButtonsWork(1, Joypads::read1)
  }

  @Test
  fun `all buttons on joypad 2 work`() {
    assertAllButtonsWork(2, Joypads::read2)
  }

  private fun assertAllButtonsWork(which: Int, read: Joypads.() -> Data) {
    val set = mutableListOf<Button>()

    Button.values().forEach { button ->
      joypads.down(which, button)
      transparent()
      opaque()

      set += button
      assertStatus(read, *set.toTypedArray())
    }

    Button.values().forEach { button ->
      joypads.up(which, button)
      transparent()
      opaque()

      set -= button
      assertStatus(read, *set.toTypedArray())
    }
  }

  private fun opaque() {
    joypads.write(0x00)
  }

  private fun transparent() {
    joypads.write(0x01)
  }

  private fun assertStatus(read: Joypads.() -> Data, vararg set: Button) {
    Button.values().forEach { button ->
      val expected = if (button in set) 1 else 0
      assertEquals(expected, read(joypads), button.name)
    }
  }
}
