package choliver.nespot

// TODO - should Memory be responsible for wrapping OOB addresses?
interface Memory {
  fun load(addr: Address): Data
  fun store(addr: Address, data: Data)
}
