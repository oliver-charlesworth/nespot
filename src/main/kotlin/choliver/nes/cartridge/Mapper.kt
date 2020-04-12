package choliver.nes.cartridge

interface Mapper {
  val prg: PrgMemory
  val chr: ChrMemory
}
