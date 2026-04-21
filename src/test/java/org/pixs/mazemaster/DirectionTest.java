package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Direction's ordinals must match the ROM's zero-page $65 encoding.
 * The (dx, dy) steps must match the hardcoded switch that goForwardForce
 * used before (ASM jumps in the goForward / castOrient paths).
 */
class DirectionTest {

    @Test
    void ordinalsMatchRomEncoding() {
        assertEquals(0, Direction.NORTH.ordinal());
        assertEquals(1, Direction.EAST.ordinal());
        assertEquals(2, Direction.SOUTH.ordinal());
        assertEquals(3, Direction.WEST.ordinal());
    }

    @Test
    void dxDyMatchForwardStep() {
        assertEquals( 0, Direction.NORTH.dx); assertEquals( 1, Direction.NORTH.dy);
        assertEquals( 1, Direction.EAST.dx);  assertEquals( 0, Direction.EAST.dy);
        assertEquals( 0, Direction.SOUTH.dx); assertEquals(-1, Direction.SOUTH.dy);
        assertEquals(-1, Direction.WEST.dx);  assertEquals( 0, Direction.WEST.dy);
    }

    @Test
    void turnLeftGoesCounterClockwise() {
        assertSame(Direction.WEST, Direction.NORTH.turnLeft());
        assertSame(Direction.SOUTH, Direction.WEST.turnLeft());
        assertSame(Direction.EAST, Direction.SOUTH.turnLeft());
        assertSame(Direction.NORTH, Direction.EAST.turnLeft());
    }

    @Test
    void turnRightGoesClockwise() {
        assertSame(Direction.EAST, Direction.NORTH.turnRight());
        assertSame(Direction.SOUTH, Direction.EAST.turnRight());
        assertSame(Direction.WEST, Direction.SOUTH.turnRight());
        assertSame(Direction.NORTH, Direction.WEST.turnRight());
    }

    @Test
    void sideIndicesMatchOriginalWallsFacingTables() {
        // Each direction returns a 5-slot array [left, right, front, left-front, right-front]
        // where the first three indices are into the [N, S, E, W] walls array.
        assertEquals(3, Direction.NORTH.sideLeft);   // west is on our left
        assertEquals(2, Direction.NORTH.sideRight);  // east on our right
        assertEquals(0, Direction.NORTH.front);      // north is forward

        assertEquals(0, Direction.EAST.sideLeft);    // north on our left
        assertEquals(1, Direction.EAST.sideRight);   // south on our right
        assertEquals(2, Direction.EAST.front);

        assertEquals(2, Direction.SOUTH.sideLeft);   // east on our left
        assertEquals(3, Direction.SOUTH.sideRight);  // west on our right
        assertEquals(1, Direction.SOUTH.front);

        assertEquals(1, Direction.WEST.sideLeft);    // south on our left
        assertEquals(0, Direction.WEST.sideRight);   // north on our right
        assertEquals(3, Direction.WEST.front);
    }
}
