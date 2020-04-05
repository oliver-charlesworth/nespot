@file:Suppress("NOTHING_TO_INLINE")

package choliver.sixfiveohtwo

typealias UInt8 = UByte
typealias UInt16 = UShort
typealias Int8 = Byte
typealias Int16 = Short

inline fun Int.u8(): UInt8 = toUByte()
inline fun UInt.u8(): UInt8 = toUByte()
inline fun UInt16.u8(): UInt8 = toUByte()

inline fun Int.u16(): UInt16 = toUShort()
inline fun UInt.u16() : UInt16 = toUShort()
inline fun Int8.u16(): UInt16 = toUShort()
inline fun UInt8.u16(): UInt16 = toUShort()

inline fun UInt8.isZero() = this == 0.u8()
inline fun UInt8.isNegative() = this >= 0x80u


inline fun Int.lo() = u8()
inline fun Int.hi() = (this ushr 8).u8()
inline fun Int.loI() = lo().toInt()
inline fun Int.hiI() = hi().toInt()

inline fun UInt.lo() = u8()
inline fun UInt.hi() = (this / 256u).u16()

inline fun UInt16.lo() = u8()
inline fun UInt16.hi() = (this / 256u).u16()

fun combine(low: UInt8, high: UInt8): UInt16 = (low.u16() or (high * 256u).u16())
