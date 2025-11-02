package org.pixs.mazemaster.states;

import java.util.Random;

import org.pixs.hardware.Sprite;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.Character;
import org.pixs.mazemaster.Game;

public class MainMenuState extends GameState {

	public MainMenuState(Game game) {
		super(game);
	}

	@Override
	public void enter() {
		
		VicIIDisplay vicII = getGame().getVicII();
		
		// Clear whole menu screen
		clearMenuScreen();
		
		// Border Color
		vicII.setBorderColor(0x0F);
		
//		Sprite sprite = new Sprite();
//		sprite.color = 0;
//		sprite.enabled = true;
//		sprite.x = 24-8;
//		sprite.y = 50-8;
//		sprite.doubleWidth = true;
//		sprite.doubleHeight = true;
//		sprite.spriteData = new byte[64];
//		for (int i=0;i<64;i++) {
//			sprite.spriteData[i] = (byte) (new Random().nextInt(256) & 0x0FF);
//		}
//		vicII.setSprite(0, sprite);
		
		// Display menu HEADER
        // JSR s9C99	 
		displayMenuHeader();
		
		// Display menu FOOTER
        // JSR s9D2C	 
		displayMenuFooter();

		// Reading pressed key in main menu	
		boolean exitMenu = false;
		while (!exitMenu) {
		
			// Display main menu with 5 choices
			//   JSR s9DEF    // clear menu
			//   JSR s9E12    // display 5 choices (center part)
			clearMenu();
			displayMenuChoices(); 
			
			int selectedOption = -1;
			while (selectedOption < 0) {
				int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
				if (readKeyboardAsPETSCII-0x31 < 6) {
					selectedOption = readKeyboardAsPETSCII-0x31;
				}
			}
	
			switch (selectedOption) {
				case 0: // Touche '1' ?
					exitMenu = newCharacter();
					break;
				case 4: // Touche '5' ?
					exitMenu = enterTheMaze();
					break;
				case 2: // Touche '3' ?
					examineCharacter();
					break;
				case 3: // Touche '4' ?
					deleteCharacter();
					break;
				default: // Touche '2' ?
					buyItem();
					break;
			}
		}
	}
	
	private void buyItem() {
		clearMenu();
		
		// Ouput screen codes 20,11,18,24,20,12,15,15,24,0B,1E,22,27,24,01,49,03,24,4A,00,2A,17,18,17,0E,28 at (7,7) 
		// that matches chars "WHO WILL BUY? 1-3 (0:NONE)"
		m_charOutputRow = 0x07;
		m_charOutputCol = 0x07;
		for (int i=0;i<0x1A;i++) {
			outputChar(getMem(0xA77C+i));
		}
		
		Character selectedCharacter = null;
		while (selectedCharacter == null) {
		
			int pressedNumber = readKeyboardAsPETSCII() - 0x30;
			while (pressedNumber > 3) {
				pressedNumber = readKeyboardAsPETSCII() - 0x30;
			}
			
			if (pressedNumber == 0) {
				return;
			}
			
			Character character = getGame().getCharacter(pressedNumber-1);
			if (character.isValid()) {
				selectedCharacter = character;
			}
		}
		
		// Ouput screen codes 1D,22,19,0E,24,4A,01,49,20,0E,24,02,49,0A,1B,24,03,49,1C,11,24,04,49,16,12,28,27 at (7,9) 
		// that matches chars "TYPE (1-WE 2-AR 3-SH 4-MI)?"	
		m_charOutputRow+=2;
		m_charOutputCol=7;
		for (int i=0;i<0x1B;i++) {
			outputChar(getMem(0xA796+i));
		}
		
		int pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		while (pressedNumber > 3) {
			pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		}
		int itemType = pressedNumber;
		
		// Ouput screen codes 12,1D,0E,16,24,17,1E,16,0B,0E,1B,27,24,4A,01,49,04 (7,10) 
		// that matches chars "ITEM NUMBER?: (1-4)"	
		m_charOutputRow++;
		m_charOutputCol=7;
		for (int i=0;i<0x12;i++) {
			outputChar(getMem(0xA7B1+i));
		}
		
		pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		while (pressedNumber > 3) {
			pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		}
		byte itemNumber = (byte) pressedNumber;
		int itemDataOffset = (4 * itemType + pressedNumber) * 2;
		int itemPrice = ((getMem(0xBFC1+itemDataOffset) << 8) & 0xFF00) | (getMem(0xBFC0+itemDataOffset) & 0x0FF);

		if (itemPrice > selectedCharacter.getGold()) {
			// Ouput screen codes 12,17,1C,1E,0F,0F,12,0C,12,0E,17,1D,24,0F,1E,17,0D,1C at (11,11) 
			// that matches chars "INSUFFICIENT FUNDS"
			m_charOutputRow++;
			m_charOutputCol=11;
			for (int i=0;i<0x12;i++) {
				outputChar(getMem(0xA7C3+i));
			}
			
			m_charOutputRow+=2;
			m_charOutputCol=0x0A;
			// Ouput screen codes 11,12,1D,24,0A,17,22,24,14,0E,22,24,1D,18,24,10,18,24,18,17,1C at (5,15) 
			// that matches chars "HIT ANY KEY TO GO ON"
			for (int i=0x0A;i<0x1E;i++) {
				outputChar(getMem(0xA753+i));
			}
			
			readKeyboardAsPETSCII();
			return;
		}
		else {
			// Remove item price from the character gold
			selectedCharacter.setGold(selectedCharacter.getGold() - itemPrice);
			selectedCharacter.setItem(itemType, (byte)(itemNumber+1));
		}
	}

	private void deleteCharacter() {
		clearMenu();
		
		// Ouput screen codes 0D,0E,15,0E,1D,0E,24,0C,11,0A,1B,0A,0C,1A,0E,1B,24,01,49,03,24,4A,00,2A,17,18,17,0E,28 at (9,8)
		// that matches chars "DELETE CHARACTER 1-3 (0:NONE)"
		m_charOutputRow = 0x08;
		m_charOutputCol = 0x06;
		for (int i=0;i<0x1D;i++) {
			outputChar(getMem(0xB7E2+i));
		}
		
		// Let player choose the caracter number to delete
		int pressedNumber = readKeyboardAsPETSCII() - 0x30;
		while (pressedNumber > 3) {
			pressedNumber = readKeyboardAsPETSCII() - 0x30;
		}
		
		if (pressedNumber == 0) {
			return;
		}

		deleteCharacter(pressedNumber-1);
		displayMenuFooter();
	}

	private void examineCharacter() {
		clearMenu();
		m_charOutputCol = 0x07;
		m_charOutputRow = 0x08;
		
		for (int i=0;i<0x1A;i++) {
			outputChar(getMem(0xA739+i));
		}
		
		Character selectedCharacter = null;
		while (selectedCharacter == null) {
		
			int pressedNumber = readKeyboardAsPETSCII() - 0x30;
			while (pressedNumber > 3) {
				pressedNumber = readKeyboardAsPETSCII() - 0x30;
			}
			
			if (pressedNumber == 0) {
				return;
			}
			
			Character character = getGame().getCharacter(pressedNumber-1);
			if (character.isValid()) {
				selectedCharacter = character;
			}
		}
		
		clearMenu();
		
		// Display character name at (5,6)
		m_charOutputRow = 0x06;
		m_charOutputCol = 0x05;
		for (int i=0;i<0x10;i++) {
			outputChar(selectedCharacter.getNameAsBytes()[i]);
		}
		
		// Ouput screen codes 1D,11,0E,24 at (22,6)
		// that matches chars "THE "
		m_charOutputCol++;
		for (int i=0;i<0x04;i++) {
			outputChar(getMem(0xA753+i));
		}
		
		// According to the class field, display either 'THE WARRIOR' or 'THE WIZARD'
		int offset = selectedCharacter.getClassType() == 1 ? 0 : 7;
		while (true) {
			outputChar(getMem(0xA72C+offset));
			offset++;
			if (offset == 0x07 || offset == 0x0D) {
				break;
			}
		}
		
		m_charOutputRow++;
		int savedCol = m_charOutputCol;
		m_charOutputCol = 5;
		// write char '-' till saved column position
		do {
			outputChar((byte)0x29);
		}
		while (m_charOutputCol < savedCol);
		
		// Ouput screen codes 1C,1D,1B,0E,17,10,1D,11,2A,24 at (5,8) 
		// that matches chars "STRENGTH: "
		m_charOutputRow++;
		int stringIndex = displayStringAtCol5(6);
		outputWord(selectedCharacter.getStrength());
		
		// Ouput screen codes 12,17,1D,0E,15,15,0E,0C,1D,2A,24 at (19,8) 
		// that matches chars "INTELLECT: "
		m_charOutputCol+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getIntellect());
		
		// Ouput screen codes 0D,0E,21,1D,0E,1B,12,1D,22,2A,24 at (5,9) 
		// that matches chars "DEXTERITY: "
		m_charOutputRow++;
		stringIndex = displayStringAtCol5(stringIndex);
		outputWord(selectedCharacter.getDexterity());
		
		// Ouput screen codes 0C,18,17,1C,1D,12,1D,1E,1D,12,18,17,2A,24 at (20,9) 
		// that matches chars "CONSTITUTION: "
		m_charOutputCol+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getConstitution());
		
		// Ouput screen codes 10,18,15,0D,2A,24,FF at (5,10) 
		// that matches chars "GOLD: "
		m_charOutputRow++;
		stringIndex = displayStringAtCol5(stringIndex);
		outputWord(selectedCharacter.getGold());
		
		// Ouput screen codes 0E,21,19,0E,1B,12,0E,17,0C,0E,2A,24 at (5,10) 
		// that matches chars "EXPERIENCE: "
		m_charOutputCol+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getXp());
		
		// Ouput screen codes 12,1D,0E,16,1C,2A at (5,10) 
		// that matches chars "ITEMS :"
		m_charOutputRow++;
		m_charOutputCol = 5;
		for (int i=4;i<0x0A;i++) {
			outputChar(getMem(0xA753+i));
		}
		
		for (int i=0;i<4;i++) {
			m_charOutputRow++;
			m_charOutputCol=5;
			byte item = selectedCharacter.getItemCode(i);
			
			// If no item defined, skip item number computation
			int itemNumber = 0;
			if (item != 0) {
				// Else, item number = item identifier + 4*item slot number
				itemNumber = i*4+item;
			}
			
			// Items name offset are stored in a table store at $A42C
			int nameOffset = getMem(0xA42C+itemNumber) & 0x0FF;
			// Display item name at address $BF00+offset, delimited with char $FF
			displayStringAt(0xBF00+nameOffset);
		}
		m_charOutputRow++;
		m_charOutputCol=5;
		
		// Ouput screen codes 0C,18,0D,0E,2A,24 at (5,15) 
		// that matches chars "CODE: "
		for (int i=0;i<0x06;i++) {
			outputChar(getMem(0xA9F4+i));
		}
		
		int groupIndex = 0;
		for (int i=0;i<21;i++) {
			byte flippingByte = getMem(0xA396+i);
			// Does this code char encore the upper or lower 4 bits of character data ?
			byte upperOrLower = getMem(0xA3AB+i);
			// If lower part, skip logical shift of value
			byte b = selectedCharacter.getRawBytes()[flippingByte];
			if (upperOrLower != 0) {
				b = (byte) ((b >> 4) & 0x0F);
				b = (byte) ((b ^ flippingByte) & 0x0F);
				outputChar(b);
			}
			else {
				b = (byte) ((b ^ flippingByte) & 0x0F);
				outputChar(b);
			}
			
			if (++groupIndex == 7) {
				groupIndex = 0;
				m_charOutputCol++;
			}
		}
		
		m_charOutputRow++;
		m_charOutputCol=0x0A;
		// Ouput screen codes 11,12,1D,24,0A,17,22,24,14,0E,22,24,1D,18,24,10,18,24,18,17,1C at (5,15) 
		// that matches chars "HIT ANY KEY TO GO ON"
		for (int i=0x0A;i<0x1E;i++) {
			outputChar(getMem(0xA753+i));
		}
		
		readKeyboardAsPETSCII();
	}

	private boolean enterTheMaze() {
		
		// First, ensure that there is a least one character in party
		Character character = getGame().getCharacter(0);
		if (!character.isValid()) {
			return false;
		}
		
		setState(new MazeState(getGame()));
		return true;
	}

	private boolean newCharacter() {
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.getNameAsBytes()[0] == 0x24) {
				newCharacter(i);
				return false;
			}
		}
		
		return false;
	}

	/**
	 * 81CD
	 * Create new character
	 * @param index
	 */
	private void newCharacter(int index) {
		clearMenu();
		putCursorAt7x7();
		
		for (int i=0;i<0x10;i++) {
			outputChar(getMem(0xA9B3+i));
		}
		
		int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		// Read keyboard while pressed key is neither 'Y' or 'N'
		while (readKeyboardAsPETSCII != 0x4E && readKeyboardAsPETSCII != 0x59) {
			readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		}
		
		newCharacter(index, readKeyboardAsPETSCII == 0x59);
		displayMenuFooter();
	}

	private void newCharacter(int index, boolean random) {
		clearMenu();
		putCursorAt7x7();
		
		// Ouput screen codes 17,0A,16,0E,2A,24 at (7,7)
		// that matches chars "NAME: "
		for (int i=0;i<0x06;i++) {
			outputChar(getMem(0xA6A7+i));
		}
		
		// Reading tampon de saisie (10 caracteres at $0B00)
		// et stockage dans le nom du personnage libre	
		byte[] name = readChars(0x10);
		
		Character character = getGame().getCharacter(index);
		character.setNameFromByte(name);
		character.resetAttributes(); 
		m_charOutputRow++;
		
		if (random) {
			int offset = 6;
			offset = displayStringNextLineCol7(offset);
			character.setStrength(generateAndDisplayRandom());
			offset = displayStringNextLineCol7(offset);
			character.setIntellect(generateAndDisplayRandom());
			offset = displayStringNextLineCol7(offset);
			character.setDexterity(generateAndDisplayRandom());
			offset = displayStringNextLineCol7(offset);
			character.setConstitution(generateAndDisplayRandom());
			
			m_charOutputRow++;
			m_charOutputCol = 7;
			for (int i=0;i<0x15;i++) {
				outputChar(getMem(0xA717+i));
			}
			
			int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
			while (readKeyboardAsPETSCII - 0x31 > 1 || readKeyboardAsPETSCII - 0x31 < 0) {
				readKeyboardAsPETSCII = readKeyboardAsPETSCII();
			}
			int classType = readKeyboardAsPETSCII - 0x31 + 1;
			character.setClassType(classType);
			outputChar((byte) classType);
			m_charOutputRow++;
			
			// Generate random gold and reset following data (experience, items, ...)
			character.setGold((new Random().nextInt(256) & 0xFF) | 0x40);
			character.setXP(0);
			character.clearIndicators();
		}
		else {
			int charIndexToParse = 0;
			int stringOffset = 0;
			byte[] rawBytes = character.getRawBytes();
			while (charIndexToParse < 21) {
				m_charOutputRow++;
				m_charOutputCol=7;
				
				// Ouput screen codes 0C,18,0D,0E,24,01,2A,24,FF at (7,9)
				// that matches chars "CODE 1: "
				stringOffset += displayStringAt(0xA9D9+stringOffset);
				
				byte[] code = readChars(7);
				
				for (int i=0;i<7;i++) {
					byte flippingByte = getMem(0xA396+charIndexToParse);
					byte upperOrLower = getMem(0xA3AB+charIndexToParse);
					
					byte c = rawBytes[flippingByte];
					byte v = (byte) (code[i] ^ flippingByte);
					if (upperOrLower == 1) {
						c = (byte) (c | ((v << 4) & 0x0F0));
					}
					else {
						c = (byte) (c | (v & 0x0F));
					}
					rawBytes[flippingByte] = c;
					charIndexToParse++;
				}
			}
			
			// Perform some validation
			// Strength, Dexterity and Intellect >= 0 and <= 18
			boolean valid = ((rawBytes[0x10] & 0x0FF) >= 0) && ((rawBytes[0x10] & 0x0FF) <= 18);
			valid &= ((rawBytes[0x11] & 0x0FF) >= 0) && ((rawBytes[0x11] & 0x0FF) <= 18);
			valid &= ((rawBytes[0x12] & 0x0FF) >= 0) && ((rawBytes[0x12] & 0x0FF) <= 18);
			// Class type is 1 or 2
			valid &= ((rawBytes[0x14] & 0x0FF) > 0) && ((rawBytes[0x14] & 0x0FF) < 3);
			// Item index for each category is >=0 and <=4
			valid &= ((rawBytes[0x19] & 0x0FF) >= 0) && ((rawBytes[0x19] & 0x0FF) < 6);
			valid &= ((rawBytes[0x1A] & 0x0FF) >= 0) && ((rawBytes[0x1A] & 0x0FF) < 6);
			valid &= ((rawBytes[0x1B] & 0x0FF) >= 0) && ((rawBytes[0x1B] & 0x0FF) < 6);
			valid &= ((rawBytes[0x1C] & 0x0FF) >= 0) && ((rawBytes[0x14] & 0x1C) < 6);
			
			if (!valid) {
				for (int i=0x10;i<rawBytes.length;i++) {
					rawBytes[i] = 0;
				}
			}
			
			character.setRawBytes(rawBytes);
			displayMenuFooter();
			return;
		}
	}

	private byte generateAndDisplayRandom() {
		delayInMillis(356);
		byte value = (byte) (6 + new Random().nextInt(13));
		outputWord(value);
		return value;
	}
	
	/**
	 * Display a string from ROM 
	 * @param offset from addr $A6A7 (string must end with $FF)
	 * @return the new offset
	 */
	private int displayStringAtCol5(int offset) {
		m_charOutputCol=5;
		return displayString(offset);
	}

	private int displayStringNextLineCol7(int offset) {
		m_charOutputRow++;
		m_charOutputCol=7;
		return displayString(offset);
	}
	
	/**
	 * Display a string from ROM 
	 * @param offset from addr $A6A7 (string must end with $FF)
	 * @return the new offset
	 */
	private int displayString(int offset) {
		byte c = getMem(0xA6A7+offset);
		while (c != (byte)(0xFF)) {
			outputChar(c);
			c = getMem(0xA6A7+(++offset));
		}
		return ++offset;
	}
	
	@Override
	public void exit() {
	}
}
