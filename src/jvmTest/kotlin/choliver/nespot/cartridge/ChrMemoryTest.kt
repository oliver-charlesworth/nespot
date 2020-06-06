package choliver.nespot.cartridge

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_VRAM
import choliver.nespot.NAMETABLE_SIZE
import choliver.nespot.cartridge.BankMappingChecker.Companion.takesBytes
import choliver.nespot.cartridge.Rom.Mirroring.*
import choliver.nespot.common.Address
import choliver.nespot.common.Data
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChrMemoryTest {
  // TODO - test for non-linear mappings (not sure why this is useful, though)
  @Test
  fun `linear RAM loads and stores`() {
    val mem = ChrMemory(ByteArray(8192), bankSize = 2048)
    val checker = BankMappingChecker(
      bankSize = 2048,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = mem::set,
      getOut = mem::get
    )

    checker.assertMappings(0 to 0, 1 to 1, 2 to 2, 3 to 3)
  }

  @Test
  fun `non-linear ROM loads`() {
    val raw = ByteArray(16384)
    val mem = ChrMemory(raw, bankSize = 2048)
    val checker = BankMappingChecker(
      bankSize = 2048,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(raw::set),
      getOut = mem::get
    )

    mem.bankMap[0] = 7
    mem.bankMap[1] = 4
    mem.bankMap[2] = 5
    mem.bankMap[3] = 2

    checker.assertMappings(7 to 0, 4 to 1, 5 to 2, 2 to 3)
  }

  @Test
  fun `invokes callbacks`() {
    val onGet = mock<(Address) -> Unit>()
    val onSet = mock<(Address, Data) -> Unit>()
    val mem = ChrMemory(ByteArray(8192), bankSize = 1024, onGet = onGet, onSet = onSet)

    mem[6]
    mem[7] = 33

    verify(onGet)(6)
    verify(onSet)(7, 33)
  }

  @Nested
  inner class Vram {
    private val mem = ChrMemory(ByteArray(0), 1024)

    @Test
    fun `single-screen - nametable 0`() {
      mem.mirroring = FIXED_LOWER

      assertVramMappings(mem, listOf(0, 1, 2, 3))
    }

    // TODO - distinguish from nametable 0 - only possible if we change mode partway through
    @Test
    fun `single-screen - nametable 1`() {
      mem.mirroring = FIXED_UPPER

      assertVramMappings(mem, listOf(0, 1, 2, 3))
    }

    @Test
    fun `vertical mirroring`() {
      mem.mirroring = VERTICAL

      assertVramMappings(mem, listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      mem.mirroring = HORIZONTAL

      assertVramMappings(mem, listOf(0, 1), listOf(2, 3))
    }

    private fun assertVramMappings(mem: ChrMemory, vararg nametableAliases: List<Int>) {
      val checker = BankMappingChecker(
        bankSize = NAMETABLE_SIZE,
        srcBase = BASE_VRAM,
        outBase = BASE_VRAM,
        setSrc = mem::set,
        getOut = this.mem::get
      )

      val mappings = nametableAliases.flatMap { aliases ->
        // Cartesian product of all aliases in group - can we get from any alias to any other alias?
        aliases.flatMap { first -> aliases.map { second -> first to second } }
      }

      checker.assertMappings(*mappings.toTypedArray())
    }
  }
}
