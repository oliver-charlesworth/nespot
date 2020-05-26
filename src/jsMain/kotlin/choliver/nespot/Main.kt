package choliver.nespot

import choliver.nespot.emulator.Emulator
import choliver.nespot.runner.JsRunner
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.Worker
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

fun main() {
  if (inWorker()) {
    Emulator.createFor(ROM_URL)
  } else {
    val script = document.getElementsByTagName("script")[0] as HTMLScriptElement
    val worker = Worker(script.src)
    JsRunner(worker).run()
  }
}

private fun inWorker() = try {
  window
  false
} catch (t: Throwable) {
  true
}
