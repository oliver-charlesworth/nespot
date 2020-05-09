package choliver.nespot.persistence

import choliver.nespot.ppu.Renderer.State as RendererState
import choliver.nespot.ppu.model.State as PpuState
import choliver.nespot.sixfiveohtwo.model.State as CpuState

typealias Base64Encoded = String

data class Snapshot(
  // TODO - cartridge
  // TODO - APU
  val cpu: CpuState,
  val ppu: PpuState,
  val renderer: RendererState,

  val ram: Base64Encoded,
  val vram: Base64Encoded,
  val palette: Base64Encoded,
  val oam: Base64Encoded
)
