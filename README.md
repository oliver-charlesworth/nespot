# NESpot

NESPot is a NES emulator written in Kotlin.  It's not yet cycle accurate and has low coverage of
[mapper variants](https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid), so not
all games work yet.


## Games tested

| Game | SHA-1 (PRG+CHR) | Status |
| --- | --- | --- |
| Super Mario Bros | `FACEE9C577A5262DBE33AC4930BB0B58C8C037F7` | Good.  [One known bug](issues/87).


## Running

Two ways to run:

```
./gradlew install
build/install/nespot/bin/nespot [<options>] <rom>
```

or:

```
./gradlew run --args "[<options>] <rom>"
```


## Options

| Option | Description |
| --- | --- |
| `-f` / `--fullscreen` | Full-screen mode |
| `-i` / `--info` | Print ROM info |


## Prerequisites

NESpot is built with JDK 11.  It may work with older Java versions, but this has not been tested. 


## Key bindings

| Key | Effect |
| --- | --- |
| Up/Down/Left/Right | Controller D-pad |
| Z | Controller A |
| X | Controller B |
| [ | Controller Select |
| ] | Controller Start |
| F | Toggle full-screen mode |


## To do

- Known game issues

  - Zelda
    - One pixel off on vertical scroll in start menu


## References

For 6502 emulation:

- https://en.wikipedia.org/wiki/MOS_Technology_6502
- http://www.obelisk.me.uk/6502/reference.html
- http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
- http://6502.org/documents/datasheets/mos/mos_6501-6505_mpu_preliminary_aug_1975.pdf

For NES internals:

- http://wiki.nesdev.com/w/index.php/CPU 
- http://wiki.nesdev.com/w/index.php/PPU
- http://wiki.nesdev.com/w/index.php/APU
