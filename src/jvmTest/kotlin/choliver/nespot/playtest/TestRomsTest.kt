package choliver.nespot.playtest

import choliver.nespot.playtest.engine.Engine
import choliver.nespot.playtest.engine.SnapshotPattern.FINAL
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class TestRomsTest {
  @Test
  fun `branch_timing_tests - Branch_Basics`() = engine.execute("branch_timing_tests/1.Branch_Basics")

  @Test
  fun `branch_timing_tests - Backward_Branch`() = engine.execute("branch_timing_tests/2.Backward_Branch")

  @Test
  fun `branch_timing_tests - Forward_Branch`() = engine.execute("branch_timing_tests/3.Forward_Branch")

  @Disabled
  @Test
  fun cpu_dummy_reads() = engine.execute("cpu_dummy_reads/cpu_dummy_reads")

  @Disabled
  @Test
  fun `cpu_interrupts_v2 - cli_latency`() = engine.execute("cpu_interrupts_v2/rom_singles/1-cli_latency")

  @Disabled
  @Test
  fun `cpu_interrupts_v2 - nmi_and_brk`() = engine.execute("cpu_interrupts_v2/rom_singles/2-nmi_and_brk")

  @Disabled
  @Test
  fun `cpu_interrupts_v2 - nmi_and_irq`() = engine.execute("cpu_interrupts_v2/rom_singles/3-nmi_and_irq")

  @Disabled
  @Test
  fun `cpu_interrupts_v2 - irq_and_dma`() = engine.execute("cpu_interrupts_v2/rom_singles/4-irq_and_dma")

  @Disabled
  @Test
  fun `cpu_interrupts_v2 - branch_delays_irq`() = engine.execute("cpu_interrupts_v2/rom_singles/5-branch_delays_irq")

  @Test
  fun cpu_timing_test6() = engine.execute("cpu_timing_test6/cpu_timing_test")

  @Test
  fun `instr_misc - abs_x_wrap`() = engine.execute("instr_misc/rom_singles/01-abs_x_wrap")

  @Test
  fun `instr_misc - branch_wrap`() = engine.execute("instr_misc/rom_singles/02-branch_wrap")

  @Disabled
  @Test
  fun `instr_misc - dummy_reads`() = engine.execute("instr_misc/rom_singles/03-dummy_reads")

  @Disabled
  @Test
  fun `instr_misc - dummy_reads_apu`() = engine.execute("instr_misc/rom_singles/04-dummy_reads_apu")

  @Disabled
  @Test
  fun `instr_timing - instr_timing`() = engine.execute("instr_timing/rom_singles/1-instr_timing")

  @Test
  fun `instr_timing - branch_timing`() = engine.execute("instr_timing/rom_singles/2-branch_timing")

  @Disabled
  @Test
  fun oam_read() = engine.execute("oam_read/oam_read")

  @Test
  fun `sprite_hit_tests - basics`() = engine.execute("sprite_hit_tests_2005.10.05/01.basics")

  @Test
  fun `sprite_hit_tests - alignment`() = engine.execute("sprite_hit_tests_2005.10.05/02.alignment")

  @Test
  fun `sprite_hit_tests - corners`() = engine.execute("sprite_hit_tests_2005.10.05/03.corners")

  @Test
  fun `sprite_hit_tests - flip`() = engine.execute("sprite_hit_tests_2005.10.05/04.flip")

  @Test
  fun `sprite_hit_tests - left_clip`() = engine.execute("sprite_hit_tests_2005.10.05/05.left_clip")

  @Test
  fun `sprite_hit_tests - right_edge`() = engine.execute("sprite_hit_tests_2005.10.05/06.right_edge")

  @Disabled
  @Test
  fun `sprite_hit_tests - screen_bottom`() = engine.execute("sprite_hit_tests_2005.10.05/07.screen_bottom")

  @Test
  fun `sprite_hit_tests - double_height`() = engine.execute("sprite_hit_tests_2005.10.05/08.double_height")

  @Disabled
  @Test
  fun `sprite_hit_tests - timing_basics`() = engine.execute("sprite_hit_tests_2005.10.05/09.timing_basics")

  @Disabled
  @Test
  fun `sprite_hit_tests - timing_order`() = engine.execute("sprite_hit_tests_2005.10.05/10.timing_order")

  @Disabled
  @Test
  fun `sprite_hit_tests - edge_timing`() = engine.execute("sprite_hit_tests_2005.10.05/11.edge_timing")

  @Disabled
  @Test
  fun `sprite_overflow_tests - Basics`() = engine.execute("sprite_overflow_tests/1.Basics")

  @Disabled
  @Test
  fun `sprite_overflow_tests - Details`() = engine.execute("sprite_overflow_tests/2.Details")

  @Disabled
  @Test
  fun `sprite_overflow_tests - Timing`() = engine.execute("sprite_overflow_tests/3.Timing")

  @Disabled
  @Test
  fun `sprite_overflow_tests - Obscure`() = engine.execute("sprite_overflow_tests/4.Obscure")

  @Disabled
  @Test
  fun `sprite_overflow_tests - Emulator`() = engine.execute("sprite_overflow_tests/5.Emulator")

  private val engine = Engine(
    romsBase = File("roms/nes-test-roms"),
    capturesBase = File("captures/nes-test-roms"),
    snapshotPattern = FINAL
  )
}
