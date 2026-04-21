package org.pixs.mazemaster;

/**
 * Catalog of the fixed-length PETSCII strings baked into the ROM image.
 * Each constant carries its start address and length so callers become
 * {@code console.putText(RomText.KILLED_ONE)} instead of
 * {@code displayString(0xBC5B, 0x0A)}.
 *
 * <p>{@code $FF}-terminated strings are <em>not</em> captured here — they
 * live inside concatenated blocks where the consumer tracks its own offset
 * across consecutive {@link PetsciiConsole#putRomStringTerminated(int)}
 * calls. For those, see the {@code *_BLOCK} address constants on
 * {@link Rom}.</p>
 */
public enum RomText {

    // -- Combat message window (all live inside $BC00..$BC97) --
    FIGHT_OR_RUN      (0xBC00, 0x10),  // "(F)IGHT OR (R)UN "
    WEAP_OR_SPELL     (0xBC10, 0x10),  // "(W)EAP OR (S)PEL "
    YOU_GO_AWAY       (0xBC20, 0x0F),  // "YOU GO AWAY..."
    THEY_CAUGHT_YOU   (0xBC2F, 0x0F),  // "THEY CAUGHT YOU"
    CAST_A_SPELL      (0xBC3E, 0x10),  // "CAST A SPELL... "
    HITS_FOR_PTS      (0xBC4E, 0x0D),  // "HITS FOR  PTS" — damage digit goes at offset 9
    KILLED_ONE        (0xBC5B, 0x0A),  // "KILLED ONE"
    MISSED            (0xBC65 + 3, 6), // "MISSED" — the 3-byte prefix is space padding
    TAKES_DAMAGE      (0xBC6E, 0x0D),  // "TAKES  DAMAGE" — damage digit at offset 5
    AND_IS_KILLED     (0xBC7B, 0x0D),  // "AND IS KILLED"
    SPELL_NUMBER      (0xBC88, 0x0E),  // "SPELL NUMBER: "

    // -- Messages on the main maze screen --
    MONSTERS_ATTACK   (0xA9A4, 0x0F),  // "MONSTERS ATTACK"
    DODGES_THE_BLOW   (0xB7B1, 0x0F),  // "DODGES THE BLOW"

    // -- Character creation & menus --
    RANDOMIZE_YN      (0xA9B3, 0x10),  // "RANDOMIZE? (Y-N)"
    CLASS_WAR_WIZ     (0xA717, 0x15),  // "CLASS (1-WAR  2-WIZ):"
    WHO_WILL_CAST     (0xA7D5, 0x1B),  // "WHO WILL CAST? 1-3 (0:NONE)"
    FOR_COMBAT_ONLY   (0xA7F0, 0x0F),  // "FOR COMBAT ONLY"
    LACK_SPELL_PTS    (0xA7F0 + 0x0F, 0x1D - 0x0F), // "LACK SPELL PTS"
    SPELL_PTS_LABEL   (0xA771, 0x0B),  // "SPELL PTS: "

    WHO_WILL_BUY      (0xA77C, 0x1A),  // "WHO WILL BUY? 1-3 (0:NONE)"
    ITEM_TYPE_PROMPT  (0xA796, 0x1B),  // "TYPE (1-WE 2-AR 3-SH 4-MI)?"
    ITEM_NUMBER_PROMPT(0xA7B1, 0x12),  // "ITEM NUMBER?: (1-4)"
    INSUFFICIENT_FUNDS(0xA7C3, 0x12),  // "INSUFFICIENT FUNDS"

    DELETE_CHARACTER  (0xB7E2, 0x1D),  // "DELETE CHARACTER 1-3 (0:NONE)"
    EXAMINE_WHICH     (0xA739, 0x1A),  // "EXAMINE WHICH 1-3 (0-NONE)"

    // -- Static labels shared across screens (inside the $A753 block) --
    THE               (0xA753 + 0x00, 0x04),       // "THE "
    ITEMS_LABEL       (0xA753 + 0x04, 0x0A - 0x04),// "ITEMS:"
    HIT_ANY_KEY       (0xA753 + 0x0A, 0x14),       // "HIT ANY KEY TO GO ON"

    WARRIOR           (0xA72C + 0x00, 7),          // "WARRIOR"
    WIZARD            (0xA72C + 0x07, 6),          // "WIZARD "

    // -- CODE entry --
    CODE_LABEL        (0xA9F4, 6),                 // "CODE: "

    // -- Main title --
    MAZE_BANNER       (0xA5B0, 0x10),              // " MAZE "
    MASTER_BANNER     (0xA5C0, 0x10),              // " MASTER"
    BY_MICHAEL_CRANFORD(0xA5D0, 0x16),             // "|BY MICHAEL CRANFORD |"
    COPYRIGHT_HES     (0xA9C3, 0x16),              // "COPYRIGHT (C) 1983 HES"
    CHARACTER_NAME    (0xA5E6, 0x0E),              // "CHARACTER NAME"
    MAZE_VIEW_FOOTER  (0xA58D, 0x22),              // "CHARACTER NAME   ARM CON CND CLASS"

    // -- ORIENT spell message ($BC96 block) --
    ORIENT_YOU_ARE        (0xBC96 + 0x00, 0x08),   // "YOU ARE "
    ORIENT_SPACES         (0xBC96 + 0x08, 0x07),   // " SPACES"
    ORIENT_NORTH          (0xBC96 + 0x0F, 0x07),   // "NORTH, "
    ORIENT_EAST           (0xBC96 + 0x16, 0x07),   // "EAST, "
    ORIENT_AND            (0xBC96 + 0x1D, 0x0A),   // "AND  LEVEL"
    ORIENT_LEVELS_BELOW   (0xBC96 + 0x27, 0x10),   // "LEVELS BELOW THE"
    ORIENT_ENTRY_STAIRS   (0xBC96 + 0x37, 0x0D),   // "ENTRY STAIRS,"
    ORIENT_NOW_FACING     (0xBC96 + 0x44, 0x0B),   // "NOW FACING "

    // -- TELEPORT spell message ($BCF9 block) --
    TELEPORT_TITLE        (0xBCF9 + 0x00, 0x0B),   // "TELEPORT..."
    TELEPORT_LEVELS_DOWN  (0xBCF9 + 0x0B, 0x0D),   // "LEVELS DOWN: "
    TELEPORT_NORTH_PROMPT (0xBCF9 + 0x18, 0x07),   // "NORTH: "
    TELEPORT_EAST_PROMPT  (0xBCF9 + 0x1F, 0x06);   // "EAST: "

    public final int address;
    public final int length;

    RomText(int address, int length) {
        this.address = address;
        this.length = length;
    }
}
