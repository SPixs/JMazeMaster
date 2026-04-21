package org.pixs.mazemaster.states;

import java.util.Arrays;

import org.pixs.mazemaster.Game;
import org.pixs.mazemaster.MazeMap;
import org.pixs.mazemaster.Party;
import org.pixs.mazemaster.PetsciiConsole;
import org.pixs.mazemaster.Rom;
import org.pixs.mazemaster.RomText;

/**
 * Base class for the three-state FSM (Init, MainMenu, Maze). Kept deliberately
 * thin: it owns the state transition primitives and exposes narrow accessors
 * for the shared services the states need ({@link PetsciiConsole},
 * {@link Rom}, {@link Party}, {@link MazeMap}, the raw memory and the keyboard
 * buffer). Text output lives on the Console; ROM knowledge on Rom.
 *
 * <p>The protected fields {@code m_charOutputRow} / {@code m_charOutputCol} are
 * aliased views on {@link PetsciiConsole#row}/{@code col}. Subclasses can keep
 * using the old names; under the hood the cursor state is owned by the single
 * shared console held on {@link Game}.</p>
 */
public abstract class GameState {

	private final Game m_game;

	public GameState(Game game) {
		m_game = game;
	}

	// ================================= Access methods ================================

	public void setState(GameState state) {
		m_game.setState(state);
	}

	public Game getGame() {
		return m_game;
	}

	/** Shared text-output cursor over the game bitmap. */
	public PetsciiConsole console() { return m_game.getConsole(); }

	/** Shortcut to the ROM façade for domain-level queries. */
	public Rom rom() { return m_game.getRom(); }

	/** Shortcut to the party of characters. */
	public Party party() { return m_game.getParty(); }

	// ================================= FSM methods =================================

	public abstract void enter();
	public abstract void exit();

	// ========================== Cursor state aliases ================================
	// Subclasses used to read/write m_charOutputRow / m_charOutputCol directly.
	// These remain accessible, now as aliases on the shared console.

	protected int getCharOutputRow() { return console().row; }
	protected int getCharOutputCol() { return console().col; }
	protected void setCharOutputRow(int r) { console().row = r; }
	protected void setCharOutputCol(int c) { console().col = c; }

	// ================================= Raw memory access ================================

	public byte getMem(int adress) {
		return m_game.getMem(adress);
	}

	/**
	 * Read a byte from the C64 memory image as an unsigned value (0..255).
	 * Prefer this over {@link #getMem(int)} when the byte represents a count,
	 * offset or coordinate — the raw byte is sign-extended to int otherwise,
	 * which silently corrupts values with bit 7 set.
	 */
	public int getMemU(int address) {
		return m_game.getMem(address) & 0xFF;
	}

	// ================================= Output helpers ================================
	// Every one of these forwards to the console. Keeping them on GameState lets
	// the existing states stay idiomatic (`outputChar`, `displayString`, …)
	// without breaking composition — the state *has* a console, not *is* one.

	protected void outputChar(byte code) { console().putChar(code); }
	public void display(byte[] str) { console().putChars(str); }
	public void displayString(int address, int charCount) { console().putRomString(address, charCount); }
	public int displayStringAt(int address) { return console().putRomStringTerminated(address); }
	protected void outputWord(int value) { console().putDecimal(value); }
	/** Write a named fixed-length message from the ROM catalog. */
	protected void displayText(RomText t) { console().putText(t); }

	protected void putCursorAt7x7() {
		console().moveTo(7, 7);
	}

	// =============================== Menu screen clears =============================

	/** Clear whole menu screen (reset bitmap and colors). */
	protected void clearMenuScreen() {
		// RAZ de la mémoire bitmap de ($2000 -> $3FFF sur C64)
		org.pixs.hardware.VicIIDisplay vicII = m_game.getVicII();
		for (int i = 0; i < 8192; i++) {
			vicII.setBitmapValue(i, (byte) 0);
		}
		// En mode bitmap, la Screen RAM sert à définir les couleurs.
		// ASM b94B5 fills $0400-$0800 (1024 bytes) with $1B (blanc sur gris foncé).
		for (int i = 0; i < 1024; i++) {
			vicII.setCharAt(i, (byte) 0x1B);
		}
	}

	/**
	 * $9DEF — CLEAR SCREEN de la partie centrale du menu principal
	 * (bitmap $2740 → $362F, 12 lignes).
	 */
	protected void clearMenu() {
		org.pixs.hardware.VicIIDisplay vicII = m_game.getVicII();
		for (int i = 0x740; i < 0x1630; i++) {
			vicII.setBitmapValue(i, (byte) 0);
		}
	}

	// =============================== Menu layout helpers ============================

	/** $9C99 — header for the three main-menu screens. */
	protected void displayMenuHeader() {
		console().moveTo(9, 0);
		outputChar((byte) 0x2C);
		for (int i = 0; i < 0x14; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2D); console().newLineAtCol9();
		outputChar((byte) 0x2B);
		console().col = 14;
		for (int i = 2; i < 14; i++) outputChar(getMem(0xA5B0 + i));
		console().col = 0x1E;
		outputChar((byte) 0x2B); console().newLineAtCol9();
		outputChar((byte) 0x2B);
		console().col = 14;
		for (int i = 2; i < 14; i++) outputChar(getMem(0xA5C0 + i));
		console().col = 0x1E;
		outputChar((byte) 0x2B); console().newLineAtCol9();
		// "|BY MICHAEL CRANFORD |"
		for (int i = 0; i < 0x16; i++) outputChar(getMem(0xA5D0 + i));
		console().newLineAtCol9();
		outputChar((byte) 0x2E);
		for (int i = 0; i < 0x14; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2F);
		console().newLineAtCol9();
		// "COPYRIGHT (C) 1983 HES"
		for (int i = 0; i < 0x16; i++) outputChar(getMem(0xA9C3 + i));
	}

	/** $9D2C — footer listing the party character names. */
	protected void displayMenuFooter() {
		console().moveTo(9, 18);
		outputChar((byte) 0x2C);
		for (int i = 0; i < 0x15; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2D); console().newLineAtCol9();
		outputChar((byte) 0x2B);
		console().col = 0x0E;
		for (int i = 0; i < 0x0E; i++) outputChar(getMem(0xA5E6 + i)); // "CHARACTER NAME"
		console().col = 0x1F;
		outputChar((byte) 0x2B); console().newLineAtCol9();

		for (int slot = 0; slot < 3; slot++) {
			for (int i = 4 * slot; i < 4 * slot + 4; i++) outputChar(getMem(0xA5F4 + i));
			console().col++;
			byte[] characterName = m_game.getCharacter(slot).getNameAsBytes();
			for (int i = 0; i < 0x10; i++) outputChar(characterName[i]);
			console().col++;
			outputChar((byte) 0x2B);
			console().newLineAtCol9();
		}

		outputChar((byte) 0x2E);
		for (int i = 0; i < 0x15; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2F);
	}

	protected void displayMenuChoices() {
		console().moveTo(5, 7);
		outputChar((byte) 0x2C);
		for (int i = 0; i < 0x1C; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2D);
		console().newLineAtCol5();
		for (int i = 0; i < 0x11; i++) outputChar(getMem(0xA600 + i));
		console().col = 0x22;
		outputChar((byte) 0x2B);
		console().row++;
		console().col--;
		outputChar((byte) 0x2B);
		console().col = 5;
		outputChar((byte) 0x2B);
		console().newLineAtCol5();
		int count = 0;
		do {
			for (int i = 0; i < 0x1E; i++) {
				outputChar(getMem(0xA611 + count));
				count++;
			}
			console().newLineAtCol5();
		} while (count < 0x96);
		outputChar((byte) 0x2E);
		for (int i = 0; i < 0x1C; i++) outputChar((byte) 0x29);
		outputChar((byte) 0x2F);
	}

	// =============================== Keyboard input helpers ============================

	public int readKeyboardAsPETSCII() {
		byte in = m_game.getIN();
		while (in == 0) {
			try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
			in = m_game.getIN();
		}
		return in & 0x0FF;
	}

	public int readKeyboardAsPETSCIINoBlocking() {
		return m_game.getIN() & 0x0FF;
	}

	protected void deleteCharacter(int index) {
		m_game.deleteCharacter(index);
	}

	/**
	 * $A003 — read up to {@code count} characters with a blinking cursor,
	 * Enter validates (non-empty), Delete backs up, Space and hex digits / A-Z
	 * are accepted. Returns the screen-code buffer (padded with $24 spaces).
	 */
	public byte[] readChars(int count) {
		byte[] result = new byte[count];
		Arrays.fill(result, (byte) 0x24);
		int counter = 0;
		int counter2 = 0;
		int charIndex = 0;

		boolean nameEntered = false;
		while (!nameEntered) {
			while (true) {
				int keyPETSCII;
				counter++;
				if ((counter & 0xFF) == 0) {
					counter = 0;
					counter2++;
					if (counter2 == 4) counter2 = 0;
				}
				if (counter2 < 2) {
					outputChar((byte) 0x48);
					console().col--;
				} else {
					outputChar((byte) 0x24);
					console().col--;
				}
				delayInMillis(0.4);
				keyPETSCII = readKeyboardAsPETSCIINoBlocking();
				if (keyPETSCII == 0) continue;

				if (keyPETSCII == 0x0D) break; // Enter
				if (keyPETSCII == 0x20) { // Space
					outputChar((byte) 0x24);
					result[charIndex++] = (byte) 0x24;
					if (charIndex == count) break;
					continue;
				}
				if (keyPETSCII == 0x14) { // Delete
					if (charIndex > 0) {
						result[--charIndex] = (byte) 0x24;
						outputChar((byte) 0x24);
						console().col -= 2;
					}
					continue;
				}
				int keyChar = keyPETSCII - 0x30;
				if (keyChar < 0x0A) {
					outputChar((byte) keyChar);
					result[charIndex++] = (byte) keyChar;
					if (charIndex == count) break;
				} else {
					keyChar -= 0x07;
					if (keyChar >= 0x0A && keyChar <= 0x24) {
						outputChar((byte) keyChar);
						result[charIndex++] = (byte) keyChar;
						if (charIndex == count) break;
					}
				}
			}
			nameEntered = charIndex > 0;
		}

		outputChar((byte) 0x24);
		return result;
	}

	public void delayInMillis(double durationInMillis) {
		long nanos = (long) (durationInMillis * 1_000_000);
		java.util.concurrent.locks.LockSupport.parkNanos(nanos);
	}
}
