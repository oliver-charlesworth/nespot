package choliver.nespot.cpu

import choliver.nespot.common.*

@MutableForPerfReasons
data class Regs(
  var pc: Address = 0x0000,
  var a: Data = 0x00,
  var x: Data = 0x00,
  var y: Data = 0x00,
  var s: Data = 0x00,
  var p: Flags = Flags()
) {
  override fun toString() = "(PC=${pc.format16()}, S=${s.format8()}, A=${a.format8()}, X=${x.format8()}, Y=${y.format8()}, P=${p})"

  /** Like [copy], but allows us to also set individual status flags. */
  fun with(
    pc: Address = this.pc,
    a: Data = this.a,
    x: Data = this.x,
    y: Data = this.y,
    s: Data = this.s,
    n: Boolean = p.n,
    v: Boolean = p.v,
    d: Boolean = p.d,
    i: Boolean = p.i,
    z: Boolean = p.z,
    c: Boolean = p.c
  ) = copy(
    pc = pc,
    a = a,
    x = x,
    y = y,
    s = s,
    p = Flags(n = n, v = v, d = d, i = i, z = z, c = c)
  )
}

@MutableForPerfReasons
data class Flags(
  var n: Boolean = false,
  var v: Boolean = false,
  var d: Boolean = false,
  var i: Boolean = false,
  var z: Boolean = false,
  var c: Boolean = false
) {
  override fun toString() = "" +
    (if (n) 'N' else '-') +
    (if (v) 'V' else '-') +
    "--" +
    (if (d) 'D' else '-') +
    (if (i) 'I' else '-') +
    (if (z) 'Z' else '-') +
    (if (c) 'C' else '-')

  fun data(): Data = (
    (if (n) 0x80 else 0) or
    (if (v) 0x40 else 0) or
    (if (d) 0x08 else 0) or
    (if (i) 0x04 else 0) or
    (if (z) 0x02 else 0) or
    (if (c) 0x01 else 0)
  )
}

fun Data.toFlags() = Flags(
  n = isBitSet(7),
  v = isBitSet(6),
  d = isBitSet(3),
  i = isBitSet(2),
  z = isBitSet(1),
  c = isBitSet(0)
)
