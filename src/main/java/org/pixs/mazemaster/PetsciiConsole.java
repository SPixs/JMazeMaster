package org.pixs.mazemaster;

import org.pixs.hardware.VicIIDisplay;

/**
 * Text output layer for the game's bitmap screen. Owns the cursor position
 * (the former {@code m_charOutputRow}/{@code m_charOutputCol} on
 * {@link org.pixs.mazemaster.states.GameState}, originally at zero-page $4A/$49)
 * and every method that blits a glyph from the custom charset into the VIC-II
 * bitmap memory.
 *
 * <p>Cursor coordinates are exposed as public mutable fields because the
 * original game manipulates them directly in numerous places (e.g., "put
 * cursor at 7,7 then output string", "decrement column to keep the blinking
 * cursor in place"). Keeping them public mirrors that idiom 1:1 and avoids a
 * flurry of getter/setter boilerplate; callers set them just as the ASM sets
 * {@code $49} and {@code $4A}.</p>
 */
public final class PetsciiConsole {

    private final Game game;

    /** Column (0..39), originally at zero-page $49. */
    public int col;
    /** Row (0..24), originally at zero-page $4A. */
    public int row;

    public PetsciiConsole(Game game) {
        this.game = game;
    }

    // -- Output ---------------------------------------------------------------

    /**
     * $9E94: copy 8 glyph bytes to the bitmap address corresponding to
     * ({@link #col}, {@link #row}), then advance the column.
     */
    public void putChar(byte code) {
        VicIIDisplay vicII = game.getVicII();
        int startAdress = (row * 40 * 8) + col * 8;
        for (int i = 0; i < 8; i++) {
            vicII.setBitmapValue(startAdress + i, game.getCharset()[code * 8 + i]);
        }
        col++;
    }

    /** Writes each byte in turn; cursor advances {@code chars.length} columns. */
    public void putChars(byte[] chars) {
        for (byte b : chars) putChar(b);
    }

    /** Writes {@code count} bytes read from the ROM starting at {@code address}. */
    public void putRomString(int address, int count) {
        for (int i = 0; i < count; i++) {
            putChar(game.getMem(address + i));
        }
    }

    /** Writes a named fixed-length message from the ROM. */
    public void putText(RomText text) {
        putRomString(text.address, text.length);
    }

    /**
     * Writes the bytes at {@code address} until hitting a $FF terminator.
     * Returns the number of bytes consumed including the terminator — useful
     * for walking a concatenated block of strings (the caller just adds the
     * returned value to its running offset).
     */
    public int putRomStringTerminated(int address) {
        int written = 0;
        byte c = game.getMem(address);
        while (c != (byte) 0xFF) {
            putChar(c);
            written++;
            c = game.getMem(address + written);
        }
        return written + 1;
    }

    /**
     * $A1ED: writes a 16-bit value as decimal digits, leading zeros suppressed
     * but always at least one digit. The digits live at screen codes 0..9 in
     * the custom charset.
     */
    public void putDecimal(int value) {
        value = value & 0x0FFFF;
        for (char c : String.valueOf(value).toCharArray()) {
            putChar((byte) (c - '0'));
        }
    }

    // -- Cursor control ------------------------------------------------------

    public void moveTo(int col, int row) {
        this.col = col;
        this.row = row;
    }

    /** sA38B: return to column 9 on the next line. */
    public void newLineAtCol9() {
        row++;
        col = 9;
    }

    /** sA392: return to column 5 on the next line. */
    public void newLineAtCol5() {
        row++;
        col = 5;
    }
}
