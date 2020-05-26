package choliver.nespot

import ROM_PATH
import choliver.nespot.cartridge.Rom
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.Worker
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

fun main() {
  if (inWorker()) {
    self.fetch(ROM_PATH).then { response ->
      response.arrayBuffer().then { buffer ->
        val b2 = Int8Array(buffer)
        val array = ByteArray(buffer.byteLength) { b2[it] }
        Emulator(Rom.parse(array))
      }
    }

    Emulator.createFor("/smb3.nes")
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
