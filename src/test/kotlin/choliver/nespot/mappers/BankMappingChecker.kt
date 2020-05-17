package choliver.nespot.mappers

import choliver.nespot.Address
import choliver.nespot.Data
import org.junit.jupiter.api.Assertions.assertEquals


internal class BankMappingChecker(
  private val bankSize: Int,
  private val srcBase: Address = 0x0000,
  private val outBase: Address = 0x0000,
  private val setSrc: (Address, Data) -> Unit,
  private val getOut: (Address) -> Data
) {
  fun assertMapping(srcBank: Int, outBank: Int) {
    val srcBankBase = srcBase + (srcBank * bankSize)
    val outBankBase = outBase + (outBank * bankSize)
    val offsetLast = bankSize - 1

    setSrc(srcBankBase, 0x30)
    setSrc(srcBankBase + offsetLast, 0x40)

    assertEquals(0x30, getOut(outBankBase))
    assertEquals(0x40, getOut(outBankBase + offsetLast))
  }

  companion object {
    internal fun takesBytes(setSrc: (Address, Byte) -> Unit) =
      { addr: Address, data: Data -> setSrc(addr, data.toByte()) }
  }
}
