package choliver.nespot.sixfiveohtwo.model

enum class AddressMode {
  ACCUMULATOR,
  IMMEDIATE,
  IMPLIED,
  INDIRECT,
  RELATIVE,
  ABSOLUTE,
  ABSOLUTE_X,
  ABSOLUTE_Y,
  ZERO_PAGE,
  ZERO_PAGE_X,
  ZERO_PAGE_Y,
  INDEXED_INDIRECT,
  INDIRECT_INDEXED
}
