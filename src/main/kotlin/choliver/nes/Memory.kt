package choliver.nes

interface Memory {
  fun load(addr: Address): Data
  fun store(addr: Address, data: Data)
}
