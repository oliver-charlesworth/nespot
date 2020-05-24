package choliver.nespot

import org.khronos.webgl.Float32Array

class MyProcessor : AudioWorkletProcessor {
  override fun process(
    inputs: Array<Array<Float32Array>>,
    outputs: Array<Array<Float32Array>>,
    parameters: Map<String, Float32Array>
  ): Boolean {
    return false
  }
}
