package choliver.nespot

import java.security.MessageDigest

fun ByteArray.sha1(): String {
  val md = MessageDigest.getInstance("SHA-1")
  md.update(this)
  return md.digest().hex()
}

private fun ByteArray.hex() = joinToString("") { "%02X".format(it) }
