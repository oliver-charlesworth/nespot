package choliver.nespot.snapshot

import choliver.nespot.nes.Sequencer.State as SequencerState
import choliver.nespot.ppu.model.State as PpuState
import choliver.nespot.sixfiveohtwo.model.State as CpuState

typealias Base64Encoded = String

data class Snapshot(
  // TODO - interrupts
  // TODO - cartridge
  // TODO - APU
  val sequencer: SequencerState = SequencerState(),
  val cpu: CpuState,
  val ppu: PpuState,

  val ram: Base64Encoded,
  val vram: Base64Encoded,
  val palette: Base64Encoded,
  val oam: Base64Encoded
)
