package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the Rom façade to the exact address arithmetic in memory-map.txt.
 * Each test seeds a 64KB buffer, wraps it in Rom, then checks the domain
 * accessor returns the expected value. The addresses themselves (the magic
 * numbers) are verified implicitly — if Rom's arithmetic ever drifts, these
 * tests fail.
 */
class RomTest {

    private byte[] freshMemory() { return new byte[Rom.SIZE]; }

    // -- Basic byte/word accessors --

    @Test
    void getByteUMasksSignExtension() {
        byte[] mem = freshMemory();
        mem[0x100] = (byte) 0xFF;      // would be -1 as signed byte
        mem[0x101] = (byte) 0x80;      // would be -128 as signed byte
        Rom rom = new Rom(mem);

        assertEquals(0xFF, rom.getByteU(0x100));
        assertEquals(0x80, rom.getByteU(0x101));
        assertEquals((byte) 0xFF, rom.getByte(0x100));
    }

    @Test
    void getWordReadsLittleEndian() {
        byte[] mem = freshMemory();
        mem[0x200] = 0x34;
        mem[0x201] = (byte) 0xBE;      // high byte
        Rom rom = new Rom(mem);

        assertEquals(0xBE34, rom.getWord(0x200));
    }

    @Test
    void constructorRejectsWrongSize() {
        assertThrows(IllegalArgumentException.class, () -> new Rom(new byte[1024]));
        assertThrows(IllegalArgumentException.class, () -> new Rom(new byte[65535]));
    }

    // -- Monster tables --

    @Test
    void monsterHPReadsFromA498() {
        byte[] mem = freshMemory();
        mem[0xA498 + 39] = (byte) 0xFF; // Balrog
        mem[0xA498 + 0]  = 0x08;
        Rom rom = new Rom(mem);

        assertEquals(0xFF, rom.monsterHP(39));
        assertEquals(0x08, rom.monsterHP(0));
    }

    @Test
    void monsterAttackBonusReadsFromA470() {
        byte[] mem = freshMemory();
        mem[0xA470 + 39] = 0x1F; // Balrog
        Rom rom = new Rom(mem);

        assertEquals(0x1F, rom.monsterAttackBonus(39));
    }

    @Test
    void monsterARReadsFromA4C0() {
        byte[] mem = freshMemory();
        mem[0xA4C0 + 0] = 0x14;
        mem[0xA4C0 + 39] = 0x00;
        Rom rom = new Rom(mem);

        assertEquals(0x14, rom.monsterAR(0));
        assertEquals(0x00, rom.monsterAR(39));
    }

    @Test
    void monsterNameAddressSwitchesTableAt27() {
        byte[] mem = freshMemory();
        // Monster 5 (id < 27): name offset stored at A448 + 5.
        mem[0xA448 + 5] = 0x10;
        // Monster 30 (id >= 27): name offset stored at A463 + (30-27).
        mem[0xA463 + 3] = 0x42;
        Rom rom = new Rom(mem);

        assertEquals(0xA80D + 0x10, rom.monsterNameAddress(5));
        assertEquals(0xA90B + 0x42, rom.monsterNameAddress(30));
    }

    // -- Combat maths --

    @Test
    void dodgeScoreReadsFromC64InitTable() {
        // The binary we ship already has the precomputed dodge table
        // ($1C..$08) at $5300..$5314. Simulate it here.
        byte[] mem = freshMemory();
        for (int i = 0; i < 0x15; i++) {
            mem[0x5300 + i] = (byte) (0x1C - i);
        }
        Rom rom = new Rom(mem);

        assertEquals(0x1C, rom.dodgeScore(0));      // AR -10 → highest dodge
        assertEquals(0x12, rom.dodgeScore(0x0A));   // AR  0
        assertEquals(0x08, rom.dodgeScore(0x14));   // AR +10 → lowest dodge
    }

    @Test
    void wanderingThresholdReadsFromA3E6() {
        byte[] mem = freshMemory();
        mem[0xA3E6 + 0] = 0x00;
        mem[0xA3E6 + 4] = 0x1A;
        Rom rom = new Rom(mem);

        assertEquals(0x00, rom.wanderingThreshold(0));
        assertEquals(0x1A, rom.wanderingThreshold(4));
    }

    // -- Items --

    @Test
    void itemPriceEncodesCategoryTimesFourPlusIndexDoubled() {
        // Memory layout: $BFC0 + (4*cat + idx)*2 holds a little-endian 16-bit price.
        byte[] mem = freshMemory();
        int offset = (4 * 2 + 3) * 2;   // category 2 (shield), item 3 (ward shield)
        mem[0xBFC0 + offset]     = (byte) 0xD0;
        mem[0xBFC0 + offset + 1] = 0x07;          // 0x07D0 = 2000
        Rom rom = new Rom(mem);

        assertEquals(2000, rom.itemPrice(2, 3));
    }

    @Test
    void itemNameAddressRemapsThroughA42C() {
        byte[] mem = freshMemory();
        // Magic item slot (slot 3), item code 4 (Hawk blazon): A42C[16] = $AF.
        mem[0xA42C + 16] = (byte) 0xAF;
        Rom rom = new Rom(mem);

        assertEquals(0xBF00 + 0xAF, rom.itemNameAddress(3, 4));
    }

    @Test
    void weaponDamageMaskReadsFromA407() {
        byte[] mem = freshMemory();
        mem[0xA407 + 0] = 0x03; // None
        mem[0xA407 + 4] = 0x3F; // Wrathblade
        Rom rom = new Rom(mem);

        assertEquals(0x03, rom.weaponDamageMask(0));
        assertEquals(0x3F, rom.weaponDamageMask(4));
    }

    @Test
    void armorAndShieldReadFromTheirTables() {
        byte[] mem = freshMemory();
        mem[0xA3F0 + 0] = 0x14;
        mem[0xA3F0 + 4] = 0x08;
        mem[0xA3EB + 4] = 0x04;
        Rom rom = new Rom(mem);

        assertEquals(0x14, rom.armorRating(0));
        assertEquals(0x08, rom.armorRating(4));
        assertEquals(0x04, rom.shieldReduction(4));
    }

    // -- Spells --

    @Test
    void isCombatSpellFlagsZeroMeansCombat() {
        byte[] mem = freshMemory();
        mem[0xA3C0 + 1]  = 0x00; // FIREBALL — combat
        mem[0xA3C0 + 3]  = 0x01; // HEAL — non-combat
        mem[0xA3C0 + 15] = 0x01; // SHADOW SHIELD — non-combat per A3C0
        mem[0xA3C0 + 17] = 0x00; // FLAME FURY — combat
        Rom rom = new Rom(mem);

        assertTrue(rom.isCombatSpell(1));
        assertFalse(rom.isCombatSpell(3));
        assertFalse(rom.isCombatSpell(15));
        assertTrue(rom.isCombatSpell(17));
    }

    @Test
    void spellPointsCostReadsFromA3D3() {
        byte[] mem = freshMemory();
        mem[0xA3D3 + 1] = 0x01;
        mem[0xA3D3 + 17] = 0x06;
        Rom rom = new Rom(mem);

        assertEquals(1, rom.spellPointsCost(1));
        assertEquals(6, rom.spellPointsCost(17));
    }

    @Test
    void spellDamageMaskReadsFromA3F5() {
        byte[] mem = freshMemory();
        mem[0xA3F5 + 1]  = 0x1F; // FIREBALL mask — 1..32
        mem[0xA3F5 + 17] = 0x3F; // FLAME FURY mask — 1..64
        Rom rom = new Rom(mem);

        assertEquals(0x1F, rom.spellDamageMask(1));
        assertEquals(0x3F, rom.spellDamageMask(17));
    }

    // -- Maze layout --

    @Test
    void wallsByteComputesLevelOffsetShift9() {
        byte[] mem = freshMemory();
        // Level 2, square (3, 4): $AA00 + 2*$200 + 4*20 + 3 = $AE53.
        mem[0xAA00 + 2 * 0x200 + 4 * 20 + 3] = 0x55;
        Rom rom = new Rom(mem);

        assertEquals((byte) 0x55, rom.wallsByte(2, 3, 4));
    }

    @Test
    void triggerTableStartFollowsSameShift() {
        assertEquals(0xAB90, new Rom(new byte[Rom.SIZE]).triggerTableStart(0));
        assertEquals(0xAD90, new Rom(new byte[Rom.SIZE]).triggerTableStart(1));
        assertEquals(0xB390, new Rom(new byte[Rom.SIZE]).triggerTableStart(4));
    }

    @Test
    void setByteWritesIntoMemory() {
        byte[] mem = freshMemory();
        Rom rom = new Rom(mem);
        rom.setByte(0x1234, (byte) 0xAB);

        assertEquals(0xAB, rom.getByteU(0x1234));
    }

    // -- Triggers, sprites, encoding --

    @Test
    void triggerMessageAddressCombinesLsbAndMsb() {
        byte[] mem = freshMemory();
        mem[0xA43D + 3] = 0x17;   // LSB for slot 3
        mem[0xB4E1 + 3] = (byte) 0xBB; // MSB
        Rom rom = new Rom(mem);

        assertEquals(0xBB17, rom.triggerMessageAddress(3));
    }

    @Test
    void monsterSpriteTopAddressAppliesRomQuirkOnlyWhenRawByteIsZero() {
        // The Java port (and presumably the ASM) compares the already-shifted
        // top address to the literal $B0, not to $B000. That condition only
        // triggers when the raw descriptor byte is 0 (so shifted == 0).
        // Preserved faithfully — tests pin the current behaviour, not the
        // likely-intended "< $B000".
        byte[] mem = freshMemory();
        mem[0xBE62 + 5 * 4] = 0x00;
        Rom rom = new Rom(mem);

        // Fixup: ((0 + $B0) & $FF00) | $80 = 0 | 0x80 = 0x80
        assertEquals(0x80, rom.monsterSpriteTopAddress(5));
    }

    @Test
    void monsterSpriteTopAddressNormalValue() {
        byte[] mem = freshMemory();
        mem[0xBE62 + 2 * 4] = (byte) 0xB4;
        Rom rom = new Rom(mem);

        assertEquals(0xB400, rom.monsterSpriteTopAddress(2));
    }

    @Test
    void starLineGatheresFourParallelTables() {
        byte[] mem = freshMemory();
        mem[0xA56D + 3] = 0x10; // startY
        mem[0xA575 + 3] = 0x20; // startX
        mem[0xA57D + 3] = 0x30; // endY
        mem[0xA585 + 3] = 0x40; // endX
        Rom rom = new Rom(mem);

        int[] coords = rom.starLine(3);
        assertEquals(0x10, coords[0]);
        assertEquals(0x20, coords[1]);
        assertEquals(0x30, coords[2]);
        assertEquals(0x40, coords[3]);
    }

    @Test
    void balrogEnigmaAnswerReadsFourBytesFromA428() {
        byte[] mem = freshMemory();
        mem[0xA428] = 0x0F; // F
        mem[0xA429] = 0x0A; // A
        mem[0xA42A] = 0x1D; // T
        mem[0xA42B] = 0x0E; // E
        Rom rom = new Rom(mem);

        byte[] answer = rom.balrogEnigmaAnswer();
        assertEquals(4, answer.length);
        assertEquals(0x0F, answer[0]);
        assertEquals(0x0A, answer[1]);
        assertEquals(0x1D, answer[2]);
        assertEquals(0x0E, answer[3]);
    }

    @Test
    void directionNameOffsetReadsA424Table() {
        byte[] mem = freshMemory();
        mem[0xA424 + 0] = 0x00;
        mem[0xA424 + 1] = 0x05;
        mem[0xA424 + 2] = 0x0A;
        mem[0xA424 + 3] = 0x0F;
        Rom rom = new Rom(mem);

        assertEquals(0x00, rom.directionNameOffset(0));
        assertEquals(0x05, rom.directionNameOffset(1));
        assertEquals(0x0A, rom.directionNameOffset(2));
        assertEquals(0x0F, rom.directionNameOffset(3));
    }

    @Test
    void codeFlippingByteAndNibbleFlagReadTheirTables() {
        byte[] mem = freshMemory();
        mem[0xA396 + 2] = 0x10;
        mem[0xA3AB + 2] = 0x01;
        Rom rom = new Rom(mem);

        assertEquals(0x10, rom.codeFlippingByte(2));
        assertEquals(0x01, rom.codeNibbleFlag(2));
    }
}
