package choliver.nespot.runner

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.nes.Joypads.Button.*
import choliver.nespot.runner.Event.ControllerButtonDown
import choliver.nespot.runner.Event.ControllerButtonUp
import net.java.games.input.Component
import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import net.java.games.input.Event
import java.io.File
import java.util.*
import kotlin.concurrent.timerTask


// TODO - prevent gross logging
class ControllerManager(
  private val onEvent: (e: choliver.nespot.runner.Event) -> Unit = {}
) {
  private val controllers: Array<Controller>

  init {
    val dir = createTempDir()
    val lib = File(dir, "libjinput-osx.dylib")
    lib.outputStream().use {
      this.javaClass.getResourceAsStream("/libjinput-osx.jnilib").copyTo(it)
    }
    System.setProperty("net.java.games.input.librarypath", dir.absolutePath)
    controllers = ControllerEnvironment.getDefaultEnvironment().controllers
  }

  fun start() {
    Timer().scheduleAtFixedRate(timerTask {
      val event = Event()
      val x = AxisManager(LEFT, RIGHT)
      val y = AxisManager(UP, DOWN)
      while (true) {
        controllers.forEach { controller ->
          controller.poll()
          while (controller.eventQueue.getNextEvent(event)) {
            when (event.component.identifier) {
              Axis.X -> x.onEvent(event.value)
              Axis.Y -> y.onEvent(event.value)
              Component.Identifier.Button._1 -> onButtonEvent(event.value, A)
              Component.Identifier.Button._2 -> onButtonEvent(event.value, B)
              Component.Identifier.Button._8 -> onButtonEvent(event.value, SELECT)
              Component.Identifier.Button._9 -> onButtonEvent(event.value, START)
            }
          }
        }
      }
    }, 5, 5)
  }

  private fun onButtonEvent(value: Float, button: Button) {
    when (value) {
      1.0f -> onEvent(ControllerButtonDown(button))
      else -> onEvent(ControllerButtonUp(button))
    }
  }

  private inner class AxisManager(
    private val buttonLow: Button,
    private val buttonHigh: Button
  ) {
    private var prev = 0.0f

    fun onEvent(value: Float) {
      when (prev) {
        -1.0f -> onEvent(ControllerButtonUp(buttonLow))
        +1.0f -> onEvent(ControllerButtonUp(buttonHigh))
      }
      when (value) {
        -1.0f -> onEvent(ControllerButtonDown(buttonLow))
        +1.0f -> onEvent(ControllerButtonDown(buttonHigh))
      }
      prev = value
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val gash = ControllerManager(onEvent = { e ->
        when (e) {
          is ControllerButtonDown -> println("[${e.button}] -> down")
          is ControllerButtonUp -> println("[${e.button}] -> up")
        }
      })
      gash.start()
    }
  }

}
