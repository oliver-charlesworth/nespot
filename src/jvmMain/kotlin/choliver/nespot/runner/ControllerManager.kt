package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Joypads.Button.*
import choliver.nespot.runner.Event.ControllerButtonDown
import choliver.nespot.runner.Event.ControllerButtonUp
import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import net.java.games.input.Event
import java.io.File
import java.util.*
import kotlin.concurrent.timerTask
import net.java.games.input.Component.Identifier.Button as JInputButton


class ControllerManager(
  private val onEvent: (e: choliver.nespot.runner.Event) -> Unit = {},
  private val samplePeriodMs: Long = SAMPLE_PERIOD_MS
) {
  private val controllers: Array<Controller>
  private val x = AxisManager(LEFT, RIGHT)
  private val y = AxisManager(UP, DOWN)
  private val timer = Timer()

  init {
    val dir = createTempDir()
    dir.deleteOnExit()

    copyFile("/libjinput-osx.jnilib", File(dir, "libjinput-osx.dylib"))
    // Not actually 64-bit, but JInput thinks it should be called this
    copyFile("/libjinput-arm.so", File(dir, "libjinput-linux64.so"))

    System.setProperty("jinput.loglevel", "OFF")
    System.setProperty("net.java.games.input.librarypath", dir.absolutePath)
    controllers = ControllerEnvironment.getDefaultEnvironment().controllers
  }

  private fun copyFile(src: String, target: File) {
    target.outputStream().use {
      this.javaClass.getResourceAsStream(src).copyTo(it)
    }
  }

  fun start() {
    timer.schedule(timerTask { onTimer() }, 0, samplePeriodMs)
  }

  fun exit() {
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
    private const val SAMPLE_PERIOD_MS = 1L
  }
}
