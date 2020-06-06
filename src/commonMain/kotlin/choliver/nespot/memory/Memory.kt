package choliver.nespot.memory

import choliver.nespot.common.Address
import choliver.nespot.common.Data

// TODO - should Memory be responsible for wrapping OOB addresses?
interface Memory {
  operator fun get(addr: Address): Data
  operator fun set(addr: Address, data: Data) {}
}
