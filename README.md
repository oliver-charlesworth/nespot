# NESpot

## To do

- Sound!
  - Clicking
  - SMB - explosions sound wrong
  - Interrupts

- Rendering

  - Enable / disable rendering
  - Clipping - inc. impact on collision detection
  - Limit to four sprites
  - Detect too many sprites
  - Sprite priority (overlap)
  - Greyscale
  - Colour emphasis

- Cartridge
  - CHR-RAM (used by Zelda)
  - PRG-RAM

- Quirks

  - Sprite overflow bug
  - OAM address reset during rendering
  - Trigger VBL if isVblEnabled becomes true during VBL period

- Model cycle counts for:

  - Page boundary crossings 
  - OAM DMA
  - APU DMC stalls (see http://wiki.nesdev.com/w/index.php/APU_DMC)
  
- PPU reset
  
- Unify where we do address clipping (e.g. inside or outside `Memory` implementations)
  
- Decimal mode (not required for NES)

- Constants strewn everywhere

- `Runner`

  - `--info` mode to print out the hash / headers of the ROM.
  - Full-screen mode.
  - Ability to serialise state (i.e. save game).


## References

- https://en.wikipedia.org/wiki/MOS_Technology_6502
- http://www.obelisk.me.uk/6502/reference.html
- http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
- http://6502.org/documents/datasheets/mos/mos_6501-6505_mpu_preliminary_aug_1975.pdf
- http://wiki.nesdev.com/w/index.php/CPU 
- http://wiki.nesdev.com/w/index.php/PPU
