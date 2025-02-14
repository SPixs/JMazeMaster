package org.pixs.mazemaster.states;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pixs.C64KeyMapping;
import org.pixs.hardware.CIA1;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.Game;

public abstract class GameState {

	private Game m_game;
	
	// Originally stored at $4A
	protected int m_charOutputRow;
	
	// Originally stored at $49
	protected int m_charOutputCol;

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
	
	// =================================== FSM methods =================================
	
	public abstract void enter();
	public abstract void exit();
	
	// ================================= Helper methods ================================
	
	public byte getMem(int adress) {
		return getGame().getMem(adress);
	}
	
	// =============================== Display control methods =========================
	
	/**
	 * Clear whole menu screen (reset bitmap and colors)
	 */
	protected void clearMenuScreen() {
		// RAZ de la mémoire bitmap de ($2000 -> $3FFF sur C64)
		VicIIDisplay vicII = getGame().getVicII();
		for (int i=0;i<8192;i++) {
			vicII.setBitmapValue(i, (byte)0);
		}
		
		// En mode bitmap, la Screen RAM sert à définir les couleurs.
		// The Background Pixel Color is defined by Bits#0 - Bit#3 of the corresponding Byte in Screen RAM.  
		// The Foreground Pixel Color is defined by Bits#4 - Bits#7 - again from the corresponding Byte in Screen RAM. 
		// Remplissage de la mémoire $0400 -> $0800 avec $1B (blanc sur fond gris foncé)
		for (int i=0;i<8192;i++) {
			vicII.setCharAt(i, (byte)(0x1B));
		}
	}
	
	/**
	 * $9C99 in original game Cartidge
	 */
	protected void displayMenuHeader() {
		// Display round corner char at (9,0)
		m_charOutputRow = 0;
		m_charOutputCol = 9;
		outputChar((byte)0x2C);
		
		// Display '-' char at (10,0)->(29,0)
		for (int i=0;i<0x14;i++) {
			outputChar((byte)0x29);
		}
		
		// Display round corner char at (30,0)
		outputCharAndReturnCol9((byte)0x2D);
		
		// Display '|' at (9,1)
		outputChar((byte)0x2B);
		
		// Display 12 chars at (14,1), stored at $A5B2 : " MAZE " (each screen char use 2 RAM char)
		m_charOutputCol = 14;
		for (int i=2;i<14;i++) {
			outputChar(getMem(0xA5B0+i));
		}
		
		// Display '|' at (30,1)
		m_charOutputCol = 0x1E;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Display '|' at (10,2)
		outputChar((byte)0x2B);
		
		// Display 12 chars at (14,2), stored at $A5C2 : " MASTER " (each screen char use 2 RAM char)
		m_charOutputCol = 14;
		for (int i=2;i<14;i++) {
			outputChar(getMem(0xA5C0+i));
		}
		
		// Display '|' at (30,2)
		m_charOutputCol = 0x1E;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Ouput screen codes 2B,0B,22,24,16,12,0C,11,0A,0E,15,24,0C,1B,0A,17,0F,18,1B,0D,24,2B at (9,3)
		// that matches chars "|BY MICHAEL CRANFORD |"
		for (int i=0;i<0x16;i++) {
			outputChar(getMem(0xA5D0+i));
		}
		returnNextRowCol9();
		
		// Display round corner char at (9,4)
		outputChar((byte)0x2E);
		
		// Display '-' char at (10,4)->(29,4)
		for (int i=0;i<0x14;i++) {
			outputChar((byte)0x29);
		}
		
		// Display round corner char at (30,4)
		outputChar((byte)0x2F);
		returnNextRowCol9();
		
		// Ouput screen codes 0C,18,19,22,1B,12,10,11,1D,24,4A,0C,28,24,01,09,08,03,24,11,0E,1C at (9,5)
		// that matches chars "COPYRIGHT (C) 1983 HES"
		for (int i=0;i<0x16;i++) {
			outputChar(getMem(0xA9C3+i));
		}
	}

	/**
	 * $9D2C in original game Cartidge
	 */
	protected void displayMenuFooter() {
		// Display round corner char at (9,18)
		m_charOutputRow = 18;
		m_charOutputCol = 9;
		outputChar((byte)0x2C);
		
		// Display '-' char at (10,18)->(30,18)
		for (int i=0;i<0x15;i++) {
			outputChar((byte)0x29);
		}
		
		// Display round corner char at (31,18)
		outputCharAndReturnCol9((byte)0x2D);
		
		// Display '|' at (9,19)
		outputChar((byte)0x2B);
		
		// Ouput screen codes 0C,11,0A,1B,0A,0C,1D,0E,1B,24,17,0A,16,0E at (14,19)
		// that matches chars "CHARACTER NAME"
		m_charOutputCol = 0x0E;
		for (int i=0;i<0x0E;i++) {
			outputChar(getMem(0xA5E6+i));
		}
		
		// Display '|' at (31,19)
		m_charOutputCol = 0x1F;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Ouput screen codes 2B,24,01,28 at (9,20)
		// that matches chars "| 1)"
		for (int i=0;i<4;i++) {
			outputChar(getMem(0xA5F4+i));
		}
		
		// Display 15 chars at (14,20), stored at $0800 : character 1 name
		m_charOutputCol++;
		byte[] characterName = getGame().getCharacter(0).getNameAsBytes();
		for (int i=0;i<0x10;i++) {
			outputChar(characterName[i]);
		}
		
		// Display '|' at (31,20)
		m_charOutputCol++;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Ouput screen codes 2B,24,02,28 at (9,21)
		// that matches chars "| 2)"
		for (int i=4;i<8;i++) {
			outputChar(getMem(0xA5F4+i));
		}
		
		// Display 15 chars at (14,21), stored at $0900 : character 2 name
		m_charOutputCol++;
		characterName = getGame().getCharacter(1).getNameAsBytes();
		for (int i=0;i<0x10;i++) {
			outputChar(characterName[i]);
		}
		
		// Display '|' at (31,21)
		m_charOutputCol++;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Ouput screen codes 2B,24,03,28 at (9,22)
		// that matches chars "| 3)"	
		for (int i=8;i<0x0C;i++) {
			outputChar(getMem(0xA5F4+i));
		}
		
		// Display 15 chars at (14,22), stored at $0A00 : character 3 name
		m_charOutputCol++;
		characterName = getGame().getCharacter(2).getNameAsBytes();
		for (int i=0;i<0x10;i++) {
			outputChar(characterName[i]);
		}
		
		// Display '|' at (31,22)
		m_charOutputCol++;
		outputCharAndReturnCol9((byte)0x2B);
		
		// Display round corner char at (9,23)
		outputChar((byte)0x2E);
		
		// Display '-' char at (10,23)->(30,23)
		for (int i=0;i<0x15;i++) {
			outputChar((byte)0x29);
		}
		
		// Display round corner char at (31,23)
		outputChar((byte)0x2F);
	}

	
	/**
	 * $9D2C in original game Cartidge
	 *
	 * CLEAR SCREEN de la partie centrale du menu principal*
	 * RAZ de la memoire de $2740 -> $362F
	 * soit 3824 octets, soit 478 (3824/8) blocks de 8x8 pixels bitmap, 
	 * soit 11 lignes écran (478/40)
	 * 
	 * Note : le code original efface de $2740 -> $362F, soit de 80% de la ligne 4 (depuis 0) jusque la fin de la ligne 16 (soit 10 lignes + fin ligne 4 et debut ligne 11 -> bug non visible)
	 */
	protected void clearMenu() {
		// on commence la RAZ à l'adresse $2740 ($2000 est l'adresse de debut C64)
		// soit la 5e ligne de l'écran bitmap
		VicIIDisplay vicII = getGame().getVicII();
		for (int i=0x740;i<0x1630;i++) {
			vicII.setBitmapValue(i, (byte)0);
		}
	}
	
	protected void displayMenuChoices() {
		m_charOutputRow = 0x07;
		m_charOutputCol = 0x05;
	
		outputChar((byte)0x2C);
		
		// Display '-' chars
		for (int i=0;i<0x1c;i++) {
			outputChar((byte)0x29);
		}
		
		outputChar((byte)0x2D);
		returnNextRowCol5();
		
		for (int i=0;i<0x11;i++) {
			outputChar(getMem(0xA600+i));
		}
		m_charOutputCol = 0x22;
		outputChar((byte)0x2B);
		
		m_charOutputRow++;
		m_charOutputCol--;
		outputChar((byte)0x2B);
		
		m_charOutputCol = 0x05;
		outputChar((byte)0x2B);
		
		returnNextRowCol5();
		int count = 0;
		do {
			for (int i=0;i<0x1E;i++) {
				outputChar(getMem(0xA611+count));
				count++;
			}
			returnNextRowCol5();
		}
		while (count < 0x96);
		outputChar((byte)0x2E);
		for (int i=0;i<0x1C;i++) {
			outputChar((byte)0x29);
		}
		outputChar((byte)0x2F);
	}
	
	/**
	 * $9E94
	 * Output a char at current (m_charOutputCol, m_charOutputRow) and increment m_charOutputCol
	 * @param code (originally stored in Acc)
	 */
	protected void outputChar(byte code) {
		VicIIDisplay vicII = getGame().getVicII();
		int startAdress = (m_charOutputRow*40*8)+m_charOutputCol*8;
		for (int i=0;i<8;i++) {
			vicII.setBitmapValue(startAdress+i, getGame().getCharset()[code*8+i]);
		}
		m_charOutputCol++;
	}

	public void displayString(int address, int charCount) {
		for (int i=0;i<charCount;i++) {
			outputChar(getMem(address+i));
		}
	}

	/**
	 * A1ED
	 * @param value
	 */
	protected void outputWord(int value) {
		value = value & 0x0FFFF;
		for (char c : String.valueOf(value).toCharArray()) {
			outputChar((byte) (c - '0'));
		}
		
	}
	
	/**
	 * $A388
	 * Output a char at current (m_charOutputCol, m_charOutputRow) and put cursor on next line, column 9
	 * @param b
	 */
	private void outputCharAndReturnCol9(byte code) {
		outputChar(code);
		returnNextRowCol9();
	}
	
	public int displayStringAt(int address) {
		int dispayedCount = 0;
		byte c = getMem(address);
		while (c != (byte)(0xFF)) {
			outputChar(c);
			dispayedCount++;
			c = getMem(address+dispayedCount);
		}
		return dispayedCount+1;
	}
	
	public void display(byte[] str) {
		for (byte b : str) {
			outputChar(b);
		}
	}
	
	/**
	 * sA392
	 * Put cursor on next line, column 5
	 */
	private void returnNextRowCol5() {
		m_charOutputRow++;
		m_charOutputCol=5;
	}
	
	/**
	 * sA38B
	 * Put cursor on next line, column 9
	 */
	private void returnNextRowCol9() {
		m_charOutputRow++;
		m_charOutputCol=9;
	}
	
	protected void putCursorAt7x7() {
		m_charOutputRow=7;
		m_charOutputCol=7;
	}
	
	public int readKeyboardAsPETSCII() {
		byte in = getGame().getIN();
		while (in == 0) {
			Thread.yield();
			in = getGame().getIN();
		}
		return in & 0x0FF;
	}
	
	public int readKeyboardAsPETSCIINoBlocking() {
		return getGame().getIN() & 0x0FF;
	}
	
	protected void deleteCharacter(int index) {
		getGame().deleteCharacter(index);
	}

	/**
	 * A003
	 * Lecture du clavier (max 10 chars) avec validation par 'enter' ou remplissage complet du buffer.
	 * @return
	 */
	public byte[] readChars(int count) {
		// Fill input buffer with space char ' '
		byte[] result = new byte[count];
		Arrays.fill(result, (byte)0x24);
		
		int counter = 0;
		int counter2 = 0;
		int charIndex = 0;
		
		boolean nameEntered = false;
		while (!nameEntered) {
			while (true) {
				int keyPETSCII = readKeyboardAsPETSCIINoBlocking();
				counter++;
				if ((counter & 0xFF) == 0) {
					counter = 0;
					counter2++;
					if (counter2 == 4) {
						counter2 = 0;
					}
				}
				
				// if cursor counter < 2, display blank cursor
				// else display plain
				if (counter2 < 2) {
					outputChar((byte)0x48);
					m_charOutputCol--;
				}
				else {
					outputChar((byte)0x24);
					m_charOutputCol--;
				}
				delayInMillis(0.4);
				keyPETSCII = readKeyboardAsPETSCIINoBlocking();
				if (keyPETSCII != 0) {
					if (keyPETSCII == 0x0D) { // 'Enter' key pressed ?
						break;
					}
					else if (keyPETSCII == 0x20) {
						outputChar((byte)0x24);
						result[charIndex++] = (byte)0x24; // 'Space' key pressed ?
						if (charIndex == count) {
							break;
						}
					}
					else if (keyPETSCII == 0x14) { // 'Delete' key pressed ?
						if (charIndex > 0) {
							result[--charIndex] = (byte)0x24;
							outputChar((byte)0x24);
							m_charOutputCol--;
							m_charOutputCol--;
						}
					}
					else {
						int keyChar = keyPETSCII - 0x30;
						if (keyChar < 0x0A) {
							outputChar((byte)keyChar);
							result[charIndex++] = (byte)keyChar; 
							if (charIndex == count) {
								break;
							}							
						}
						else {
							keyChar -= 0x07;
							if (keyChar >= 0x0A && keyChar <= 0x24) {
								outputChar((byte)keyChar);
								result[charIndex++] = (byte)keyChar; 
								if (charIndex == count) {
									break;
								}	
							}
						}
					}
				}
			}
			nameEntered = charIndex > 0;
		}
		
		outputChar((byte)0x24);
		return result;
	}
	
	public void delayInMillis(double durationInMillis) {
		long nanoTime = System.nanoTime();
		while (System.nanoTime() - nanoTime < durationInMillis * 1000000) { 
			Thread.yield(); 
		}
	}
}
