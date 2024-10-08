package choliver.nespot.ui

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Joypads.Button.*
import choliver.nespot.ui.Event.ControllerButtonDown
import choliver.nespot.ui.Event.ControllerButtonUp
import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import net.java.games.input.Event
import java.io.Closeable
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import net.java.games.input.Component.Identifier.Button as JInputButton


class ControllerManager(
  private val onEvent: (e: choliver.nespot.ui.Event) -> Unit = {},
  private val samplePeriodMs: Long = SAMPLE_PERIOD_MS
) : Closeable {
  private val controllers: Array<Controller>
  private val x = AxisManager(LEFT, RIGHT)
  private val y = AxisManager(UP, DOWN)
  private val timer = Timer()

  init {
    val dir = createTempDirectory()
    dir.toFile().deleteOnExit()

    copyFile("/libjinput-osx.jnilib", dir / "libjinput-osx.dylib")
    // Not actually 64-bit, but JInput thinks it should be called this
    copyFile("/libjinput-arm.so", dir / "libjinput-linux64.so")

    System.setProperty("jinput.loglevel", "OFF")
    System.setProperty("net.java.games.input.librarypath", dir.pathString)
    controllers = ControllerEnvironment.getDefaultEnvironment().controllers
  }

  private fun copyFile(src: String, target: Path) {
    target.outputStream().use {
      this.javaClass.getResourceAsStream(src)!!.copyTo(it)
    }
  }

  fun start() {
    timer.schedule(timerTask { onTimer() }, 0, samplePeriodMs)
  }

  override fun close() {
    timer.cancel()
  }

  private fun onTimer() {
    val event = Event()
    controllers.forEach { controller ->
      controller.poll()
      while (controller.eventQueue.getNextEvent(event)) {
        when (event.component.identifier) {
          Axis.X -> x.onEvent(event.value)
          Axis.Y -> y.onEvent(event.value)
          JInputButton._1, JInputButton.THUMB -> onButtonEvent(event.value, A)
          JInputButton._2, JInputButton.THUMB2 -> onButtonEvent(event.value, B)
          JInputButton._8, JInputButton.BASE3 -> onButtonEvent(event.value, SELECT)
          JInputButton._9, JInputButton.BASE4 -> onButtonEvent(event.value, START)
        }
      }
    }
  }

  private fun onButtonEvent(value: Float, button: Button) {
    when (value) {
      1.0f -> onEvent(ControllerButtonDown(button))
      else -> onEvent(ControllerButtonUp(button))
    }
  }

  private inner class AxisManager(private val buttonMin: Button, private val buttonMax: Button) {
    private var state = State.NEUTRAL

    fun onEvent(value: Float) {
      val nextState = when (value) {
        -1.0f -> State.MIN
        +1.0f -> State.MAX
        else -> State.NEUTRAL
      }

      if (nextState != state) {
        when (state) {
          State.MIN -> onEvent(ControllerButtonUp(buttonMin))
          State.MAX -> onEvent(ControllerButtonUp(buttonMax))
          else -> Unit
        }

        when (nextState) {
          State.MIN -> onEvent(ControllerButtonDown(buttonMin))
          State.MAX -> onEvent(ControllerButtonDown(buttonMax))
          else -> Unit
        }
      }

      state = nextState
    }
  }

  private enum class State {
    NEUTRAL,
    MIN,
    MAX
  }

  companion object {
    private const val SAMPLE_PERIOD_MS = 10L
  }
}
