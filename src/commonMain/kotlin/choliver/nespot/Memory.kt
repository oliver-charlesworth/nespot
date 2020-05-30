package choliver.nespot

// TODO - should Memory be responsible for wrapping OOB addresses?
interface Memory {
  operator fun get(addr: Address): Data
  operator fun set(addr: Address, data: Data) {}
}
