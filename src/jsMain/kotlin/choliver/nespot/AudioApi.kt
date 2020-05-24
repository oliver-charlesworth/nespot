package choliver.nespot

import org.khronos.webgl.Float32Array

abstract external class AudioBuffer {
  val sampleRate: Double
  val length: Int
  val duration: Double
  val numberOfChannels: Int
  fun getChannelData(channel: Int): Float32Array
}

external interface AudioNode {
  fun connect(destination: AudioNode)
}

external interface AudioScheduledSourceNode : AudioNode {
  fun start(`when`: Double = definedExternally, offset: Double = definedExternally, duration: Double = definedExternally)
}

abstract external class AudioBufferSourceNode : AudioScheduledSourceNode {
  var buffer: AudioBuffer?
}

external class AudioContext {
  val sampleRate: Double
  val destination: AudioNode
  val currentTime: Double
  fun createBuffer(numOfChannels: Int, length: Int, sampleRate: Double): AudioBuffer
  fun createBufferSource(): AudioBufferSourceNode
}
