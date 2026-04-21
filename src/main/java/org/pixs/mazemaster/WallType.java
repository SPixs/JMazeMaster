package org.pixs.mazemaster;

/**
 * Type of a single wall edge in a maze square.
 *
 * <p>The ordinal values must stay aligned with the 2-bit encoding the ROM
 * uses in $AA00..$B3FF (two bits per side, packed {@code wweessnn} in
 * each square byte): 0=open, 1=solid wall, 2=visible door, 3=hidden door.</p>
 */
public enum WallType {
    NONE,
    WALL,
    DOOR,
    HIDDEN
}
