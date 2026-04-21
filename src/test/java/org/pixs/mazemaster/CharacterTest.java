package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Character-data layout verification against the ASM memory map:
 * <pre>
 *  $800-$80F : character 1 name (16 bytes)
 *  $810      : strength
 *  $811      : intellect
 *  $812      : dexterity
 *  $813      : constitution
 *  $814      : class (1=war, 2=wiz)
 *  $815-$816 : gold (LSB, MSB)
 *  $817-$818 : experience (LSB, MSB)
 *  $819-$81C : weapon, armor, shield, magical item
 * </pre>
 */
class CharacterTest {

    @Test
    void nameAsBytesPadsShortNamesWithSpaces() {
        Character c = new Character();
        c.setName("AB");
        byte[] name = c.getNameAsBytes();

        assertEquals(16, name.length);
        assertEquals(0x0A, name[0]); // 'A' screen code
        assertEquals(0x0B, name[1]); // 'B' screen code
        for (int i = 2; i < 16; i++) {
            assertEquals(0x24, name[i], "byte " + i + " should be the space screen code");
        }
    }

    @Test
    void isValidRequiresNonSpaceFirstChar() {
        Character empty = new Character();
        assertFalse(empty.isValid(), "default-constructed character has no name → invalid");

        Character named = new Character();
        named.setName("FRODO");
        assertTrue(named.isValid());
    }

    @Test
    void setNameFromByteRoundTrip() {
        byte[] screenCodes = new byte[16];
        // "HOBBIT          " in screen codes: H=$11, O=$18, B=$0B, B=$0B, I=$12, T=$1D
        screenCodes[0] = 0x11;
        screenCodes[1] = 0x18;
        screenCodes[2] = 0x0B;
        screenCodes[3] = 0x0B;
        screenCodes[4] = 0x12;
        screenCodes[5] = 0x1D;
        for (int i = 6; i < 16; i++) screenCodes[i] = 0x24;

        Character c = new Character();
        c.setNameFromByte(screenCodes);

        byte[] back = c.getNameAsBytes();
        for (int i = 0; i < 16; i++) {
            assertEquals(screenCodes[i], back[i], "name byte " + i);
        }
    }

    /**
     * Regression for the "Character.getRawBytes copied only 15 bytes of the
     * name" bug: the ROM structure places strength at $810, so the first
     * 16 bytes (offset 0..0x0F) must all be the name.
     */
    @Test
    void getRawBytesIncludesFull16BytesOfName() {
        Character c = new Character();
        c.setName("ABCDEFGHIJKLMNOP"); // 16 characters
        byte[] raw = c.getRawBytes();

        for (int i = 0; i < 0x10; i++) {
            assertEquals(0x0A + i, raw[i], "expected full name at offset " + i);
        }
    }

    @Test
    void getRawBytesWritesAttributesAtExpectedOffsets() {
        Character c = new Character();
        c.setName("X");
        c.setStrength(18);
        c.setIntellect(16);
        c.setDexterity(14);
        c.setConstitution(200);
        c.setClassType(1);
        c.setGold(0x1234);
        c.setXP(0xBEEF);
        c.setItem(0, (byte) 4); // wrathblade
        c.setItem(1, (byte) 3); // magic armor
        c.setItem(2, (byte) 2); // magic shield
        c.setItem(3, (byte) 1); // staff of light

        byte[] raw = c.getRawBytes();

        assertEquals(18, raw[0x10]);
        assertEquals(16, raw[0x11]);
        assertEquals(14, raw[0x12]);
        assertEquals((byte) 200, raw[0x13]);
        assertEquals(1, raw[0x14]);
        assertEquals(0x34, raw[0x15] & 0xFF);
        assertEquals(0x12, raw[0x16] & 0xFF);
        assertEquals(0xEF, raw[0x17] & 0xFF);
        assertEquals(0xBE, raw[0x18] & 0xFF);
        assertEquals(4, raw[0x19]);
        assertEquals(3, raw[0x1A]);
        assertEquals(2, raw[0x1B]);
        assertEquals(1, raw[0x1C]);
    }

    @Test
    void setRawBytesIsSymmetricForAttributeBlock() {
        Character c = new Character();
        c.setName("Y");
        c.setStrength(17);
        c.setConstitution(255);
        c.setGold(65500);
        c.setXP(0xFF00);
        c.setClassType(2);
        c.setItem(0, (byte) 2);
        c.setItem(1, (byte) 1);
        c.setItem(2, (byte) 4);
        c.setItem(3, (byte) 3);

        byte[] raw = c.getRawBytes();
        Character target = new Character();
        target.setRawBytes(raw);

        assertEquals(17, target.getStrength());
        assertEquals(255, target.getConstitution());
        assertEquals(65500, target.getGold());
        assertEquals(0xFF00, target.getXp());
        assertEquals(2, target.getClassType());
        assertEquals(2, target.getItemCode(0));
        assertEquals(1, target.getItemCode(1));
        assertEquals(4, target.getItemCode(2));
        assertEquals(3, target.getItemCode(3));
    }
}
