package org.pixs.mazemaster;

/**
 * Read-only geometry of a single maze floor, indexed from the ROM.
 *
 * <p>The original game stores each square as one packed byte at
 * {@code $AA00 + level*$200 + y*20 + x} with the layout {@code wweessnn}
 * (two bits per direction). This class exposes typed queries on top.</p>
 */
public final class MazeMap {

    public static final int SIZE = 20;

    private final Rom rom;

    public MazeMap(Rom rom) {
        this.rom = rom;
    }

    /** Raw wall byte for the given square. */
    public byte wallsByte(int level, int x, int y) {
        return rom.wallsByte(level, x, y);
    }

    /** Wall type on a specific side of a square. */
    public WallType wall(int level, int x, int y, int sideIndex) {
        int square = rom.wallsByte(level, x, y) & 0xFF;
        return WallType.values()[(square >> (sideIndex * 2)) & 0x03];
    }

    /**
     * Compute the 5-slot "facing walls" array used by the 3D renderer.
     *
     * <ul>
     *   <li>slot 0: wall on our left</li>
     *   <li>slot 1: wall on our right</li>
     *   <li>slot 2: wall directly in front</li>
     *   <li>slot 3: front wall of the left-neighbour square
     *       (only meaningful when slot 0 is NONE)</li>
     *   <li>slot 4: front wall of the right-neighbour square
     *       (only meaningful when slot 1 is NONE)</li>
     * </ul>
     *
     * Replaces the four ROM routines that existed for each facing direction
     * (getWallsFacingN/E/S/W) — all four share the same structure once you
     * consult {@link Direction}'s index metadata.
     */
    public WallType[] facingWalls(int level, int x, int y, Direction dir) {
        WallType[] walls = getWalls(level, x, y);
        WallType[] result = new WallType[5];
        result[0] = walls[dir.sideLeft];
        result[1] = walls[dir.sideRight];
        result[2] = walls[dir.front];
        if (result[0] == WallType.NONE) {
            Direction leftDir = dir.turnLeft();
            int lx = wrap(x + leftDir.dx);
            int ly = wrap(y + leftDir.dy);
            result[3] = getWalls(level, lx, ly)[dir.front];
        }
        if (result[1] == WallType.NONE) {
            Direction rightDir = dir.turnRight();
            int rx = wrap(x + rightDir.dx);
            int ry = wrap(y + rightDir.dy);
            result[4] = getWalls(level, rx, ry)[dir.front];
        }
        return result;
    }

    private int wrap(int coord) {
        return (coord + SIZE) % SIZE;
    }

    /** Decodes one square byte into {@code [north, south, east, west]}. */
    private WallType[] getWalls(int level, int x, int y) {
        byte b = rom.wallsByte(level, x, y);
        return new WallType[] {
                WallType.values()[b & 0x03],
                WallType.values()[(b >> 2) & 0x03],
                WallType.values()[(b >> 4) & 0x03],
                WallType.values()[(b >> 6) & 0x03]
        };
    }
}
