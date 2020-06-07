package choliver.nespot.ui

import choliver.nespot.RATIO_STRETCH
import choliver.nespot.VISIBLE_HEIGHT
import choliver.nespot.VISIBLE_WIDTH
import kotlin.math.max
import kotlin.math.min

class DisplayInfo(targetWidth: Double, targetHeight: Double) {
  constructor(scale: Double) : this(
    VISIBLE_WIDTH * scale * RATIO_STRETCH,
    VISIBLE_HEIGHT * scale
  )

  val sourceWidth = VISIBLE_WIDTH.toDouble()
  val sourceHeight = VISIBLE_HEIGHT.toDouble()

  val scaleVertical = min(
    targetWidth / (sourceWidth * RATIO_STRETCH),
    targetHeight / sourceHeight
  )
  val scaleHorizontal = scaleVertical * RATIO_STRETCH

  val resultWidth = scaleHorizontal * sourceWidth
  val resultHeight = scaleVertical * sourceHeight

  val marginHorizontal = max(0.0, (targetWidth - resultWidth) / 2)
  val marginVertical = max(0.0, (targetHeight - resultHeight) / 2)
}
