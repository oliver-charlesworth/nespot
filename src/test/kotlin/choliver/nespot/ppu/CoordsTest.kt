package choliver.nespot.ppu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoordsTest {
  @Test
  fun `increments x components`() {
    assertEquals(
      Coords(fineX = 1),
      Coords(fineX = 0).incrementX()
    )
    assertEquals(
      Coords(coarseX = 1, fineX = 0),
      Coords(coarseX = 0, fineX = 7).incrementX()
    )
    assertEquals(
      Coords(nametableX = 1, coarseX = 0, fineX = 0),
      Coords(nametableX = 0, coarseX = 31, fineX = 7).incrementX()
    )
    assertEquals(
      Coords(nametableX = 0, coarseX = 0, fineX = 0),
      Coords(nametableX = 1, coarseX = 31, fineX = 7).incrementX()
    )
  }

  @Test
  fun `increments y components`() {
    assertEquals(
      Coords(fineY = 1),
      Coords(fineY = 0).incrementY()
    )
    assertEquals(
      Coords(coarseY = 1, fineY = 0),
      Coords(coarseY = 0, fineY = 7).incrementY()
    )
    assertEquals(
      Coords(nametableY = 1, coarseY = 0, fineY = 0),
      Coords(nametableY = 0, coarseY = 29, fineY = 7).incrementY()
    )
    assertEquals(
      Coords(nametableY = 0, coarseY = 0, fineY = 0),
      Coords(nametableY = 1, coarseY = 29, fineY = 7).incrementY()
    )
    // Some weird special cases
    assertEquals(
      Coords(coarseY = 31, fineY = 0),
      Coords(coarseY = 30, fineY = 7).incrementY()
    )
    assertEquals(
      Coords(coarseY = 0, fineY = 0),
      Coords(coarseY = 31, fineY = 7).incrementY()
    )
  }
}
