# NESpot

NESPot is a NES emulator written in Kotlin.  It's not yet cycle accurate and has low coverage of
[mapper variants](https://wiki.nesdev.com/w/index.php/Mapper#iNES_1.0_mapper_grid), so not
all games work yet.


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
| `-s <file>` / `--snapshot <file>` | Restore from snapshot file |


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
| R | Restore initial state |
| S | Create snapshot (currently dumped to console) |
| Cmd+Q | Quit |


## Games tested

| Game | SHA-1 (PRG+CHR) | Mapper | Status |
| --- | --- | --- |
| Donkey Kong           | `D222DBBA5BD3716BBF62CA91167C6A9D15C60065` | 0  | ✅ No known issues. |
| Super Mario Bros      | `FACEE9C577A5262DBE33AC4930BB0B58C8C037F7` | 0  | ✅ No known issues. |
| Bubble Bobble         | `C1A8F6A9316080487CFEACA62F3D573CD9D484E9` | 1  | ✅ No known issues. |
| Hook                  | `EAE5205928D3FA2AFEA0374B457D2BB91E414D99` | 1  | ✅ No known issues. |
| Space Harrier         | `AEE6BB2338E71CC9390FBB845225C19E194CDD21` | 1  | ✅ No known issues. |
| Track and Field II    | `87E7943769CE95747AC80DA044B2CF9E63410AF2` | 1  | ✅ No known issues. |
| Dizzy the Adventurer  | `5A4EF3B2F5880D3B4609AE4F3381D688B3A02CAC` | 71 | ✅ No known issues. |
| Castelian             | `847D56E43754E402666A91188520737094E9ECFA` | 2  | ✅ No known issues. |
| Legend of Zelda       | `A12D74C73A0481599A5D832361D168F4737BBCF6` | 1  | ✅ Very playable.  [One known minor bug](https://github.com/oliver-charlesworth/nespot/issues/89). |
| Micro Machines        | `C7FD43041FC139DC8440C95C28A0115DC79E2691` | 71 | ✅ Very playable.  [A few graphical glitches](https://github.com/oliver-charlesworth/nespot/issues/88). |
| Bomberman II          | `2E401097B7B6F5DE5B0F88E6A97C5675BD916801` | 1  | ✅ Very playable.  [Vertical scroll of splash screen not quite right](https://github.com/oliver-charlesworth/nespot/issues/91). |
| Kirby's Adventure     | `118307DA6C77A592F0884BAAD14120301D8D3A1B` | 4  | ✅ Very playable.  [Start menu judders](TODO). |
| MIG-29 Soviet Fighter | `B74802F946D99A83E8E223B6F987E6482A8EC41D` | 71 | ❌ Mostly unplayable.  [Gameplay is flickery and slow](https://github.com/oliver-charlesworth/nespot/issues/96). |
| Xenophobe             | `2C430A5D4AF069A4C4B9082422B7F570ADA5AE31` | 1  | ❌ Unplayable.  [Freezes on splash screen](https://github.com/oliver-charlesworth/nespot/issues/97). |
| New Zealand Story     | `EA388B7B826E14CB907FBEDC1651EE831FB22D41` | 4  | ❌ Unplayable.  [Freezes after splash screen].(TODO). |
| Super Mario Bros 3    | `31144C7C436CC78D7FD3AC42B224EEE3A6C2BABC` | 4  | ❌ Unplayable.  [Crashes on splash screen].(TODO). |
| Jurassic Park         | `57F755C6A10C6681761070B0350DA72432A534E9` | 4  | ❌ Unplayable.  [Just a black screen].(TODO). |


## Implementation resources

For 6502 emulation:

- https://en.wikipedia.org/wiki/MOS_Technology_6502
- http://www.obelisk.me.uk/6502/reference.html
- http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
- http://6502.org/documents/datasheets/mos/mos_6501-6505_mpu_preliminary_aug_1975.pdf

For NES internals:

- http://wiki.nesdev.com/w/index.php/CPU
- http://wiki.nesdev.com/w/index.php/PPU
- http://wiki.nesdev.com/w/index.php/APU
