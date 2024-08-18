package choliver.nespot

import choliver.nespot.ui.Ui
import choliver.nespot.worker.Worker
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.get
import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
  if (inWorker()) {
    Worker()
  } else {
    val script = document.getElementsByTagName("script")[0] as HTMLScriptElement
    Ui(
      worker = org.w3c.dom.Worker(script.src),
      romPath = window.location.pathname + ".nes"
    )
  }
}

private fun inWorker() = try {
  window
  false
} catch (t: Throwable) {
  true
}
