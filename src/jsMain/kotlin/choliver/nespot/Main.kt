package choliver.nespot

import choliver.nespot.worker.EmulatorWorker
import org.w3c.dom.Worker
import kotlin.browser.window

fun main() {
  if (inWorker()) {
    EmulatorWorker.createFor("/smb.nes")
  } else {
    val worker = Worker("/nespot.js")
    JsRunner(worker).run()
  }
}

private fun inWorker() = try {
  window
  false
} catch (t: Throwable) {
  true
}
