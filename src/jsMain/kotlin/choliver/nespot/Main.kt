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
    Emulator()
  } else {
    val script = document.getElementsByTagName("script")[0] as HTMLScriptElement
    JsRunner(
      worker = Worker(script.src),
      romPath = window.location.pathname + ".nes"
    ).run()
  }
}

private fun inWorker() = try {
  window
  false
} catch (t: Throwable) {
  true
}
