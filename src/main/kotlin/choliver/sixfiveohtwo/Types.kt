package choliver.sixfiveohtwo

typealias UInt8 = UByte
typealias UInt16 = UShort
typealias Int8 = Byte
typealias Int16 = Short

inline fun Int.toUInt8(): UInt8 = toUByte()
inline fun UInt.toUInt8(): UInt8 = toUByte()
inline fun UInt16.toUInt8(): UInt8 = toUByte()

inline fun Int.toUInt16(): UInt16 = toUShort()
inline fun UInt.toUInt16() : UInt16 = toUShort()
inline fun Int8.toUInt16(): UInt16 = toUShort()
inline fun UInt8.toUInt16(): UInt16 = toUShort()

inline fun UInt8.isZero() = this == 0.toUInt8()
inline fun UInt8.isNegative() = this >= 0x80u
