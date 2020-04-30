# NESpot

## To do

- Known game issues

  - Micro Machines
    - Lots of graphical artifacts
  - Zelda
    - One pixel off on vertical scroll in start menu

- Sound

  - Clicking
  - Interrupts

- Rendering

  - Greyscale
  - Colour emphasis

- Quirks

  - [Sprite overflow bug](http://wiki.nesdev.com/w/index.php/PPU_sprite_evaluation#Sprite_overflow_bug)
  - OAM address reset during rendering
  - Trigger VBL if isVblEnabled becomes true during VBL period

- Model cycle counts for:

  - Page boundary crossings 
  - [OAM DMA stalls](http://wiki.nesdev.com/w/index.php/PPU_OAM#DMA)
  - [APU DMC stalls](http://wiki.nesdev.com/w/index.php/APU_DMC#Memory_reader)
  
- PPU reset
  
- Unify where we do address clipping (e.g. inside or outside `Memory` implementations)
  
- Decimal mode (not required for NES)

- Constants strewn everywhere

- `Runner`

  - Ability to serialise state (i.e. save game).


## References

- https://en.wikipedia.org/wiki/MOS_Technology_6502
- http://www.obelisk.me.uk/6502/reference.html
- http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
- http://6502.org/documents/datasheets/mos/mos_6501-6505_mpu_preliminary_aug_1975.pdf
- http://wiki.nesdev.com/w/index.php/CPU 
- http://wiki.nesdev.com/w/index.php/PPU
