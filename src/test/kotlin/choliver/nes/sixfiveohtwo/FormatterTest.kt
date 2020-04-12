package choliver.nes.sixfiveohtwo

import choliver.nes.sixfiveohtwo.model.Instruction
import choliver.nes.sixfiveohtwo.model.Opcode.*
import choliver.nes.sixfiveohtwo.model.Operand.*
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.X
import choliver.nes.sixfiveohtwo.model.Operand.IndexSource.Y
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatterTest {
  @Test
  fun modes() {
    assertEquals("tsx", Instruction(TSX, Implied).format())
    assertEquals("asl A", Instruction(ASL, Accumulator).format())
    assertEquals("beq $34", Instruction(BEQ, Relative(0x34)).format())
    assertEquals("adc #$34", Instruction(ADC, Immediate(0x34)).format())
    assertEquals("adc $34", Instruction(ADC, ZeroPage(0x34)).format())
    assertEquals("adc $34,X", Instruction(ADC, ZeroPageIndexed(0x34, X)).format())
    assertEquals("adc $0034", Instruction(ADC, Absolute(0x0034)).format())
    assertEquals("adc $0034,Y", Instruction(ADC, AbsoluteIndexed(0x0034, Y)).format())
    assertEquals("jmp ($0034)", Instruction(JMP, Indirect(0x0034)).format())
    assertEquals("adc ($34,X)", Instruction(ADC, IndexedIndirect(0x34)).format())
    assertEquals("adc ($34),Y", Instruction(ADC, IndirectIndexed(0x34)).format())
  }
}
