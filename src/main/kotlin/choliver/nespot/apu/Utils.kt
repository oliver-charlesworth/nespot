package choliver.nespot.apu

import kotlin.properties.Delegates.observable

internal inline fun <T> observable(initialValue: T, crossinline onChange: (newValue: T) -> Unit) =
  observable(initialValue) { _, _, newValue -> onChange(newValue) }
