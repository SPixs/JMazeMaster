package org.pixs.mazemaster;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * The three-slot adventuring group. Encapsulates the {@code Character[3]}
 * that {@link Game} used to expose raw, plus the
 * "valid slots are contiguous from index 0" invariant that
 * {@link Game#deleteCharacter(int)} preserves (dead characters are shifted
 * down so an empty slot never appears between living ones).
 *
 * <p>Iterating over {@code Party} yields only the currently-valid characters;
 * this matches the ASM pattern of checking {@code $800/$900/$A00} first-byte
 * against {@code $24} (space) to detect empty slots.</p>
 */
public final class Party implements Iterable<Character> {

    public static final int MAX_SIZE = 3;

    private final Character[] slots;

    public Party() {
        slots = new Character[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            slots[i] = new Character();
        }
    }

    /** Character at the given slot (0..2), valid or not. */
    public Character at(int slot) {
        return slots[slot];
    }

    /** {@code true} when slot 0 is empty, meaning no adventurer remains. */
    public boolean allDead() {
        return !slots[0].isValid();
    }

    /** Number of filled slots. */
    public int aliveCount() {
        int count = 0;
        for (Character c : slots) if (c.isValid()) count++;
        return count;
    }

    /** Sum of a per-character integer attribute across living slots. */
    public int sumOverAlive(ToIntFunction<Character> f) {
        int total = 0;
        for (Character c : slots) if (c.isValid()) total += f.applyAsInt(c);
        return total;
    }

    /** Apply a side-effecting action to each living character. */
    public void forEachAlive(Consumer<Character> body) {
        for (Character c : slots) if (c.isValid()) body.accept(c);
    }

    /**
     * Remove the character at {@code index} and shift the following slots
     * down by one; the third slot becomes a fresh empty character.
     * Matches {@link Game#deleteCharacter(int)} (which this method mirrors).
     */
    public void deleteAt(int index) {
        for (int i = index; i < MAX_SIZE - 1; i++) {
            slots[i] = slots[i + 1];
        }
        slots[MAX_SIZE - 1] = new Character();
    }

    @Override
    public Iterator<Character> iterator() {
        return new Iterator<>() {
            private int i = 0;
            private int nextAlive = findNext(0);

            @Override
            public boolean hasNext() { return nextAlive < MAX_SIZE; }

            @Override
            public Character next() {
                if (nextAlive >= MAX_SIZE) throw new NoSuchElementException();
                Character c = slots[nextAlive];
                i = nextAlive + 1;
                nextAlive = findNext(i);
                return c;
            }

            private int findNext(int from) {
                for (int j = from; j < MAX_SIZE; j++) {
                    if (slots[j].isValid()) return j;
                }
                return MAX_SIZE;
            }
        };
    }
}
