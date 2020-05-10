package choliver.nespot.ppu

import choliver.nespot.ppu.model.Coords
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoordsTest {
  @Test
  fun `increments x components`() {
    assertEquals(
      Coords(xFine = 1),
      Coords(xFine = 0).incrementX()
    )
    assertEquals(
      Coords(xCoarse = 1, xFine = 0),
      Coords(xCoarse = 0, xFine = 7).incrementX()
    )
    assertEquals(
      Coords(nametable = 1, xCoarse = 0, xFine = 0),
      Coords(nametable = 0, xCoarse = 31, xFine = 7).incrementX()
    )
    assertEquals(
      Coords(nametable = 0, xCoarse = 0, xFine = 0),
      Coords(nametable = 1, xCoarse = 31, xFine = 7).incrementX()
    )
  }

  @Test
  fun `increments y components`() {
    assertEquals(
      Coords(yFine = 1),
      Coords(yFine = 0).incrementY()
    )
    assertEquals(
      Coords(yCoarse = 1, yFine = 0),
      Coords(yCoarse = 0, yFine = 7).incrementY()
    )
    assertEquals(
      Coords(nametable = 2, yCoarse = 0, yFine = 0),
      Coords(nametable = 0, yCoarse = 29, yFine = 7).incrementY()
    )
    assertEquals(
      Coords(nametable = 0, yCoarse = 0, yFine = 0),
      Coords(nametable = 2, yCoarse = 29, yFine = 7).incrementY()
    )
    // Some weird special cases
    assertEquals(
      Coords(yCoarse = 31, yFine = 0),
      Coords(yCoarse = 30, yFine = 7).incrementY()
    )
    assertEquals(
      Coords(yCoarse = 0, yFine = 0),
      Coords(yCoarse = 31, yFine = 7).incrementY()
    )
  }
}
