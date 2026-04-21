package org.pixs.mazemaster.states;

import java.util.Random;

import org.pixs.hardware.Sprite;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.Character;
import org.pixs.mazemaster.Game;
import org.pixs.mazemaster.RomText;

public class MainMenuState extends GameState {

	private static final Random RANDOM = new Random();

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

		// Lecture de la touche press�e dans le menu principal	
		boolean exitMenu = false;
		while (!exitMenu) {
		
			// Affichage du menu principal avec les 5 choix
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
		
		// "WHO WILL BUY? 1-3 (0:NONE)" at (7,7)
		console().moveTo(7, 7);
		displayText(RomText.WHO_WILL_BUY);
		
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
		
		// "TYPE (1-WE 2-AR 3-SH 4-MI)?" at (7,9)
		console().row += 2;
		console().col = 7;
		displayText(RomText.ITEM_TYPE_PROMPT);
		
		int pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		while (pressedNumber > 3) {
			pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		}
		int itemType = pressedNumber;
		
		// "ITEM NUMBER?: (1-4)" at (7,10)
		console().row++;
		console().col = 7;
		displayText(RomText.ITEM_NUMBER_PROMPT);
		
		pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		while (pressedNumber > 3) {
			pressedNumber = (readKeyboardAsPETSCII() - 0x31) & 0xFFFF;
		}
		byte itemNumber = (byte) pressedNumber;
		int itemPrice = rom().itemPrice(itemType, pressedNumber);

		if (itemPrice > selectedCharacter.getGold()) {
			// "INSUFFICIENT FUNDS" at (11,11)
			console().row++;
			console().col = 11;
			displayText(RomText.INSUFFICIENT_FUNDS);
			
			console().row += 2;
			console().col = 0x0A;
			displayText(RomText.HIT_ANY_KEY);
			
			readKeyboardAsPETSCII();
			return;
		}
		else {
			// Remove item price from the character gold
			selectedCharacter.setGold(selectedCharacter.getGold() - itemPrice);
			selectedCharacter.setItem(itemType, (byte)(itemNumber+1));

			// j85E5 in source.asm jumps to j85DE: display "HIT ANY KEY TO GO ON" and wait
			console().row += 2;
			console().col = 0x0A;
			displayText(RomText.HIT_ANY_KEY);
			readKeyboardAsPETSCII();
		}
	}

	private void deleteCharacter() {
		clearMenu();
		
		// "DELETE CHARACTER 1-3 (0:NONE)" at (6,8)
		console().moveTo(6, 8);
		displayText(RomText.DELETE_CHARACTER);
		
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
		// "EXAMINE WHICH 1-3 (0-NONE)" at (7,8)
		console().moveTo(7, 8);
		displayText(RomText.EXAMINE_WHICH);
		
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
		console().row = 0x06;
		console().col = 0x05;
		for (int i=0;i<0x10;i++) {
			outputChar(selectedCharacter.getNameAsBytes()[i]);
		}
		
		// Ouput screen codes 1D,11,0E,24 at (22,6)
		// that matches chars "THE "
		console().col++;
		displayText(RomText.THE);

		// "THE WARRIOR" or "THE WIZARD" according to class type
		displayText(selectedCharacter.getClassType() == 1 ? RomText.WARRIOR : RomText.WIZARD);
		
		console().row++;
		int savedCol = console().col;
		console().col = 5;
		// write char '-' till saved column position
		do {
			outputChar((byte)0x29);
		}
		while (console().col < savedCol);
		
		// Ouput screen codes 1C,1D,1B,0E,17,10,1D,11,2A,24 at (5,8) 
		// that matches chars "STRENGTH: "
		console().row++;
		int stringIndex = displayStringAtCol5(6);
		outputWord(selectedCharacter.getStrength());
		
		// Ouput screen codes 12,17,1D,0E,15,15,0E,0C,1D,2A,24 at (19,8) 
		// that matches chars "INTELLECT: "
		console().col+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getIntellect());
		
		// Ouput screen codes 0D,0E,21,1D,0E,1B,12,1D,22,2A,24 at (5,9) 
		// that matches chars "DEXTERITY: "
		console().row++;
		stringIndex = displayStringAtCol5(stringIndex);
		outputWord(selectedCharacter.getDexterity());
		
		// Ouput screen codes 0C,18,17,1C,1D,12,1D,1E,1D,12,18,17,2A,24 at (20,9) 
		// that matches chars "CONSTITUTION: "
		console().col+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getConstitution());
		
		// Ouput screen codes 10,18,15,0D,2A,24,FF at (5,10) 
		// that matches chars "GOLD: "
		console().row++;
		stringIndex = displayStringAtCol5(stringIndex);
		outputWord(selectedCharacter.getGold());
		
		// Ouput screen codes 0E,21,19,0E,1B,12,0E,17,0C,0E,2A,24 at (5,10) 
		// that matches chars "EXPERIENCE: "
		console().col+=2;
		stringIndex = displayString(stringIndex);
		outputWord(selectedCharacter.getXp());
		
		// "ITEMS:"
		console().row++;
		console().col = 5;
		displayText(RomText.ITEMS_LABEL);
		
		for (int i=0;i<4;i++) {
			console().row++;
			console().col=5;
			byte item = selectedCharacter.getItemCode(i);
			
			// If no item defined, skip item number computation
			int itemNumber = 0;
			if (item != 0) {
				// Else, item number = item identifier + 4*item slot number
				itemNumber = i*4+item;
			}
			
			// Items name offset are stored in a table store at $A42C
			// For an empty slot (item code 0) we show the "empty" name at offset 0.
			// Otherwise Rom.itemNameAddress handles the slot*4 + itemCode arithmetic.
			if (item == 0) {
				displayStringAt(0xBF00);
			} else {
				displayStringAt(rom().itemNameAddress(i, item));
			}
		}
		console().row++;
		console().col=5;
		
		// "CODE: " at (5,15)
		displayText(RomText.CODE_LABEL);
		
		int groupIndex = 0;
		for (int i = 0; i < 21; i++) {
			byte flippingByte = rom().codeFlippingByte(i);
			boolean upperNibble = rom().codeNibbleFlag(i) != 0;
			byte b = selectedCharacter.getRawBytes()[flippingByte];
			if (upperNibble) b = (byte) ((b >> 4) & 0x0F);
			outputChar((byte) ((b ^ flippingByte) & 0x0F));
			
			if (++groupIndex == 7) {
				groupIndex = 0;
				console().col++;
			}
		}
		
		console().row++;
		console().col = 0x0A;
		displayText(RomText.HIT_ANY_KEY);

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
		displayText(RomText.RANDOMIZE_YN);
		
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
		
		// Lecture du tampon de saisie (10 caracteres at $0B00)
		// et stockage dans le nom du personnage libre	
		byte[] name = readChars(0x10);
		
		Character character = getGame().getCharacter(index);
		character.setNameFromByte(name);
		character.resetAttributes(); 
		console().row++;
		
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
			
			console().row++;
			console().col = 7;
			displayText(RomText.CLASS_WAR_WIZ);
			
			int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
			while (readKeyboardAsPETSCII - 0x31 > 1 || readKeyboardAsPETSCII - 0x31 < 0) {
				readKeyboardAsPETSCII = readKeyboardAsPETSCII();
			}
			int classType = readKeyboardAsPETSCII - 0x31 + 1;
			character.setClassType(classType);
			outputChar((byte) classType);
			console().row++;
			
			// Generate random gold and reset following data (experience, items, ...)
			character.setGold((RANDOM.nextInt(256) & 0xFF) | 0x40);
			character.setXP(0);
			character.clearIndicators();
		}
		else {
			int charIndexToParse = 0;
			int stringOffset = 0;
			byte[] rawBytes = character.getRawBytes();
			while (charIndexToParse < 21) {
				console().row++;
				console().col=7;
				
				// Ouput screen codes 0C,18,0D,0E,24,01,2A,24,FF at (7,9)
				// that matches chars "CODE 1: "
				stringOffset += displayStringAt(0xA9D9+stringOffset);
				
				byte[] code = readChars(7);
				
				for (int i = 0; i < 7; i++) {
					byte flippingByte = rom().codeFlippingByte(charIndexToParse);
					boolean upperNibble = rom().codeNibbleFlag(charIndexToParse) == 1;

					byte c = rawBytes[flippingByte];
					byte v = (byte) (code[i] ^ flippingByte);
					if (upperNibble) c = (byte) (c | ((v << 4) & 0x0F0));
					else             c = (byte) (c | (v & 0x0F));
					rawBytes[flippingByte] = c;
					charIndexToParse++;
				}
			}
			
			// Perform some validation (matches b8289/b829F in source.asm)
			// Strength, Intellect, Dexterity <= 18 ($13)
			boolean valid = (rawBytes[0x10] & 0x0FF) <= 18;
			valid &= (rawBytes[0x11] & 0x0FF) <= 18;
			valid &= (rawBytes[0x12] & 0x0FF) <= 18;
			// Class type is 1 or 2
			valid &= ((rawBytes[0x14] & 0x0FF) > 0) && ((rawBytes[0x14] & 0x0FF) < 3);
			// Item index for each category is < 5
			valid &= (rawBytes[0x19] & 0x0FF) < 5;
			valid &= (rawBytes[0x1A] & 0x0FF) < 5;
			valid &= (rawBytes[0x1B] & 0x0FF) < 5;
			valid &= (rawBytes[0x1C] & 0x0FF) < 5;
			
			if (!valid) {
				for (int i=0x10;i<rawBytes.length;i++) {
					rawBytes[i] = 0;
				}
			}
			
			character.setRawBytes(rawBytes);
		}
	}

	private byte generateAndDisplayRandom() {
		delayInMillis(356);
		byte value = (byte) (6 + RANDOM.nextInt(13));
		outputWord(value);
		return value;
	}
	
	/**
	 * Display a string from ROM 
	 * @param offset from addr $A6A7 (string must end with $FF)
	 * @return the new offset
	 */
	private int displayStringAtCol5(int offset) {
		console().col=5;
		return displayString(offset);
	}

	private int displayStringNextLineCol7(int offset) {
		console().row++;
		console().col=7;
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
