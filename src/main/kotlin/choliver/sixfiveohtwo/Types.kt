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

fun combine(low: UInt8, high: UInt8): UInt16 = (low.u16() or (high * 256u).u16())
