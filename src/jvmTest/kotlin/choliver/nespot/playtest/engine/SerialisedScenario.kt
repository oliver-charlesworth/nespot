package choliver.nespot.playtest.engine

import choliver.nespot.nes.Joypads.Button
import choliver.nespot.playtest.engine.SerialisedScenario.Stimulus.*
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

internal data class SerialisedScenario(
  val romHash: String,
  val stimuli: List<Stimulus>
) {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  @JsonSubTypes(
    JsonSubTypes.Type(ButtonDown::class, name = "buttonDown"),
    JsonSubTypes.Type(ButtonUp::class, name = "buttonUp"),
    JsonSubTypes.Type(Snapshot::class, name = "snapshot"),
    JsonSubTypes.Type(Close::class, name = "close")
  )
  sealed class Stimulus {
    abstract val timestamp: Long

    data class ButtonDown(override val timestamp: Long, val button: Button) : Stimulus()
    data class ButtonUp(override val timestamp: Long, val button: Button) : Stimulus()
    data class Snapshot(override val timestamp: Long, val hash: String) : Stimulus()
    data class Close(override val timestamp: Long) : Stimulus()
  }
}
