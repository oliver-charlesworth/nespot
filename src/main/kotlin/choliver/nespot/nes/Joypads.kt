package choliver.nespot.nes

import choliver.nespot.Data

interface Joypads {
  fun write(data: Data)
  fun read1(): Data
  fun read2(): Data

  // See http://wiki.nesdev.com/w/index.php/Standard_controller#Report
  @Suppress("unused")
  enum class Button(val idx: Int) {
    A(0),
    B(1),
    SELECT(2),
    START(3),
    UP(4),
    DOWN(5),
    LEFT(6),
    RIGHT(7)
  }
}
