package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import choliver.nespot.printInfo


class InfoRunner(private val rom: Rom) {
  fun run() {
    rom.printInfo()
  }
}
