package choliver.nespot.playtest

import choliver.nespot.playtest.engine.Engine
import choliver.nespot.playtest.engine.SnapshotPattern.PERIODIC
import org.junit.jupiter.api.Test
import java.io.File

class GamesTest {
  @Test
  fun `super mario bros`() = engine.execute("smb")

  @Test
  fun `super mario bros 3`() = engine.execute("smb3")

  @Test
  fun `bubble bobble`() = engine.execute("bb")

  @Test
  fun `donkey kong`() = engine.execute("dk")

  @Test
  fun `kirby's adventure`() = engine.execute("kirby")

  @Test
  fun `micro machines`() = engine.execute("mm")

  @Test
  fun `mig-29 soviet fighter`() = engine.execute("mig29")

  @Test
  fun castelian() = engine.execute("castelian")

  @Test
  fun `jurassic park`() = engine.execute("jp")

  @Test
  fun `new zealand story`() = engine.execute("nzs")

  @Test
  fun `fire hawk`() = engine.execute("firehawk")

  @Test
  fun battletoads() = engine.execute("battletoads")

  @Test
  fun `wwf wrestlemania`() = engine.execute("wwf")


  private val engine = Engine(
    romsBase = File("roms"),
    capturesBase = File("captures"),
    snapshotPattern = PERIODIC
  )
}
