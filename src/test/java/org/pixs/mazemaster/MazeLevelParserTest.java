package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifies the level parser matches the ROM layout:
 * <ul>
 *   <li>walls table at $AA00 + level*$200, 20×20 bytes encoding
 *       ww ee ss nn in bits 76/54/32/10,</li>
 *   <li>trigger table at $AB90 + level*$200, 56 (y, x) pairs where
 *       the first four are the special triggers (upstairs, downstairs,
 *       clue, hole),</li>
 *   <li>wandering-monster thresholds at $A3E6 + level.</li>
 * </ul>
 */
class MazeLevelParserTest {

    @Test
    void parsesAllFiveLevelsWithCorrectWallsAndTriggers() throws Exception {
        byte[] rom = new byte[65536];

        // Seed per-level markers so we can verify the offsets are correct.
        for (int level = 0; level < 5; level++) {
            int wallsBase = 0xAA00 + level * 0x200;
            int triggersBase = 0xAB90 + level * 0x200;

            // wallType = (level+1, level+1, level+1, level+1) at every square
            // i.e. "WALL on each side" when level+1 == 1, "DOOR" when 2, etc.
            int wallCode = level + 1;
            byte square = (byte) ((wallCode) | (wallCode << 2) | (wallCode << 4) | (wallCode << 6));
            for (int i = 0; i < 400; i++) {
                rom[wallsBase + i] = square;
            }

            // Put the upstairs at (2, 3), downstairs at (2, 4), clue at (5, 6),
            // hole at (7, 8); then fill the rest with $FF so they never match.
            rom[triggersBase + 0] = 3;  // upstairs Y
            rom[triggersBase + 1] = 2;  // upstairs X
            rom[triggersBase + 2] = 4;  // downstairs Y
            rom[triggersBase + 3] = 2;  // downstairs X
            rom[triggersBase + 4] = 6;  // clue Y
            rom[triggersBase + 5] = 5;  // clue X
            rom[triggersBase + 6] = 8;  // hole Y
            rom[triggersBase + 7] = 7;  // hole X
            for (int i = 8; i < 112; i++) {
                rom[triggersBase + i] = (byte) 0xFF;
            }

            // Threshold byte (memory map: $A3E6..$A3EA).
            rom[0xA3E6 + level] = (byte) ((level + 1) * 10);
        }

        MazeLevel[] levels = MazeLevelParser.parseLevels(rom);

        assertEquals(5, levels.length);
        for (int level = 0; level < 5; level++) {
            MazeLevel lv = levels[level];
            assertEquals(level, lv.getLevelNumber());
            assertEquals((level + 1) * 10, lv.getWanderingThreshold(),
                    "threshold should come from $A3E6 + level");

            List<Trigger> triggers = lv.getTriggers();
            // First four triggers carry the special types in the order documented
            // in memory-map.txt.
            assertSame(Trigger.TriggerType.UP_STAIRS, triggers.get(0).getType());
            assertSame(Trigger.TriggerType.DOWN_STAIRS, triggers.get(1).getType());
            assertSame(Trigger.TriggerType.CLUE, triggers.get(2).getType());
            assertSame(Trigger.TriggerType.HOLE, triggers.get(3).getType());
            // Past index 3 everything is "NORMAL" (monsters or disabled).
            for (int i = 4; i < triggers.size(); i++) {
                assertSame(Trigger.TriggerType.NORMAL, triggers.get(i).getType(),
                        "trigger " + i + " at level " + level);
            }
        }
    }
}
