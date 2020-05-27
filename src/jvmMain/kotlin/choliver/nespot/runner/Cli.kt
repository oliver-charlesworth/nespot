package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int


class Cli : CliktCommand(name = "nespot") {
  private val raw by argument(name = "rom").file(mustExist = true, canBeDir = false)
  private val displayInfo by option("--info", "-i").flag()
  private val numPerfFrames by option("--perf", "-p").int()
  private val fullScreen by option("--fullscreen", "-f").flag()
  private val snapshotFile by option("--snapshot", "-s").file(mustExist = true, canBeDir = false)

  override fun run() {
    val rom = Rom.parse(raw.readBytes())

    when {
      displayInfo -> InfoRunner(rom).run()
      (numPerfFrames != null) -> PerfRunner(rom, numPerfFrames!!).run()
      else -> InteractiveRunner(rom, snapshotFile, fullScreen).run()
    }
  }
}

fun main(args: Array<String>) = Cli().main(args)