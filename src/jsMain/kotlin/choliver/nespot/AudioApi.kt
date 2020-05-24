package choliver.nespot

import org.khronos.webgl.Float32Array
import org.w3c.dom.WorkerGlobalScope
import org.w3c.dom.events.Event
import kotlin.js.Promise

external interface AudioWorkletProcessor {
  // TODO - what about parameterDescriptors ?

  fun process(
    inputs: Array<Array<Float32Array>>,
    outputs: Array<Array<Float32Array>>,
    parameters: Map<String, Float32Array>
  ): Boolean
}

abstract external class AudioWorkletGlobalScope : WorkerGlobalScope {
  val currentFrame: Long
  val currentTime: Double
  val sampleRate: Float
  fun registerProcessor(name: String, processorCtor: JsClass<out AudioWorkletProcessor>)
}

external fun registerProcessor(name: String, processorCtor: () -> AudioWorkletProcessor)

abstract external class AudioBuffer {
  val sampleRate: Float
  val length: Int
  val duration: Double
  val numberOfChannels: Int
  fun getChannelData(channel: Int): Float32Array
}

abstract external class AudioNode {
  fun connect(destination: AudioNode)
}

abstract external class AudioScheduledSourceNode : AudioNode {
  var onended: ((Event) -> dynamic)
  fun start(`when`: Double = definedExternally)
}

abstract external class AudioBufferSourceNode : AudioScheduledSourceNode {
  var buffer: AudioBuffer?
}

external class AudioWorkletNode(context: BaseAudioContext, name: String) : AudioNode

abstract external class Worklet {
  fun addModule(moduleURL: String): Promise<Unit>
}

abstract external class AudioWorklet : Worklet

abstract external class BaseAudioContext {
  val destination: AudioNode
  val sampleRate: Float
  val currentTime: Double
  val audioWorklet: AudioWorklet
  fun createBuffer(numOfChannels: Int, length: Int, sampleRate: Float): AudioBuffer
  fun createBufferSource(): AudioBufferSourceNode
}

external class AudioContext : BaseAudioContext
