package org.pixs.mazemaster;

/**
 * The four compass directions the party can face. Ordinals match the raw
 * encoding the ROM uses in zero-page $65 (0=NORTH, 1=EAST, 2=SOUTH, 3=WEST),
 * so conversions are safe in either direction.
 *
 * <p>Each direction carries:</p>
 * <ul>
 *   <li>{@code dx}/{@code dy}: the step applied to the party's X/Y when
 *       moving forward,</li>
 *   <li>{@code sideLeft}/{@code sideRight}/{@code front}: the index into a
 *       square's walls-array ({@code [N, S, E, W]} returned by
 *       {@code MazeState.getWalls}) that gives the wall on our left,
 *       on our right, and directly ahead.</li>
 * </ul>
 */
public enum Direction {
    //       dx  dy  left  right  front     (indices into [N,S,E,W])
    NORTH(    0,  1,   3,    2,    0),
    EAST (    1,  0,   0,    1,    2),
    SOUTH(    0, -1,   2,    3,    1),
    WEST (   -1,  0,   1,    0,    3);

    public final int dx;
    public final int dy;
    /** Index of the wall that appears on our left when facing this direction. */
    public final int sideLeft;
    /** Index of the wall that appears on our right when facing this direction. */
    public final int sideRight;
    /** Index of the wall directly in front when facing this direction. */
    public final int front;

    Direction(int dx, int dy, int sideLeft, int sideRight, int front) {
        this.dx = dx;
        this.dy = dy;
        this.sideLeft = sideLeft;
        this.sideRight = sideRight;
        this.front = front;
    }

    /** Turn 90° counter-clockwise (N→W→S→E→N). */
    public Direction turnLeft() {
        return values()[(ordinal() + 3) & 3];
    }

    /** Turn 90° clockwise (N→E→S→W→N). */
    public Direction turnRight() {
        return values()[(ordinal() + 1) & 3];
    }

    public static Direction fromOrdinal(int ord) {
        return values()[ord & 3];
    }
}
