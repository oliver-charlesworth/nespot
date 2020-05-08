package choliver.nespot.persistence

import choliver.nespot.Memory
import choliver.nespot.data
import choliver.nespot.nes.Nes
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.util.*


class SnapshotManager(private val nes: Nes.Diagnostics) {
  private val mapper = jacksonObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)

  fun snapshotToStdout() {
    println(mapper.writeValueAsString(snapshot()))
  }

  fun snapshot() = Snapshot(
    cpu = nes.cpu.state,
    ppu = nes.ppu.state,
    ram = encodeMemory(2048, nes.ram),
    vram = encodeMemory(2048, nes.vram),
    palette = encodeMemory(32, nes.ppu.palette),
    oam = encodeMemory(256, nes.ppu.oam)
  )

  fun restore(file: File) {
    restore(mapper.readValue<Snapshot>(file))
  }

  fun restore(snapshot: Snapshot) {
    nes.cpu.state = snapshot.cpu
    nes.ppu.state = snapshot.ppu
    decodeMemory(2048, nes.ram, snapshot.ram)
    decodeMemory(2048, nes.vram, snapshot.vram)
    decodeMemory(32, nes.ppu.palette, snapshot.palette)
    decodeMemory(256, nes.ppu.oam, snapshot.oam)
  }

  private fun encodeMemory(size: Int, memory: Memory) =
    Base64.getEncoder().encodeToString(ByteArray(size) { memory[it].toByte() })

  private fun decodeMemory(size: Int, memory: Memory, snapshot: String) {
    val decoded = Base64.getDecoder().decode(snapshot)
    if (decoded.size != size) {
      throw IllegalArgumentException("Unexpected memory length")
    }
    decoded.forEachIndexed { addr, data -> memory[addr] = data.data() }
  }
}

