package choliver.nespot.dom

import org.w3c.dom.Navigator
import org.w3c.dom.events.Event

fun Navigator.getGamepads(): Array<Gamepad?> {
  val n = this
  return js("n.getGamepads()")
}

abstract external class GamepadEvent : Event {
  val gamepad: Gamepad
}

abstract external class Gamepad {
  val axes: DoubleArray
    get() = definedExternally
  val buttons: Array<GamepadButton>
    get() = definedExternally
  val connected: Boolean
    get() = definedExternally
  val id: String
  val index: Int
  val mapping: String
  val timestamp: Double
    get() = definedExternally
}

abstract external class GamepadButton {
  val value: Double
    get() = definedExternally
  val pressed: Boolean
    get() = definedExternally
}
