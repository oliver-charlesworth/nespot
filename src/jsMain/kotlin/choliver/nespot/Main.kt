package choliver.nespot

import choliver.nespot.cartridge.Rom
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.browser.window

fun main() {
  window.fetch("/smb3.nes").then { response ->
    response.arrayBuffer().then { buffer ->
      val b2 = Int8Array(buffer)
      val array = ByteArray(buffer.byteLength) { b2[it] }
      JsRunner(Rom.parse(array)).run()
    }
  }
}
