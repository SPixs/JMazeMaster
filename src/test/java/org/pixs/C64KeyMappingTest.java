package org.pixs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Mirrors the ROM's key-table handling. The original game reads PETSCII bytes
 * straight out of $FFE4 GETIN; our Java layer translates scan codes through
 * {@link C64KeyMapping#SCAN_TO_PETSCII} and delegates for the two
 * cursor-up/cursor-left extensions handled specially by SwingKeyboard.
 */
class C64KeyMappingTest {

    @Test
    void digitScanCodesProduceAsciiDigits() {
        assertEquals((byte) '0', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_0));
        assertEquals((byte) '1', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_1));
        assertEquals((byte) '5', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_5));
        assertEquals((byte) '9', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_9));
    }

    @Test
    void letterScanCodesProduceAsciiUppercase() {
        assertEquals((byte) 'A', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_A));
        assertEquals((byte) 'M', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_M));
        assertEquals((byte) 'Z', C64KeyMapping.scanToPETSCII(IKeyboard.KEY_Z));
    }

    @Test
    void returnKeyMapsToPetsciiCarriageReturn() {
        assertEquals((byte) 0x0D, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_RETURN));
    }

    @Test
    void spaceKeyMapsToPetsciiSpace() {
        assertEquals((byte) 0x20, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_SPACE));
    }

    @Test
    void cursorKeysMapToPetsciiCursorCodes() {
        assertEquals((byte) 0x1D, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_CRSR_RIGHT));
        assertEquals((byte) 0x91, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_CRSR_DOWN));
        // KEY_CRSR_UP and KEY_CRSR_LEFT are handled by the switch tail.
        assertEquals((byte) 0xD1, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_CRSR_UP));
        assertEquals((byte) 0x9D, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_CRSR_LEFT));
    }

    @Test
    void modifierAndUnmappedReturnZero() {
        // Shift has no character. These used to throw IllegalStateException;
        // the fix makes them return 0 so the EDT cannot crash on stray keys.
        assertEquals((byte) 0x00, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_LSHIFT));
        assertEquals((byte) 0x00, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_RSHIFT));
        assertEquals((byte) 0x00, C64KeyMapping.scanToPETSCII(IKeyboard.KEY_CTRL));
        // Any scan code beyond the low-64 table and not CRSR_UP/CRSR_LEFT.
        assertEquals((byte) 0x00, C64KeyMapping.scanToPETSCII((byte) 0x80));
        assertEquals((byte) 0x00, C64KeyMapping.scanToPETSCII((byte) 0xFE));
    }
}
