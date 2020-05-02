package choliver.nespot.snapshot

import choliver.nespot.ppu.model.State as PpuState
import choliver.nespot.sixfiveohtwo.model.State as CpuState

data class Snapshot(
  // TODO - interrupts
  // TODO - cartridge
  // TODO - APU
  val cpu: CpuState,
  val ppu: PpuState,

  val ram: List<Int>,
  val vram: List<Int>,
  val palette: List<Int>,
  val oam: List<Int>
)
