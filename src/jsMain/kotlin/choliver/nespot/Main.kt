package choliver.nespot

import org.khronos.webgl.Float32Array
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

private const val processorName = "balls-processor"

external val self: AudioWorkletGlobalScope

fun jsTypeOf(o: Any): String {
  return js("typeof o")
}

fun main() {
  println("In main")

  val func = {
    class MyProcessor : AudioWorkletProcessor {
      override fun process(
        inputs: Array<Array<Float32Array>>,
        outputs: Array<Array<Float32Array>>,
        parameters: Map<String, Float32Array>
      ): Boolean {
        return false
      }
    }
    console.log("Ah herro")
    registerProcessor("balls-processor", ::MyProcessor)
    console.log("Ah goodbye")
//    console.log(self)
  }

  val blob = Blob(arrayOf("(${func})()"), BlobPropertyBag(type = "text/javascript"))

  console.log("blob", blob)

  val audioCtx = AudioContext()

  val url = URL.createObjectURL(blob)

  println(url)

  audioCtx.audioWorklet.addModule(url).then {
    val node = AudioWorkletNode(audioCtx, processorName)
    node.connect(audioCtx.destination)
  }
}
