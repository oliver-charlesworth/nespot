package choliver.sixfiveohtwo

import choliver.sixfiveohtwo.AddressMode.*
import choliver.sixfiveohtwo.AddressMode.IndexSource.*
import choliver.sixfiveohtwo.Opcode.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatterTest {
  @Test
  fun modes() {
    assertEquals("TSX", format(TSX, Implied))
    assertEquals("ASL A", format(ASL, Accumulator))
    assertEquals("BEQ $34", format(BEQ, Relative(0x34)))
    assertEquals("ADC #$34", format(ADC, Immediate(0x34.u8())))
    assertEquals("ADC $34", format(ADC, ZeroPage(0x34.u8())))
    assertEquals("ADC $34,X", format(ADC, ZeroPageIndexed(0x34.u8(), X)))
    assertEquals("ADC $0034", format(ADC, Absolute(0x0034.u16())))
    assertEquals("ADC $0034,Y", format(ADC, AbsoluteIndexed(0x0034.u16(), Y)))
    assertEquals("JMP ($0034)", format(JMP, Indirect(0x0034.u16())))
    assertEquals("ADC ($34,X)", format(ADC, IndexedIndirect(0x34.u8())))
    assertEquals("ADC ($34),Y", format(ADC, IndirectIndexed(0x34.u8())))
  }

  private fun format(op: Opcode, mode: AddressMode) = "${op.name}${format(mode)}"

  private fun format(mode: AddressMode) = when (mode) {
    is Implied -> ""
    is Accumulator -> " A"
    is Relative -> " $%02x".format(mode.offset)
    is Immediate -> " #$%02x".format(mode.literal.toByte())
    is ZeroPage -> " $%02x".format(mode.address.toByte())
    is ZeroPageIndexed -> " $%02x,%s".format(mode.address.toByte(), mode.source.name)
    is Absolute -> " $%04x".format(mode.address.toShort())
    is AbsoluteIndexed -> " $%04x,%s".format(mode.address.toShort(), mode.source.name)
    is Indirect -> " ($%04x)".format(mode.address.toShort())
    is IndexedIndirect -> " ($%02x,X)".format(mode.address.toByte())
    is IndirectIndexed -> " ($%02x),Y".format(mode.address.toByte())
  }
}
