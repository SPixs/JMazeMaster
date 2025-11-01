# JMazeMaster

A Java recreation of **Maze Master**, the 1983 Commodore 64 dungeon crawler by Michael Cranford (creator of *The Bard's Tale*).

This isn't an emulator—I reverse-engineered the original cartridge ROM and reconstructed the entire game logic in Java. The goal was to make it as faithful to the original as possible while running natively.

![Game Screenshot](media/main_menu.png) ![Image](media/enter_maze.png)

## What's Maze Master?

You control a party of up to 3 characters (warriors and wizards) exploring a 20×20 maze across 5 levels. Fight monsters, collect loot, solve a riddle, and eventually face the BALROG in the final chamber. Pretty standard stuff for early 80s dungeon crawlers, but Cranford made it work on the C64's limited hardware.

## Building and Running

**Requirements:**
- Java 11 or later
- Maven (for easy building)

**Clone and build:**
```sh
git clone https://github.com/SPixs/JMazeMaster.git
cd JMazeMaster
mvn clean package
```

**Run it:**
```sh
java -jar target/JMazeMaster-1.0-SNAPSHOT.jar
```

Or if you want to run directly without building the JAR:
```sh
mvn exec:java
```

## Reverse Engineering Notes

I disassembled the original 6510 assembly code from the C64 cartridge and documented everything. You can find the fully commented source here:

**→ [c64_resources/source.asm](c64_resources/source.asm)**

The Java code references original memory addresses and code labels throughout (like `$A5B2`, `$9C99`) to make it easier to cross-reference with the assembly.

If you're into retro game preservation or just curious how 8-bit games worked, this might be useful.

## Game Manual

The original manual is included in the repo. It explains spells, equipment, and game mechanics:

**→ [media/mazeMasterManuel.pdf](media/mazeMasterManuel.pdf)**

## Technical Details

- **Hardware emulation**: VIC-II graphics chip, CIA1 for input, PETSCII character set
- **No external dependencies**: Pure Java with Swing for rendering
- **Encoding**: All sources are UTF-8
- **Memory**: Loads the original 64KB C64 memory image and works with it directly

The architecture separates hardware emulation (`org.pixs.hardware`) from game logic (`org.pixs.mazemaster`), using a state pattern for different game modes (menu, maze exploration, combat).

## Screenshots

![Image](media/fight.png)

## License

MIT License - see [LICENSE](LICENSE) for details.
