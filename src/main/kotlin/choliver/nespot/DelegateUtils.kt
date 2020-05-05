package choliver.nespot

import kotlin.properties.Delegates.observable
import kotlin.properties.Delegates.vetoable

internal inline fun <T> observable(initialValue: T, crossinline onChange: (newValue: T) -> Unit) =
  observable(initialValue) { _, _, newValue -> onChange(newValue) }

internal inline fun <T> vetoable(initialValue: T, crossinline onChange: (newValue: T) -> Boolean) =
  vetoable(initialValue) { _, _, newValue -> onChange(newValue) }
