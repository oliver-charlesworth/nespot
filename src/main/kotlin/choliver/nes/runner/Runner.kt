package choliver.nes.runner

import choliver.nes.Nes
import choliver.nes.debugger.FakeJoypads
import choliver.nes.debugger.Screen

class Runner(
  rom: ByteArray
) {
  private val joypads = FakeJoypads()
  private val screen = Screen(
    onButtonDown = { joypads.down(1, it) },
    onButtonUp = { joypads.up(1, it) }
  )
  private val nes = Nes(
    rom,
    screen.buffer,
    joypads,
    onNmi = { screen.redraw() }
  )

  fun start() {
    screen.show()
    nes.inspection.fireReset()
    while (true) {
      nes.inspection.step()
    }
  }
}
