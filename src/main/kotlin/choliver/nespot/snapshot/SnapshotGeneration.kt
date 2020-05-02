package choliver.nespot.snapshot

import choliver.nespot.Memory
import choliver.nespot.nes.Nes
import java.util.*

fun Nes.Inspection2.toSnapshot() = Snapshot(
  cpu = cpu.state,
  ppu = ppu.state,
  ram = encodeMemory(2048, ram),
  vram = encodeMemory(2048, vram),
  palette = encodeMemory(32, ppu.palette),
  oam = encodeMemory(256, ppu.oam)
)

fun Nes.Inspection2.fromSnapshot(snapshot: Snapshot) {
  cpu.state = snapshot.cpu
  ppu.state = snapshot.ppu
  decodeMemory(2048, ram, snapshot.ram)
  decodeMemory(2048, vram, snapshot.vram)
  decodeMemory(32, ppu.palette, snapshot.palette)
  decodeMemory(256, ppu.oam, snapshot.oam)
}

private fun encodeMemory(size: Int, memory: Memory) =
  Base64.getEncoder().encodeToString(ByteArray(size) { memory.load(it).toByte() })

private fun decodeMemory(size: Int, memory: Memory, snapshot: String) {
  val decoded = Base64.getDecoder().decode(snapshot)
  if (decoded.size != size) {
    throw IllegalArgumentException("Unexpected memory length")
  }
  decoded.forEachIndexed { addr, data -> memory.store(addr, data.toInt())  }
}
