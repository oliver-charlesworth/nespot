package choliver.nes

interface Joypads {
  fun write(data: Data)
  fun read1(): Data
  fun read2(): Data
}
