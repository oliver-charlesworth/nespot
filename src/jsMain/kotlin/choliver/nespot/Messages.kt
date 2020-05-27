package choliver.nespot

import org.w3c.dom.MessageEvent

data class Config(
  val romPath: String,
  val sampleRateHz: Int
) {
  constructor(obj: dynamic) : this(obj.romPath, obj.sampleRateHz)
}

// Main to worker
const val MSG_CONFIGURE = "CONFIGURE"
const val MSG_EMULATE_UNTIL = "EMULATE_UNTIL"
const val MSG_BUTTON_DOWN = "BUTTON_DOWN"
const val MSG_BUTTON_UP = "BUTTON_UP"

// Worker to main
const val MSG_ALIVE = "ALIVE"
const val MSG_VIDEO_FRAME = "VIDEO_FRAME"
const val MSG_AUDIO_CHUNK = "AUDIO_CHUNK"

fun messageHandler(block: (type: String, payload: Any?) -> Unit) = { e: MessageEvent ->
  val msg = e.data as Array<*>
  block(msg[0] as String, msg[1])
}
