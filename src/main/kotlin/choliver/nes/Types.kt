@file:Suppress("NOTHING_TO_INLINE")

package choliver.nes

typealias Data = Int      // 8-bit in reality (usually unsigned)
typealias Address8 = Int   // 8-bit in reality (unsigned)
typealias Address = Int   // 16-bit in reality (unsigned)

inline fun Data.isZero() = this == 0
inline fun Data.isNeg() = isBitSet(7)

inline fun Address.lo(): Data = data()
inline fun Address.hi(): Data = (this shr 8).data()

inline fun Byte.data(): Data = toInt().data()
inline fun Int.data(): Data = this and 0xFF

inline fun Int.addr(): Address = this and 0xFFFF
inline fun Int.addr8(): Address8 = this and 0xFF

inline fun addr(lo: Data, hi: Data): Address = (lo or (hi shl 8))

inline fun Data.sext() = this or (if (isNeg()) 0xFF00 else 0x0000)

inline fun Byte.isBitSet(i: Int) = toInt().isBitSet(i)
inline fun Int.isBitSet(i: Int) = (this and (1 shl i)) != 0

