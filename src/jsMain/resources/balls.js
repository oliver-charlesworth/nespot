class WhiteNoiseProcessor extends AudioWorkletProcessor {
  process (inputs, outputs, parameters) {
    return true
  }
}

console.log(typeof WhiteNoiseProcessor)
console.log(registerProcessor)
registerProcessor('balls-processor', WhiteNoiseProcessor)
