package org.pixs.mazemaster.states;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Isolated tests for the pure-logic helpers carved out of MazeState. Each test
 * pins behaviour to the corresponding ASM subroutine comment.
 */
class MazeStateLogicTest {

    // -------------------- convertToWord — ASM s9F9D --------------------

    @Test
    void convertToWordSingleDigitReturnsItsValue() {
        byte[] buf = { 5, 0x24, 0x24 };
        assertEquals(5, MazeState.convertToWord(buf));
    }

    @Test
    void convertToWordMultiDigit() {
        byte[] buf = { 1, 5, 0x24 };
        assertEquals(15, MazeState.convertToWord(buf));
    }

    @Test
    void convertToWordThreeDigits() {
        byte[] buf = { 2, 5, 5 };
        assertEquals(255, MazeState.convertToWord(buf));
    }

    @Test
    void convertToWordSpaceEndsNumber() {
        // "5 " : space terminates even though there are more slots.
        byte[] buf = { 5, 0x24, 5 };
        assertEquals(5, MazeState.convertToWord(buf));
    }

    /**
     * ROM behaviour: a non-digit screen code invalidates the whole string
     * (ASM s9F9D `CMP #$0A / BCC b9FC6` else zeroes a75/a76 and returns).
     * Before the fix the Java version treated letters as 10..35 and silently
     * produced numeric values, so typing "A" selected spell 10 (GUARDIAN).
     */
    @Test
    void convertToWordRejectsLetterAsInvalid() {
        byte[] buf = { 1, 0x0A, 0x24 }; // "1A"
        assertEquals(0, MazeState.convertToWord(buf));
    }

    @Test
    void convertToWordRejectsTrailingLetter() {
        byte[] buf = { 5, 0x0A, 0x24 }; // "5A"
        assertEquals(0, MazeState.convertToWord(buf));
    }

    @Test
    void convertToWordEmptyBufferReturnsZero() {
        byte[] buf = { 0x24, 0x24, 0x24 };
        assertEquals(0, MazeState.convertToWord(buf));
    }

    // -------------------- checkOverflow16bits — ASM b9BBF / b9BD3 --------------------

    @Test
    void checkOverflowBelowCapIsPassThrough() {
        assertEquals(0, MazeState.checkOverflow16bits(0));
        assertEquals(1234, MazeState.checkOverflow16bits(1234));
        assertEquals(0xFFFF, MazeState.checkOverflow16bits(0xFFFF));
    }

    @Test
    void checkOverflowSaturatesMsbToFFAndKeepsLowByte() {
        // 0x10003 → MSB overflows. ASM stores $FF as MSB and the wrapped LSB.
        assertEquals(0xFF03, MazeState.checkOverflow16bits(0x10003));
        assertEquals(0xFF50, MazeState.checkOverflow16bits(0x10050));
        assertEquals(0xFF00, MazeState.checkOverflow16bits(0x10000));
    }
}
