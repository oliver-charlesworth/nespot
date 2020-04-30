package choliver.nespot

import choliver.nespot.sixfiveohtwo.model.Flags
import choliver.nespot.sixfiveohtwo.model.State
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test

class Farts {
  @Test
  fun farts() {
    val mapper = jacksonObjectMapper()

    val state = State(
      pc = 0x1234,
      a = 0x23,
      x = 0x34,
      y = 0x45,
      s = 0x56,
      p = Flags(
        n = true,
        v = true,
        d = true,
        i = true,
        z = true,
        c = true
      )
    )

    mapper.enable(SerializationFeature.INDENT_OUTPUT)
    println(mapper.writeValueAsString(state))
  }
}
