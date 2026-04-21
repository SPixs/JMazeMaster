package org.pixs.mazemaster;

/**
 * Façade over the 64KB memory image loaded from {@code maze_master.bin}.
 * Every raw hex address that used to be sprinkled through the game code is
 * named here, with the source ASM address in the Javadoc.
 *
 * <p>Two levels of access are exposed:</p>
 * <ul>
 *   <li>{@link #getByte(int)} / {@link #getByteU(int)} / {@link #getWord(int)}
 *       for generic reads (mirrors ASM {@code LDA}/{@code LDX}/{@code LDY}),</li>
 *   <li>domain accessors ({@code monsterHP}, {@code itemPrice}, ...) that
 *       encapsulate the address arithmetic for each ROM table.</li>
 * </ul>
 *
 * <p>All byte accessors return {@code int} in 0..255 so callers never have to
 * remember to mask with {@code & 0xFF}.</p>
 */
public final class Rom {

    public static final int SIZE = 65536;

    private final byte[] data;

    public Rom(byte[] image) {
        if (image.length != SIZE) {
            throw new IllegalArgumentException("ROM image must be 64KB, got " + image.length);
        }
        this.data = image;
    }

    /** Raw signed byte at the given address. Prefer {@link #getByteU(int)} unless you specifically need sign-extension. */
    public byte getByte(int address)  { return data[address]; }

    /** Byte at the given address, widened as unsigned (0..255). */
    public int  getByteU(int address) { return data[address] & 0xFF; }

    /** Little-endian 16-bit word at (lsbAddress, lsbAddress+1). */
    public int  getWord(int lsbAddress) {
        return getByteU(lsbAddress) | (getByteU(lsbAddress + 1) << 8);
    }

    /** Writes a byte, used by gameplay to disable a fired trigger (stores $FF). */
    public void setByte(int address, byte value) { data[address] = value; }

    // ------------------------------------------------------------------
    // Domain accessors. Each cites the ASM label/memory-map entry it covers.
    // ------------------------------------------------------------------

    // -- Monsters ($A448/$A470/$A498/$A4C0) --

    /** Initial HP of monster {@code id} ($A498 + id). */
    public int monsterHP(int id)          { return getByteU(0xA498 + id); }

    /** Monster attack bonus — used for both hit chance and extra-damage rolls ($A470 + id). */
    public int monsterAttackBonus(int id) { return getByteU(0xA470 + id); }

    /** Monster armor rating (0..$14 maps to -10..+10) ($A4C0 + id). */
    public int monsterAR(int id)          { return getByteU(0xA4C0 + id); }

    /** Address of the null-terminated monster name string (routed through $A448 for ids &lt; 27, $A463 otherwise). */
    public int monsterNameAddress(int id) {
        return id < 27
                ? 0xA80D + getByteU(0xA448 + id)
                : 0xA90B + getByteU(0xA463 + id - 27);
    }

    // -- Equipment ($A3EB/$A3F0/$A407/$A42C/$BF00/$BFC0) --

    /** Base armor rating value for armor {@code id} ($A3F0 + id); none=$14, leather=$11, ..., mithril=$08. */
    public int armorRating(int armorId)          { return getByteU(0xA3F0 + armorId); }

    /** AR reduction granted by shield {@code id} ($A3EB + id); none=0, shield=1, ..., deflector=4. */
    public int shieldReduction(int shieldId)     { return getByteU(0xA3EB + shieldId); }

    /** Weapon damage mask ($A407 + id); None=$03, Sword=$07, ..., Wrathblade=$3F. */
    public int weaponDamageMask(int weaponId)    { return getByteU(0xA407 + weaponId); }

    /**
     * Price of the given item. Prices live in $BFC0 as little-endian 16-bit words
     * indexed by (4 * category + item).
     */
    public int itemPrice(int category, int itemIndex) {
        return getWord(0xBFC0 + (4 * category + itemIndex) * 2);
    }

    /**
     * Address of the null-terminated item name string for slot {@code slot}
     * (0=weapon, 1=armor, 2=shield, 3=magic item) and item code {@code itemCode}
     * (1..4 for a real item, 0 meaning "empty"). Names themselves start at $BF00;
     * $A42C holds their one-byte offsets.
     */
    public int itemNameAddress(int slot, int itemCode) {
        return 0xBF00 + getByteU(0xA42C + itemCode + slot * 4);
    }

    // -- Spells ($A3C0/$A3D3/$A3F5) --

    /** {@code true} if spell {@code n} is a combat spell (ASM table $A3C0[n] == 0). */
    public boolean isCombatSpell(int spellNumber) { return getByte(0xA3C0 + spellNumber) == 0; }

    /** Spell-point cost ($A3D3 + n). */
    public int spellPointsCost(int spellNumber)   { return getByteU(0xA3D3 + spellNumber); }

    /** Damage mask for combat spells, AR reduction for protection spells ($A3F5 + n). */
    public int spellDamageMask(int spellNumber)   { return getByteU(0xA3F5 + spellNumber); }

    // -- Combat maths ($5300, $A3E6) --

    /** Dodge score matching an AR (0..$14) — $1C for AR -10 down to $08 for AR +10. */
    public int dodgeScore(int armorRating)        { return getByteU(0x5300 + armorRating); }

    /**
     * Wandering-monster XP threshold for a floor: monsters are enabled while
     * the party's total (xp/1024) stays below this value ($A3E6 + level).
     */
    public int wanderingThreshold(int level)      { return getByteU(0xA3E6 + level); }

    // -- Maze layout --

    /**
     * Wall-definition byte for the square at (x, y) on the given floor.
     * Bits are laid out {@code wweessnn} (west/east/south/north, 2 bits each).
     * Level N table starts at $AA00 + N*$200.
     */
    public byte wallsByte(int level, int x, int y) {
        return getByte(0xAA00 + (level << 9) + y * 20 + x);
    }

    /**
     * Raw trigger data for a floor, copied into RAM by {@link Game} at startup.
     * 112 bytes organised as (y, x) pairs: first 4 pairs are upstairs,
     * downstairs, clue, hole, the rest are monster encounters.
     */
    public int triggerTableStart(int level) {
        return 0xAB90 + (level << 9);
    }

    // -- Character data in RAM ($0800 / $0900 / $0A00) --
    // Note: gameplay holds Character objects; these helpers are only useful if a
    // component wants to read raw character bytes straight from the memory image.

    /** Base address of character slot {@code slot} (0..2). */
    public int characterBase(int slot) { return 0x0800 + slot * 0x100; }
}
