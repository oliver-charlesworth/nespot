package choliver.nespot

interface AudioSink {
  val sampleRateHz get() = 44100    // Don't care
  fun put(sample: Float) {}
}
