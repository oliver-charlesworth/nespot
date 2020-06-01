package choliver.nespot.runner

import choliver.nespot.nes.Joypads
import choliver.nespot.runner.TimestampedEvent.Event.*
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class TimestampedEvent(
  val timestamp: Long,
  val event: Event
) {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
  @JsonSubTypes(
    JsonSubTypes.Type(ButtonDown::class, name = "buttonDown"),
    JsonSubTypes.Type(ButtonUp::class, name = "buttonUp"),
    JsonSubTypes.Type(Snapshot::class, name = "snapshot"),
    JsonSubTypes.Type(Close::class, name = "close")
  )
  sealed class Event {
    data class ButtonDown(val button: Joypads.Button) : Event()
    data class ButtonUp(val button: Joypads.Button) : Event()
    data class Snapshot(val data: List<Int>) : Event()
    object Close : Event()
  }
}
