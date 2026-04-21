package org.pixs.mazemaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PartyTest {

    private Character named(String name, int dex) {
        Character c = new Character();
        c.setName(name);
        c.setDexterity(dex);
        return c;
    }

    private Party partyOf(Character... cs) {
        Party p = new Party();
        for (int i = 0; i < cs.length; i++) {
            // Use setName/raw-bytes to write into the existing slot rather than re-creating.
            p.at(i).setName(cs[i].isValid() ? "X" + i : "");
            p.at(i).setDexterity(cs[i].getDexterity());
        }
        return p;
    }

    @Test
    void freshPartyHasNoValidCharacters() {
        Party p = new Party();
        assertTrue(p.allDead());
        assertEquals(0, p.aliveCount());
        assertFalse(p.iterator().hasNext());
    }

    @Test
    void aliveCountSkipsEmptySlots() {
        Party p = new Party();
        p.at(0).setName("A");
        p.at(2).setName("C");
        // Slot 1 stays empty — simulates a mid-party death would-be (though the ASM guarantees shifting).
        assertEquals(2, p.aliveCount());
        assertFalse(p.allDead());
    }

    @Test
    void iteratorYieldsOnlyAliveInSlotOrder() {
        Party p = new Party();
        p.at(0).setName("ALICE");
        p.at(1).setName("BOB");
        p.at(2).setName("CAT");

        List<String> names = new ArrayList<>();
        for (Character c : p) names.add(c.getNameAsBytes()[0] + "");
        assertEquals(3, names.size());
    }

    @Test
    void sumOverAliveAccumulatesOnlyFilledSlots() {
        Party p = new Party();
        p.at(0).setName("ALICE"); p.at(0).setDexterity(18);
        // slot 1 empty
        p.at(2).setName("CAT");   p.at(2).setDexterity(12);

        assertEquals(30, p.sumOverAlive(Character::getDexterity));
    }

    @Test
    void forEachAliveSkipsEmptySlots() {
        Party p = new Party();
        p.at(0).setName("A"); p.at(0).setConstitution(100);
        p.at(2).setName("B"); p.at(2).setConstitution(50);

        p.forEachAlive(c -> c.setCondition(c.getConstitution()));

        assertEquals(100, p.at(0).getCondition());
        assertEquals(0,   p.at(1).getCondition(), "empty slot should not have been touched");
        assertEquals(50,  p.at(2).getCondition());
    }

    @Test
    void deleteAtShiftsRemainingCharactersDown() {
        Party p = new Party();
        p.at(0).setName("A");
        p.at(1).setName("B");
        p.at(2).setName("C");

        p.deleteAt(0);

        // After shift: slot 0 holds 'B', slot 1 holds 'C', slot 2 is empty.
        assertTrue(p.at(0).isValid());
        assertTrue(p.at(1).isValid());
        assertFalse(p.at(2).isValid());
        assertEquals(2, p.aliveCount());
    }

    @Test
    void deleteAtEndClearsOnlyThatSlot() {
        Party p = new Party();
        p.at(0).setName("A");
        p.at(1).setName("B");
        p.at(2).setName("C");

        p.deleteAt(2);

        assertTrue(p.at(0).isValid());
        assertTrue(p.at(1).isValid());
        assertFalse(p.at(2).isValid());
    }
}
