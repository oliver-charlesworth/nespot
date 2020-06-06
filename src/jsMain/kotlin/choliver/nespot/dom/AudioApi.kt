package choliver.nespot.dom

import org.khronos.webgl.Float32Array
import org.w3c.dom.events.Event

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

abstract external class BaseAudioContext {
  val destination: AudioNode
  val sampleRate: Float
  val currentTime: Double
  fun createBuffer(numOfChannels: Int, length: Int, sampleRate: Float): AudioBuffer
  fun createBufferSource(): AudioBufferSourceNode
}

external class AudioContext : BaseAudioContext
