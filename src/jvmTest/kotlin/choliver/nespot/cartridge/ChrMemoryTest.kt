package choliver.nespot.cartridge

import choliver.nespot.BASE_CHR_ROM
import choliver.nespot.BASE_VRAM
import choliver.nespot.NAMETABLE_SIZE
import choliver.nespot.cartridge.Rom.Mirroring.*
import choliver.nespot.mappers.BankMappingChecker
import choliver.nespot.mappers.BankMappingChecker.Companion.takesBytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ChrMemoryTest {
  // TODO - test for non-linear mappings (not sure why this is useful, though)
  @Test
  fun `linear RAM loads and stores`() {
    val mapper = ChrMemory(ByteArray(8192), bankSize = 2048)
    val checker = BankMappingChecker(
      bankSize = 2048,
      srcBase = BASE_CHR_ROM,
      outBase = BASE_CHR_ROM,
      setSrc = mapper::set,
      getOut = mapper::get
    )

    checker.assertMappings(0 to 0, 1 to 1, 2 to 2, 3 to 3)
  }

  @Test
  fun `non-linear ROM loads`() {
    val raw = ByteArray(16384)
    val mapper = PrgMemory(raw, bankSize = 2048)
    val checker = BankMappingChecker(
      bankSize = 2048,
      outBase = BASE_CHR_ROM,
      setSrc = takesBytes(raw::set),
      getOut = mapper::get
    )

    mapper.bankMap[0] = 7
    mapper.bankMap[1] = 4
    mapper.bankMap[2] = 5
    mapper.bankMap[3] = 2

    checker.assertMappings(7 to 0, 4 to 1, 5 to 2, 2 to 3)
  }

  @Nested
  inner class Vram {
    private val mapper = ChrMemory(ByteArray(0), 1024)

    @Test
    fun `single-screen - nametable 0`() {
      mapper.mirroring = FIXED_LOWER

      assertVramMappings(mapper, listOf(0, 1, 2, 3))
    }

    // TODO - distinguish from nametable 0 - only possible if we change mode partway through
    @Test
    fun `single-screen - nametable 1`() {
      mapper.mirroring = FIXED_UPPER

      assertVramMappings(mapper, listOf(0, 1, 2, 3))
    }

    @Test
    fun `vertical mirroring`() {
      mapper.mirroring = VERTICAL

      assertVramMappings(mapper, listOf(0, 2), listOf(1, 3))
    }

    @Test
    fun `horizontal mirroring`() {
      mapper.mirroring = HORIZONTAL

      assertVramMappings(mapper, listOf(0, 1), listOf(2, 3))
    }

    private fun assertVramMappings(mem: ChrMemory, vararg nametableAliases: List<Int>) {
      val checker = BankMappingChecker(
        bankSize = NAMETABLE_SIZE,
        srcBase = BASE_VRAM,
        outBase = BASE_VRAM,
        setSrc = mem::set,
        getOut = mapper::get
      )

      val mappings = nametableAliases.flatMap { aliases ->
        // Cartesian product of all aliases in group - can we get from any alias to any other alias?
        aliases.flatMap { first -> aliases.map { second -> first to second } }
      }

      checker.assertMappings(*mappings.toTypedArray())
    }
  }
}
