package choliver.nespot

const val AUDIO_BUFFER_LENGTH_SECONDS = 0.025
const val AUDIO_BUFFER_AHEAD_SECONDS = 0.075

const val VISIBLE_WIDTH = SCREEN_WIDTH
const val VISIBLE_HEIGHT = (SCREEN_HEIGHT - 2 * TILE_SIZE)  // Crop top and bottom
const val RATIO_STRETCH = (8.0 / 7.0)    // Evidence in forums, etc. that PAR is 8/7, and it looks good
