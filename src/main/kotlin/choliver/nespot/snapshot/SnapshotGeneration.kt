package choliver.nespot.snapshot

import choliver.nespot.Memory
import choliver.nespot.nes.Nes
import java.util.*

fun Nes.Inspection2.snapshot() = Snapshot(
  cpu = cpu.state,
  ppu = ppu.state,
  ram = encodeMemory(2048, ram),
  vram = encodeMemory(2048, vram),
  palette = encodeMemory(32, ppu.palette),
  oam = encodeMemory(256, ppu.oam)
)

private fun encodeMemory(size: Int, memory: Memory) =
  Base64.getEncoder().encodeToString(ByteArray(size) { memory.load(it).toByte() })
