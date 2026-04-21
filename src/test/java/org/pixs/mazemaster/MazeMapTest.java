package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins MazeMap to the same semantics the four hand-written
 * getWallsFacing&lt;N/E/S/W&gt; methods used to implement. Each test seeds a
 * specific wall-byte pattern and checks the facingWalls slot layout.
 */
class MazeMapTest {

    private Rom romWithSingleSquare(int level, int x, int y, int wallByte) {
        byte[] mem = new byte[Rom.SIZE];
        int addr = 0xAA00 + (level << 9) + y * 20 + x;
        mem[addr] = (byte) wallByte;
        return new Rom(mem);
    }

    /** {@code wallsByte} = wweessnn packed, 2 bits per side. */
    private int pack(int n, int s, int e, int w) {
        return (n & 3) | ((s & 3) << 2) | ((e & 3) << 4) | ((w & 3) << 6);
    }

    @Test
    void wallBitDecoding() {
        // N=WALL(1), S=DOOR(2), E=HIDDEN(3), W=NONE(0)
        Rom rom = romWithSingleSquare(0, 5, 5, pack(1, 2, 3, 0));
        MazeMap map = new MazeMap(rom);

        assertSame(WallType.WALL,   map.wall(0, 5, 5, 0));
        assertSame(WallType.DOOR,   map.wall(0, 5, 5, 1));
        assertSame(WallType.HIDDEN, map.wall(0, 5, 5, 2));
        assertSame(WallType.NONE,   map.wall(0, 5, 5, 3));
    }

    @Test
    void facingNorthReadsWestOnLeftAndEastOnRight() {
        // Current square: N=NONE, S=NONE, E=WALL, W=DOOR.
        // Looking north → left=W=DOOR, right=E=WALL, front=N=NONE.
        Rom rom = romWithSingleSquare(0, 5, 5, pack(0, 0, 1, 2));
        MazeMap map = new MazeMap(rom);

        WallType[] facing = map.facingWalls(0, 5, 5, Direction.NORTH);
        assertSame(WallType.DOOR, facing[0], "left");
        assertSame(WallType.WALL, facing[1], "right");
        assertSame(WallType.NONE, facing[2], "front");
    }

    @Test
    void facingSouthReadsEastOnLeftAndWestOnRight() {
        Rom rom = romWithSingleSquare(0, 5, 5, pack(1, 0, 2, 3));
        MazeMap map = new MazeMap(rom);

        WallType[] facing = map.facingWalls(0, 5, 5, Direction.SOUTH);
        assertSame(WallType.DOOR,   facing[0], "facing south, left = east wall");
        assertSame(WallType.HIDDEN, facing[1], "facing south, right = west wall");
        assertSame(WallType.NONE,   facing[2], "facing south, front = south wall");
    }

    @Test
    void neighbourFrontWallFillsSlot3When_noLeftWall() {
        // Current square at (5, 5): N=NONE, S=NONE, E=NONE, W=NONE (fully open).
        // Left of us when facing NORTH is (4, 5). Seed its north wall = DOOR.
        byte[] mem = new byte[Rom.SIZE];
        mem[0xAA00 + 5 * 20 + 5] = (byte) pack(0, 0, 0, 0);
        mem[0xAA00 + 5 * 20 + 4] = (byte) pack(2, 0, 0, 0); // left neighbour's north wall
        MazeMap map = new MazeMap(new Rom(mem));

        WallType[] facing = map.facingWalls(0, 5, 5, Direction.NORTH);
        assertSame(WallType.NONE, facing[0]);
        assertSame(WallType.DOOR, facing[3], "slot 3 = neighbour's front wall when own left is open");
    }

    @Test
    void neighbourFrontWallFillsSlot4When_noRightWall() {
        byte[] mem = new byte[Rom.SIZE];
        mem[0xAA00 + 5 * 20 + 5] = (byte) pack(0, 0, 0, 0);       // open square
        mem[0xAA00 + 5 * 20 + 6] = (byte) pack(1, 0, 0, 0);       // right neighbour has a WALL to the north
        MazeMap map = new MazeMap(new Rom(mem));

        WallType[] facing = map.facingWalls(0, 5, 5, Direction.NORTH);
        assertSame(WallType.NONE, facing[1]);
        assertSame(WallType.WALL, facing[4]);
    }

    @Test
    void wrapsAroundTheTorus() {
        // Left neighbour of (0, 5) facing NORTH is (19, 5) thanks to wrap.
        byte[] mem = new byte[Rom.SIZE];
        mem[0xAA00 + 5 * 20 + 0]  = (byte) pack(0, 0, 0, 0);      // (0,5) is open
        mem[0xAA00 + 5 * 20 + 19] = (byte) pack(3, 0, 0, 0);      // (19,5) has HIDDEN to the north
        MazeMap map = new MazeMap(new Rom(mem));

        WallType[] facing = map.facingWalls(0, 0, 5, Direction.NORTH);
        assertSame(WallType.HIDDEN, facing[3], "wrap on x=0 → x=19");
    }

    @Test
    void wallByteRoundTripThroughRom() {
        Rom rom = romWithSingleSquare(2, 3, 4, 0x5A);
        MazeMap map = new MazeMap(rom);

        assertEquals((byte) 0x5A, map.wallsByte(2, 3, 4));

        // Exercise the same data through facingWalls by decoding each 2-bit field.
        int expectedN = 0x5A & 0x03;
        int expectedS = (0x5A >> 2) & 0x03;
        int expectedE = (0x5A >> 4) & 0x03;
        int expectedW = (0x5A >> 6) & 0x03;
        WallType[] expected = {
                WallType.values()[expectedN],
                WallType.values()[expectedS],
                WallType.values()[expectedE],
                WallType.values()[expectedW]
        };
        // Current square only — confirm by sampling each side via map.wall(...).
        for (int side = 0; side < 4; side++) {
            assertSame(expected[side], map.wall(2, 3, 4, side), "side " + side);
        }
        // Simple array equality check on the decoded set, for good measure.
        WallType[] decoded = {
                map.wall(2, 3, 4, 0),
                map.wall(2, 3, 4, 1),
                map.wall(2, 3, 4, 2),
                map.wall(2, 3, 4, 3)
        };
        assertArrayEquals(expected, decoded);
    }
}
