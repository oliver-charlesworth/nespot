# NESpot

NESPot is a NES emulator written in Kotlin.  It's not yet cycle accurate and has low coverage of
[mapper variants](https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid), so not
all games work yet.


## Games tested

| Game | SHA-1 (PRG+CHR) | Status |
| --- | --- | --- |
| Donkey Kong           | `D222DBBA5BD3716BBF62CA91167C6A9D15C60065` | No known issues. |
| Super Mario Bros      | `FACEE9C577A5262DBE33AC4930BB0B58C8C037F7` | Very playable.  [One known minor bug](https://github.com/oliver-charlesworth/nespot/issues/87). |
| Bubble Bobble         | `C1A8F6A9316080487CFEACA62F3D573CD9D484E9` | No known issues. |
| Hook                  | `EAE5205928D3FA2AFEA0374B457D2BB91E414D99` | No known issues. |
| Legend of Zelda       | `A12D74C73A0481599A5D832361D168F4737BBCF6` | Very playable.  [On known minor bug](https://github.com/oliver-charlesworth/nespot/issues/89). |
| Micro Machines        | `C7FD43041FC139DC8440C95C28A0115DC79E2691` | Very playable.  [Lots of graphical glitches, particularly in menus](https://github.com/oliver-charlesworth/nespot/issues/88). |
| Space Harrier         | `AEE6BB2338E71CC9390FBB845225C19E194CDD21` | No known issues. |
| Track and Field II    | `87E7943769CE95747AC80DA044B2CF9E63410AF2` | No known issues. |
| Dizzy the Adventurer  | `5A4EF3B2F5880D3B4609AE4F3381D688B3A02CAC` | No known issues. |
| Bomberman II          | `2E401097B7B6F5DE5B0F88E6A97C5675BD916801` | Very playable.  [Vertical scroll of splash screen not quite right](https://github.com/oliver-charlesworth/nespot/issues/91). |
| MIG-29 Soviet Fighter | `B74802F946D99A83E8E223B6F987E6482A8EC41D` | Freezes on start of game. |
| Xenophobe             | `2C430A5D4AF069A4C4B9082422B7F570ADA5AE31` | Freezes on splash screen. |


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
| Cmd+Q | Quit |


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
