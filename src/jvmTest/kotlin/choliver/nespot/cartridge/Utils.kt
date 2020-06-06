package choliver.nespot.cartridge

import choliver.nespot.common.Address
import choliver.nespot.common.Data
import org.junit.jupiter.api.Assertions.assertEquals

internal class BankMappingChecker(
  private val bankSize: Int,
  private val srcBase: Address = 0x0000,
  private val outBase: Address = 0x0000,
  private val setSrc: (Address, Data) -> Unit,
  private val getOut: (Address) -> Data
) {
  fun assertMappings(vararg srcToOutMappings: Pair<Int, Int>, dataOffset: Int = 0) {
    srcToOutMappings.forEachIndexed { idx, (src, out) ->
      assertMapping(srcBank = src, outBank = out, dataOffset = dataOffset + idx, desc = "src: ${src}, out: ${out}")
    }
  }

  private fun assertMapping(srcBank: Int, outBank: Int, dataOffset: Int, desc: String) {
    val srcBankBase = srcBase + (srcBank * bankSize)
    val outBankBase = outBase + (outBank * bankSize)
    val offsetLast = bankSize - 1

    setSrc(srcBankBase, 0x30 + dataOffset)
    setSrc(srcBankBase + offsetLast, 0x40 + dataOffset)

    assertEquals(0x30 + dataOffset, getOut(outBankBase), "[${desc}] low")
    assertEquals(0x40 + dataOffset, getOut(outBankBase + offsetLast), "[${desc}] high")
  }

  companion object {
    internal fun takesBytes(setSrc: (Address, Byte) -> Unit) =
      { addr: Address, data: Data -> setSrc(addr, data.toByte()) }
  }
}
