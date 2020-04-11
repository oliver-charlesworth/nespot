package choliver.nes.sixfiveohtwo

import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import choliver.nes.u16
import choliver.nes.u8
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatterTest {
  @Test
  fun modes() {
    assertEquals("TSX", Instruction(TSX, Implied).format())
    assertEquals("ASL A", Instruction(ASL, Accumulator).format())
    assertEquals("BEQ $34", Instruction(BEQ, Relative(0x34)).format())
    assertEquals("ADC #$34", Instruction(ADC, Immediate(0x34.u8())).format())
    assertEquals("ADC $34", Instruction(ADC, ZeroPage(0x34.u8())).format())
    assertEquals("ADC $34,X", Instruction(ADC, ZeroPageIndexed(0x34.u8(), X)).format())
    assertEquals("ADC $0034", Instruction(ADC, Absolute(0x0034.u16())).format())
    assertEquals("ADC $0034,Y", Instruction(ADC, AbsoluteIndexed(0x0034.u16(), Y)).format())
    assertEquals("JMP ($0034)", Instruction(JMP, Indirect(0x0034.u16())).format())
    assertEquals("ADC ($34,X)", Instruction(ADC, IndexedIndirect(0x34.u8())).format())
    assertEquals("ADC ($34),Y", Instruction(ADC, IndirectIndexed(0x34.u8())).format())
  }
}
