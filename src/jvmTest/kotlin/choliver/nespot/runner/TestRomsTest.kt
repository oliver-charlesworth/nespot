package choliver.nespot.runner

import choliver.nespot.cartridge.Rom
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

@Disabled("Intended to be run manually")
class TestRomsTest {
  @Test
  fun `branch_timing_tests - Branch_Basics`() = run("branch_timing_tests/1.Branch_Basics.nes")

  @Test
  fun `branch_timing_tests - Backward_Branch`() = run("branch_timing_tests/2.Backward_Branch.nes")

  @Test
  fun `branch_timing_tests - Forward_Branch`() = run("branch_timing_tests/3.Forward_Branch.nes")

  @Test
  fun cpu_dummy_reads() = run("cpu_dummy_reads/cpu_dummy_reads.nes")

  @Test
  fun `cpu_interrupts_v2 - cli_latency`() = run("cpu_interrupts_v2/rom_singles/1-cli_latency.nes")

  @Test
  fun `cpu_interrupts_v2 - nmi_and_brk`() = run("cpu_interrupts_v2/rom_singles/2-nmi_and_brk.nes")

  @Test
  fun `cpu_interrupts_v2 - nmi_and_irq`() = run("cpu_interrupts_v2/rom_singles/3-nmi_and_irq.nes")

  @Test
  fun `cpu_interrupts_v2 - irq_and_dma`() = run("cpu_interrupts_v2/rom_singles/4-irq_and_dma.nes")

  @Test
  fun `cpu_interrupts_v2 - branch_delays_irq`() = run("cpu_interrupts_v2/rom_singles/5-branch_delays_irq.nes")

  @Test
  fun cpu_timing_test6() = run("cpu_timing_test6/cpu_timing_test.nes")

  @Test
  fun `instr_misc - abs_x_wrap`() = run("instr_misc/rom_singles/01-abs_x_wrap.nes")

  @Test
  fun `instr_misc - branch_wrap`() = run("instr_misc/rom_singles/02-branch_wrap.nes")

  @Test
  fun `instr_misc - dummy_reads`() = run("instr_misc/rom_singles/03-dummy_reads.nes")

  @Test
  fun `instr_misc - dummy_reads_apu`() = run("instr_misc/rom_singles/04-dummy_reads_apu.nes")

  @Test
  fun `instr_timing - instr_timing`() = run("instr_timing/rom_singles/1-instr_timing.nes")

  @Test
  fun `instr_timing - branch_timing`() = run("instr_timing/rom_singles/2-branch_timing.nes")

  @Test
  fun oam_read() = run("oam_read/oam_read.nes")

  @Test
  fun `sprite_hit_tests - basics`() = run("sprite_hit_tests_2005.10.05/01.basics.nes")

  @Test
  fun `sprite_hit_tests - alignment`() = run("sprite_hit_tests_2005.10.05/02.alignment.nes")

  @Test
  fun `sprite_hit_tests - corners`() = run("sprite_hit_tests_2005.10.05/03.corners.nes")

  @Test
  fun `sprite_hit_tests - flip`() = run("sprite_hit_tests_2005.10.05/04.flip.nes")

  @Test
  fun `sprite_hit_tests - left_clip`() = run("sprite_hit_tests_2005.10.05/05.left_clip.nes")

  @Test
  fun `sprite_hit_tests - right_edge`() = run("sprite_hit_tests_2005.10.05/06.right_edge.nes")

  @Test
  fun `sprite_hit_tests - screen_bottom`() = run("sprite_hit_tests_2005.10.05/07.screen_bottom.nes")

  @Test
  fun `sprite_hit_tests - double_height`() = run("sprite_hit_tests_2005.10.05/08.double_height.nes")

  @Test
  fun `sprite_hit_tests - timing_basics`() = run("sprite_hit_tests_2005.10.05/09.timing_basics.nes")

  @Test
  fun `sprite_hit_tests - timing_order`() = run("sprite_hit_tests_2005.10.05/10.timing_order.nes")

  @Test
  fun `sprite_hit_tests - edge_timing`() = run("sprite_hit_tests_2005.10.05/11.edge_timing.nes")

  @Test
  fun `sprite_overflow_tests - Basics`() = run("sprite_overflow_tests/1.Basics.nes")

  @Test
  fun `sprite_overflow_tests - Details`() = run("sprite_overflow_tests/2.Details.nes")

  @Test
  fun `sprite_overflow_tests - Timing`() = run("sprite_overflow_tests/3.Timing.nes")

  @Test
  fun `sprite_overflow_tests - Obscure`() = run("sprite_overflow_tests/4.Obscure.nes")

  @Test
  fun `sprite_overflow_tests - Emulator`() = run("sprite_overflow_tests/5.Emulator.nes")

  private fun run(romFile: String) {
    val rom = Rom.parse(File(TEST_ROMS_BASE, romFile).readBytes())
    InteractiveRunner(rom, fullScreen = false).run()
  }

  companion object {
    private val TEST_ROMS_BASE = File("roms/nes-test-roms")
  }
}
