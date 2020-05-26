package choliver.nespot.worker

import org.w3c.dom.MessageEvent

const val MSG_EMULATE_UNTIL = "EMULATE_UNTIL"
const val MSG_BUTTON_DOWN = "BUTTON_DOWN"
const val MSG_BUTTON_UP = "BUTTON_UP"
const val MSG_VIDEO_FRAME = "VIDEO_FRAME"

fun messageHandler(block: (type: String, payload: Any?) -> Unit) = { e: MessageEvent ->
  val msg = e.data as Array<*>
  block(msg[0] as String, msg[1])
}
