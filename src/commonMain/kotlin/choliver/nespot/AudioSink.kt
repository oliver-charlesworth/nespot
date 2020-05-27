package choliver.nespot

interface AudioSink {
  val sampleRateHz: Int
  fun put(sample: Float)
}
