package choliver.nespot.runner

import choliver.nespot.cartridge.Rom


class InfoRunner(private val rom: Rom) {
  fun run() {
    rom.printInfo()
  }
}
