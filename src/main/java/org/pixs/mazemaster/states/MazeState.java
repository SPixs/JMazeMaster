package org.pixs.mazemaster.states;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import org.pixs.JoystickButton;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.Character;
import org.pixs.mazemaster.Game;

public class MazeState extends GameState {

	int m_lightCounter = 0;
	
	private int m_xPos;
	private int m_yPos;

	private int m_orientation; // 0 = NORTH, 1 = EAST, 2 = SOUTH, 3 = WEST
	private int m_level = 0;

	private int m_magicArmor;

	private boolean m_wanderingMonsters;

	private boolean m_messageInWindow;

	private WallType[][] m_facingWalls;

	private boolean m_exitMaze;

	private int m_infightEscapeCounter;

	private int m_lastTriggerIndex;

	public MazeState(Game game) {
		super(game);
	}

	@Override
	public void enter() {
		VicIIDisplay vicII = getGame().getVicII();
		
		m_lightCounter = 0;
		
		// Init characters conditions with constitution
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.getRawBytes()[0] != 0x24) {
				character.setCondition(character.getConstitution());
				character.setMazeXp(character.getXp());
				character.setSpellPoints(0);
				
				// Loof for a Staff of light item in inventory
				if (character.getItemCode(3) == 0x01) {
					m_lightCounter = 0xFA;
				}
				
				// Initialize spell points of wizards.
				// It equals to 1 + lvl + (intellect - $0F, if intellect > $0F) 
				if (character.getClassType() == 0x02) {
					int points = character.getXp() / 1024 + 1 + Math.max(0, character.getIntellect() - 15);
					character.setSpellPoints(points);
				}
			}
		}
		
		vicII.setBorderColor(0x06);
		
		m_xPos = 0;
		m_yPos = 0;
		m_orientation = 0; // NORTH, EAST, SOUTH, WEST
		m_level = 0;
		// TODO : add main loop counter*
		m_magicArmor = 0;
		
		clearBitmapAndInitScreenColor();
		clear3DViewAndDrawBorder();
		initMazeViewFooterLogoAndSprites();
		initWanderingMonsters();
		draw3DView();
		
		m_exitMaze = false;
		long startNanoTime = System.nanoTime();
		boolean timerToggleFlag = false;
		
		// Main loop duration on original game is 68 CPU cycles = 69.04 µs = 0.069 ms
		while (!m_exitMaze) {
			long startTime = System.nanoTime();
			int pressedKey = readKeyboardAsPETSCIINoBlocking();
			if (pressedKey != 0) {
				switch (pressedKey) {
					case 0x4B: // 'k' key
						goForwardAndOpenDoor();
						break;
					case 0x49: // 'I' key
						goForward();
						break;
					case 0x4C: // 'L' key
						turnRight();
						break;
					case 0x4A: // 'J' key
						turnLeft();
						break;
					case 0x53: // 'S' key
						castSpell();
						break;
					case 0x50: // 'P' key
						togglePause();
						break;
					case 0x31:
					case 0x32:
					case 0x33:
						displayCharacter(pressedKey - 0x31);
					default:
						break;
				}
			}
			
			Set<JoystickButton> pressedButton = getGame().getCia1().getPressedButton();
			if (pressedButton.contains(JoystickButton.UP)) {
				goForward();
			}
			else if (pressedButton.contains(JoystickButton.LEFT) ) {
				turnLeft();
			}
			else if (pressedButton.contains(JoystickButton.RIGHT) ) {
				turnRight();
			}
			else if (pressedButton.contains(JoystickButton.FIRE) ) {
				goForwardAndOpenDoor();
			}
			
			// The code after is executed once every 4.86 seconds
			// (nearly 4783506) cpu cycles
			if (System.nanoTime() - startNanoTime > 4.8551e9) {
				startNanoTime = System.nanoTime();
				
				// Look for wandering monsters 
				if (m_wanderingMonsters && ((new Random().nextInt(32) & 0x1F) == 0)) {
					encounterRandomMonster();
				}
				else {
					// Execute some code here half the time (light decline and characters healing)
					// (nearly every 9.7 seconds, =9566743 cpu cycles)
					if (timerToggleFlag) {
						// Process light decrease
						if (m_lightCounter > 0 && m_lightCounter != 0xF0) {
							m_lightCounter--;
							System.out.println("LIGHT DECREASE : " + m_lightCounter);
							if (m_lightCounter == 0) {
								playRingSound();
								draw3DView();
							}
						}
						
						// Heal characters holding an 'Amulet of Healing'
						boolean healPerformed = false;
						for (int i=0;i<3;i++) {
							Character character = getGame().getCharacter(i);
							if (character.isValid() && character.getItemCode(3) == 0x03) {
								if (character.getCondition() < character.getConstitution()) {
									character.setCondition(Math.min(character.getConstitution(), character.getCondition()+1));
									healPerformed = true;
								}
							}
						}
						if (healPerformed) {
							displayStatsLines();
						}
					}
				}
				timerToggleFlag = !timerToggleFlag;
			}
		}
		
		hideSprites();
		returnToBaseCamp();
	}

	private void encounterRandomMonster() {
		int monsterID = 6 * m_level + new Random().nextInt(16);
		processMonsterEncounter(monsterID);
	}

	/**
	 * $897F
	 * Part has come back to base camp
	 */
	private void returnToBaseCamp() {
		// Is there at least one character alive ?
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.isValid()) {
				boolean levelGained = (character.getMazeXp() >> 10) > (character.getXp() >> 10);
				if (levelGained) {
					character.setConstitution(Math.min(255, character.getConstitution() + new Random().nextInt(3) + 1));
				}
				int attributeBonus = new Random().nextInt(2) + 1;
				switch (new Random().nextInt(3)) {
					case 0:
						character.setStrengh(Math.min(18, attributeBonus));
						break;
					case 1:
						character.setIntellect(Math.min(18, attributeBonus));
						break;
					case 2:
						character.setDexterityt(Math.min(18, attributeBonus));
						break;
					default:
						throw new IllegalStateException();
				}
			}
		}
		setState(new MainMenuState(getGame()));
	}

	private void castSpell() {
		resetMessageWindowAndCursor();
		
		// Ouput screen codes 20,11,18,24,20,12,15,15,24,0C,0A,1C,1D,27,24,01,49,03,24,4A,00,2A,17,18,17,0E at (22,6)
		// that matches chars "WHO WILL CAST? 1-3 (0:NONE)"
		for (int i=0;i<0x1B;i++) {
			outputChar(getMem(0xA7D5+i));
			if (i==13) {
				nextRowInMessageWindow();
			}
		}
		
		Character selectedCharacter = null;
		while (selectedCharacter == null) {
			// Wait for character number or 0 to exit
			int pressedNumber = readKeyboardAsPETSCII() - 0x30;
			while (pressedNumber > 3) {
				pressedNumber = readKeyboardAsPETSCII() - 0x30;
			}
			
			if (pressedNumber == 0) {
				resetMessageWindowAndCursor();
				return;
			}
			
			Character character = getGame().getCharacter(pressedNumber-1);
			if (character.isValid()) {
				selectedCharacter = character;
			}
		}
		
		next2RowsInMessageWindow();
		
		// Ouput screen codes 1C,19,0E,15,15,24,17,1E,16,0B,0E,1B,2A,24 (22,9)
		// that matches chars "SPELL NUMBER: "
		displayString(0xBC88, 0x0E);
		
		byte[] spellNumberChars = readChars(3);
		int spellNumber = convertToWord(spellNumberChars);
		if (spellNumber == 0 || spellNumber > 18) {
			resetMessageWindowAndCursor();
			return;
		}
		
		byte spellCategory = getMem(0xA3C0+spellNumber);
		
		// if combat spell, display warning message in window and return to main loop
		if (spellCategory == 0) {
			m_charOutputRow++;
			m_charOutputCol = 22;
			m_messageInWindow = true;
			
			// Ouput screen codes 0F,18,1B,24,0C,18,16,0B,0A,1D,24,18,17,15,22 at (22,10)
			// that matches chars "FOR COMBAT ONLY"
			displayString(0xA7F0, 0x0F);
			return;
		}
		
		// load required spell points for this spell
		byte requiredSpellPoints = getMem(0xA3D3+spellNumber);
		if (requiredSpellPoints > selectedCharacter.getSpellPoints()) {
			m_charOutputRow++;
			m_charOutputCol = 22;
			m_messageInWindow = true;
			
			// Or, ouput screen codes 15,0A,0C,14,24,1C,19,0E,15,15,24,19,1D,1C,1A at (22,10)
			// that matches chars "LACK SPELL PTS"
			displayString(0xA7F0+0xF, 0x1D-0x0F);
			return;
		}
		
		selectedCharacter.setSpellPoints(selectedCharacter.getSpellPoints()-requiredSpellPoints);
		resetMessageWindowAndCursor();
		switch (spellNumber) {
			case 0x03: castHeal(selectedCharacter); break; 			// Spell 3 ? (HEAL)
			case 0x04: castOrient(); break; 		// Spell 4 ? (ORIENT)
			case 0x07: castRestore(); break; 		// Spell 7 ? (RESTORE)
			case 0x08: castLight(); break; 			// Spell 8 ? (LIGHT)
			case 0x0B: castRegenerate(); break; 	// Spell 11 ? (REGENERATE)
			case 0x0C: castCatEyes(); break; 		// Spell 12 ? (CAT EYES)
			case 0x0E: castPhaseWall(); break; 		// Spell 14 ? (PHASE WALL)
			case 0x10: castTeleport(); break; 		// Spell 16 ? (TELEPORT)
			case 0x0F: castShadowShield(); break;	// Spell 15 ? (SHADOW SHIELD)
			case 0x12: castRenewal(); break; 		// Spell 18 ? (RENEWAL)
			default:
				throw new IllegalStateException();
		}
	}
	
	/**
	 * Cast spell 3 : heal
	 * This spell will heal 1-32 points of the spellcasterís own CND.
	 * @param selectedCharacter 
	 */
	private void castHeal(Character selectedCharacter) {
		int heal = new Random().nextInt(31) + 1;
		selectedCharacter.setCondition(Math.min(selectedCharacter.getCondition()+heal, selectedCharacter.getConstitution()));
		displayStatsLines();
	}

	/**
	 * Cast spell 4 : orient
	 * 
	 * This spell will inform the party of its location in relation
	 * to the exit stairway from the maze, on level 0, as well as 
	 * telling the current direction the party faces.
	 */
	private void castOrient() {
		// Ouput screen codes $22,$18,$1E,$24,$0A,$1B,$0E,$24
		// that matches chars "YOU ARE "
		displayString(0xBC96, 0x08);
		outputWord(m_yPos);
		
		// Ouput screen codes $24,$1C,$19,$0A,$0C,$0E,$1C,
		// that matches chars " SPACES"
		displayString(0xBC96+0x08, 0x0F-0x08);
		nextRowInMessageWindow();
		
		// Ouput screen codes $17,$18,$1B,$1D,$11,$25,$24
		// that matches chars "NORTH, "
		displayString(0xBC96+0x0F, 0x16-0x0F);
		outputWord(m_xPos);
		displayString(0xBC96+0x16, 0x1D-0x16);
		nextRowInMessageWindow();
		
		// Ouput screen codes $0E,$0A,$1C,$1D,$25,$24,$0A,$17,$0D,$24
		// that matches chars "EAST, AND "
		displayString(0xBC96+0x1D, 0x27-0x1D);
		
		outputChar((byte) m_level);
		m_charOutputRow++;
		m_charOutputCol = 22;
		
		// Ouput screen codes $15,$0E,$1F,$0E,$15,$1C,$24,$0B,$0E,$15,$18,$20,$24,$1D,$11,$0E
		// that matches chars "LEVELS BELOW THE"
		// Ouput screen codes $0E,$17,$1D,$1B,$22,$24,$1C,$1D,$0A,$12,$1B,$1C,$25
		// that matches chars "ENTRY STAIRS,"
		displayString(0xBC96+0x27, 0x37-0x27);
		m_charOutputRow++;
		m_charOutputCol = 22;
		displayString(0xBC96+0x37, 0x44-0x37);
		nextRowInMessageWindow();
		
		// Ouput screen codes $17,$18,$20,$24,$0F,$0A,$0C,$12,$17,$10,$24
		// that matches chars "NOW FACING "
		displayString(0xBC96+0x44, 0x4F-0x44);
		
		// Load text offset for direction NORTH = 0, EAST = 5, SOUTH = 10, WEST = 15 
		int offset = getMem(0xA424+m_orientation);
		displayString(0xBCE5+offset, 5);
		m_messageInWindow = true;
	}

	/**
	 * Cast spell 8 : light
	 * This spell allows the party to see 4 spaces ahead 
	 * (extended distance), as well as detect secret doors. 
	 * The effects of this spell last at least 15 minutes in real
	 * time, and a gong will signal termination of spell.
	 */
	private void castLight() {
		if (m_lightCounter < 0xF0) {
			m_lightCounter = 0x64;
		}
		draw3DView();
	}

	/**
	 * Cast spell 7 : restore
	 * This spell will heal the CND of every party member by 1-16 points.
	 */
	private void castRestore() {
		healParty(new Random().nextInt(15)+1);
	}

	/**
	 * Cast spell 11 : regenerate
	 * This spell is similar to spell 7, except that it heals 1-32 points.
	 */
	private void castRegenerate() {
		healParty(new Random().nextInt(31)+1);
	}
	
	private void healParty(int healValue) {
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.isValid()) {
				character.setCondition(Math.min(character.getCondition()+healValue, character.getConstitution()));
			}
		}
		displayStatsLines();
	}

	/**
	 * Cast spell 12 : cat eyes
	 * This spell is similar to Spell 8, for double duration 
	 * (at least 30 minutes of real time).
	 */
	private void castCatEyes() {
		if (m_lightCounter < 0xF0) {
			m_lightCounter = 0xC8;
		}
		draw3DView();
	}

	/**
	 * SPELL 14 PHASE WALL
	 * 
	 * Before casting this spell, the party should be standing directly in front of a wall 
	 * it wishes to pass through. When the spell is cast, the wall will vanish for one move. 
	 * If the party turns left or right, the wall will reappear. While the wall is gone, the
	 * party will be able to see what lies beyond it, and even step through it. 
	 * 
	 * The wall will rematerialize immediately. 
	 * 
	 * This spell will not function on the lowest maze level.
	 */
	private void castPhaseWall() {
		if (m_level == 4) {
			return;
		}
		
		m_facingWalls[0][2] = WallType.NONE;
		drawWalls(m_facingWalls);
	}

	/**
	 * SPELL 16 TELEPORT
	 * 
	 * This spell can be used to move the party to nearly any spot in the maze. 
	 * Use the joystick controls to indicate the number of squares north to move 
	 * (a negative value moves you south), the number of squares east 
	 * (a negative value moves you west), and the number of levels to go down 
	 * (a negative value moves you up). 
	 * 
	 * Teleporting to the bottom level "bounces" you up to the top level.
	 */
	private void castTeleport() {
		// Ouput screen codes 1D,0E,15,0E,19,18,1B,1D,26,26,26,15,0E,1F,0E,15,1C at (22,22)
		// that matches chars "TELEPORT..."
		displayString(0xBCF9, 0x0B);
		next2RowsInMessageWindow();

		// Ouput screen codes 15,0E,1F,0E,15,1C,24,0D,18,20,17,2A,24 at (22,24)
		// that matches chars "LEVELS DOWN: "
		displayString(0xBCF9+0x0B, 0x18-0x0B);
		
		// ask for a numeric value between (0..40) <-> (-20..+20) and load in ACC
		int value = selectValueInRange20();

		int newLevel = 0;
		while (true) {
			// Go downstairs ?
			if (value > 20) {
				newLevel = m_level + value - 20;
				if (newLevel <= 4) {
					// are we trying to teleport to last floor (4) ?
					if (newLevel == 4) { 
						// if so, 'bounce' party to first level (floor 0)
						newLevel = 0; 
					} 
					break;
				}
				waitForJoystickRelease();
			}
			if (value < 20) {
				// Go upstairs ?
				newLevel = m_level + value - 20;
				if (newLevel >= 0) {
					break;
				}
				waitForJoystickRelease();
			}
			else {
				// same level...
				break;
			}
		}
		m_level = newLevel;
		
		// request north offset value
		waitForJoystickRelease();
		nextRowInMessageWindow();
		
		// Ouput screen codes 17,18,1B,1D,11,2A,24 at (22,25)
		// that matches chars "NORTH: "
		displayString(0xBCF9+0x18, 0x1F-0x18);
		
		m_yPos = selectNewLocation(m_yPos);
		waitForJoystickRelease();
		nextRowInMessageWindow();
		
		// Ouput screen codes 0E,0A,1C,1D,2A,24 at (22,26)
		// that matches chars "EAST: "
		displayString(0xBCF9+0x1F, 0x25-0x1F);
		
		m_xPos = selectNewLocation(m_xPos);
		
		resetMessageWindowAndCursor();
		initWanderingMonsters();
		draw3DView();
		handleTriggers();
	}
	
	private int selectNewLocation(int current) {
		int newLocation = current;
		do {
			newLocation = current;
			int selectValueInRange20 = selectValueInRange20();
			newLocation += selectValueInRange20 - 20;
			if ((newLocation <0 || newLocation > 19)) {
				waitForJoystickRelease();
			}
		}
		while (newLocation <0 || newLocation > 19);
		return newLocation;
	}
	
	public void waitForJoystickRelease() {
		while (!getGame().getCia1().getPressedButton().isEmpty()) {
			Thread.yield();
		}
		delay();
	}
	
	/**
	 * Display an initial value of 0, ranging (-20..+20)
	 * and that can be modifier either with joystick (left,right)
	 * or cursor key (left,right).
	 * Loop until value is validated by space or fire button.
	 * 
	 * @return selected value in range 0..40
	 */
	private int selectValueInRange20() {
		int value = 20;
		int savedCol = m_charOutputCol;
		
		while (true) {
			if (value >= 20) {
				outputChar((byte)0x4B); // '+'
				outputWord(value-20);
				outputChar((byte)0x24);
			}
			else {
				outputChar((byte)0x49); // '-'
				outputWord(20-value);
				outputChar((byte)0x24);
			}
			
			m_charOutputCol = savedCol;
			
			Set<JoystickButton> pressedButton = getGame().getCia1().getPressedButton();
			int pressedKey = readKeyboardAsPETSCIINoBlocking();
			if (pressedButton.contains(JoystickButton.LEFT) || pressedKey == 0x9D) {
				value = Math.max(0, value-1);
			}
			else if (pressedButton.contains(JoystickButton.RIGHT) || pressedKey == 0x1D) {
				value = Math.min(40, value+1);
			}
			
			if (pressedKey == 0x20 || pressedButton.contains(JoystickButton.FIRE)) {
				return value;
			}
			
			delay();
		}
	}

	/**
	 * SPELL 15 SHADOW SHIELD
	 * 
	 * This spell lowers the AR of each member of the party by 2 for the duration of the entire maze expedition.
	 */
	private void castShadowShield() {
		m_magicArmor = 2;
		displayStatsLines();
	}

	/**
	 * SPELL 18 
	 * 
	 * RENEWAL This spell will completely restore all surviving members of your party to their full constitution.
	 */
	private void castRenewal() {
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.isValid()) {
				character.setCondition(character.getConstitution());
			}
		}
		displayStatsLines();
	}

	public int convertToWord(byte[] chars) {
		int result = 0;
		for (byte v : chars) {
			if (v == 0x24) return result;
			result = result * 10 + v;
		}
		return result;
	}

	public void delay() {
		delayCycles(352788);
	}
	
	public void delayCycles(int cyclesCount) {
		long startTime = System.nanoTime();
		long duration = (long) (cyclesCount * 1014.97288d);
		while (System.nanoTime() - startTime < duration) {
			Thread.yield();
		}
	}

	/**
	 * Entre en mode PAUSE
	 * 
	 * Fait clignoter le logo MAZE MASTER
	 * et boucle tant qu'aucune touche n'est pressÈe
	 */
	private void togglePause() {
		while (readKeyboardAsPETSCIINoBlocking() == 0) {
			useWhiteLogoOnRedBackground();
			delay();
			useRedLogoOnWhiteBackground();
			delay();
		}
	}

	private void turnLeft() {
		m_orientation = (m_orientation - 1 + 4) % 4;
		draw3DView();
	}

	private void turnRight() {
		m_orientation = (m_orientation + 1) % 4;
		draw3DView();
	}

	private void goForwardAndOpenDoor() {
		WallType frontWallType = getFrontWallType();
		if (frontWallType == WallType.NONE) {
			goForwardForce();
			return;
		}
		
		if (frontWallType == WallType.DOOR || frontWallType == WallType.HIDDEN) {
			// Should play sound
			playOpenDoorSound();
			goForwardForce();
			return;
		}
		
		hurtWall();
	}

	private void playOpenDoorSound() {
		// TODO Auto-generated method stub
		
	}

	private void goForward() {
		if (getFrontWallType() != WallType.NONE) {
			hurtWall();
			return;
		}
		else {
			goForwardForce();
		}
	}

	private void goForwardForce() {
		
		if (m_messageInWindow) {
			resetMessageWindowAndCursor();
			m_messageInWindow = false;
		}
		
		switch (m_orientation) {
			case 0: m_yPos = (m_yPos + 1) % 20; break;
			case 1: m_xPos = (m_xPos + 1) % 20; break;
			case 2: m_yPos = (m_yPos - 1 + 20) % 20; break;
			case 3: m_xPos = (m_xPos - 1 + 20) % 20; break;
			default:
				throw new IllegalStateException();
		}
		draw3DView();
		handleTriggers();
	}

	/**
	 * Test triggers for square at current position
	 * Trigger structure follow this format
	 * 
	 * $70 bytes grouped by pair
	 * each pair is coordinate (north, west)
	 * 4 first pairs are reserved for special triggers :
	 * $00-$01 is for upstairs
	 * $02-$03 is for downstairs
	 * $04-$05 is for clue
	 * $06-$07 is for hole in floor
	 * 
	 * All other pairs are monsters locations
	 * Note that monster number is the pair index AND with $0F
	 */
	private void handleTriggers() {
		byte[] triggers = getGame().getTriggers(m_level);
		for (int i=0;i<256;i+=2) {
			int triggerIndex = i >> 1;
			byte triggerY = triggers[i];
			byte triggerX = triggers[i+1];
			if (triggerX == m_xPos && triggerY == m_yPos) { 
				m_lastTriggerIndex = triggerIndex;
				switch (triggerIndex) {
					case 0:
						// 0 = upstairs
						processUpstairs();
						break;
					case 1:
						// 0 = downstairs
						processDownstairs();
						break;
					case 2:
						// 0 = clue
						processClue();
						break;
					case 3:
						// 0 = hole
						processHole();
						break;
					default:
						// Monster
						int monsterIndex = triggerIndex & 0x0F;
						processMonsterEncounter(monsterIndex);
						break;
				}
				return;
			}
		}
	}
	
	private void processUpstairs() {
		int messageAddress = (getMem(0xA43D) & 0x0FF) | (getMem(0xB4E1) << 8 & 0x0FFFF);
		resetMessageWindowAndCursor();
		playRingSound();
		m_charOutputCol = 21;
		m_charOutputRow = 6;
		
		int dispayedCount = 0;
		byte c = getMem(messageAddress);
		while (c != (byte)(0xFF)) {
			outputChar(c);
			dispayedCount++;
			c = getMem(messageAddress+dispayedCount);
			
			if (m_charOutputCol == 0x27) {
				m_charOutputRow++;
				m_charOutputCol = 0x15;
			}
		}
		int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		while (true) {
			if (readKeyboardAsPETSCII == 0x59) {
				// 'Y' pressed
				if (m_level == 0) {
					returnToBaseCamp();
					m_exitMaze = true;
					return;
				}
				m_level--;
				draw3DView();
				initWanderingMonsters();
				resetMessageWindowAndCursor();
				return;
			}
			else if (readKeyboardAsPETSCII == 0x4E) {
				// 'N' key pressed ?	
				resetMessageWindowAndCursor();
				return;
			}
			readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		}
	}

	private void playRingSound() {
		delay();
	}

	private void processDownstairs() {
		int messageAddress = (getMem(0xA43D+1) & 0x0FF) | (getMem(0xB4E1+1) << 8 & 0x0FFFF);
		resetMessageWindowAndCursor();
		playRingSound();
		m_charOutputCol = 21;
		m_charOutputRow = 6;
		
		int dispayedCount = 0;
		byte c = getMem(messageAddress);
		while (c != (byte)(0xFF)) {
			outputChar(c);
			dispayedCount++;
			c = getMem(messageAddress+dispayedCount);
			
			if (m_charOutputCol == 0x27) {
				m_charOutputRow++;
				m_charOutputCol = 0x15;
			}
		}
		int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		while (true) {
			if (readKeyboardAsPETSCII == 0x59) {
				// 'Y' pressed
				m_level++;
				draw3DView();
				initWanderingMonsters();
				resetMessageWindowAndCursor();
				return;
			}
			else if (readKeyboardAsPETSCII == 0x4E) {
				// 'N' key pressed ?	
				resetMessageWindowAndCursor();
				return;
			}
			readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		}
	}

	private void processClue() {
		int messageAddress = (getMem(0xA43D+2+(m_level<<1)) & 0x0FF) | ((getMem(0xB4E1+2+(m_level<<1)) << 8) & 0x0FFFF);
		resetMessageWindowAndCursor();
		playRingSound();
		m_charOutputCol = 21;
		m_charOutputRow = 6;
		
		int dispayedCount = 0;
		byte c = getMem(messageAddress);
		while (c != (byte)(0xFF)) {
			outputChar(c);
			dispayedCount++;
			c = getMem(messageAddress+dispayedCount);
			
			if (m_charOutputCol == 0x27) {
				m_charOutputRow++;
				m_charOutputCol = 0x15;
			}
		}
		
		m_messageInWindow = true;
		if (m_level != 4) {
			return;
		}
		
		// Enigma
		byte[] answer = readChars(9);
		boolean correct = true;
		for (int i=0;i<4;i++) {
			correct &= answer[i] == getMem(0xA428+i);
		}
		// If answer is incorrect, return one space WEST
		if (!correct) {
			m_xPos--;
			draw3DView();
		}
		resetMessageWindowAndCursor();
	}

	private void processHole() {
		System.out.println("HOLE");
		m_level++;
		draw3DView();
		initWanderingMonsters();
	}

	private void processMonsterEncounter(int monsterIndex) {
		// add 6*level to monster ID -> final monster ID in range ($0..$27=Balrog)
		monsterIndex = m_level * 6 + monsterIndex;
		
		System.out.println("MONSTER : " + monsterIndex);
		
		// If monster ID is 39 ($27), ensure that party position is (3,19) (BALROG location)
		// If party is anywhere else, load monster ID 25 ($19) instead...
		if (monsterIndex == 0x27 && m_xPos != 3 && m_yPos != 19) {
			monsterIndex = 0x19;
		}
		
		// Load monster sprite descriptors
		int offset = monsterIndex * 4;
		int multiColor0 = getMem(0xBE60+offset); 
		int multiColor1 = getMem(0xBE61+offset); 
		int spriteBottomAddress = (getMem(0xBE63+offset) & 0x0FF) << 8;
		int spriteTopAddress = (getMem(0xBE62+offset) & 0x0FF) << 8;
		if (spriteTopAddress < 0x0B0) {
			spriteTopAddress = ((spriteTopAddress + 0x0B0) & 0x0FF00) | 0x080;
		}
		System.out.println("Monster top sprites @ : " + Integer.toHexString(spriteTopAddress));
		System.out.println("Monster bottom sprites @ : " + Integer.toHexString(spriteBottomAddress));
		
		VicIIDisplay vicII = getGame().getVicII();
		for (int i=0;i<64;i++) {
			vicII.getSprite(0).spriteData[i] = getMem(spriteTopAddress+i);
			vicII.getSprite(1).spriteData[i] = getMem(spriteTopAddress+i+64);
			vicII.getSprite(2).spriteData[i] = getMem(spriteBottomAddress+i+0x80);
			vicII.getSprite(3).spriteData[i] = getMem(spriteBottomAddress+i+64+0x80);
		}
		clear3DView();

		// Display sprites 0..3 
		for (int i=0;i<4;i++) {
			vicII.getSprite(i).multiColor1 = multiColor0;		
			vicII.getSprite(i).multiColor2 = multiColor1;		
			vicII.getSprite(i).enabled = true;
		}
		
		toggleLogoTwice();
		
		// Play a sound while bliking the MAZE MASTER logo
		// TODO
		toggleLogoTwice();
		toggleLogoTwice();
		toggleLogoTwice();
		
		// Display monster name in message window
		resetMessageWindowAndCursor();
		
		m_charOutputCol = 24;
		
		if (monsterIndex >= 27) {
			offset = monsterIndex - 27;
			int nameOffset = getMem(0xA463+offset) & 0x0FF;
			displayStringAt(0xA90B+nameOffset);
			
			// And also at bottom of 3D view
			m_charOutputCol = 5;
			m_charOutputRow = 18;
			displayStringAt(0xA90B+nameOffset);
		}
		else {
			int nameOffset = getMem(0xA448+monsterIndex) & 0x0FF;
			displayStringAt(0xA80D+nameOffset);
			
			// And also at bottom of 3D view
			m_charOutputCol = 5;
			m_charOutputRow = 18;
			displayStringAt(0xA80D+nameOffset);
		}
		
		// Compute and display number of fighting monsters
		m_charOutputRow = 6;
		// Get a random value between 1 & 4
		int count = new Random().nextInt(4) + 1;
		count += m_level & 0x03;
		if (m_wanderingMonsters) {
			count += 2;
		}
		
		// is it a Balrog ?
		if (monsterIndex == 0x27) {
			count = 1;
		}
		
		// output number of monsters at (22,6)
		m_charOutputCol = 0x16;
		outputChar((byte) count);
		next2RowsInMessageWindow();
		
		// Ouput screen codes 4A,0F,28,12,10,11,1D,24,18,1B,24,4A,1B,28,1E,17 (22,9)
		// that matches chars "(F)IGHT OR (R)UN"
		displayString(0xBC00, 0x10);
		
		int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		while (readKeyboardAsPETSCII != 0x46 && readKeyboardAsPETSCII != 0x52) {
			readKeyboardAsPETSCII = readKeyboardAsPETSCII();
		}
		
		if (readKeyboardAsPETSCII == 0x46) {
			// Key 'F' pressed ?
			// Fight !
			startFight(monsterIndex, count);
		}
		else {
			// Run away !
			next2RowsInMessageWindow();
			boolean doMonsterEngage = doMonsterEngage(monsterIndex, 0);
			if (doMonsterEngage) {
				// Monster engages and so, catch party
				// Ouput screen codes 1D,11,0E,22,24,0C,0A,1E,10,11,1D,24,22,18,1E in message window
				// that matches chars "THEY CAUGHT YOU"
				displayString(0xBC2F, 0x0F);
				longDelay();
				startFight(monsterIndex, count);
			}
			else {
				// Party engages and so, can run away	
				// Ouput screen codes 22,18,1E,24,10,18,1D,24,0A,20,0A,22,26,26,26 in message window
				// that matches chars "YOU GO AWAY..."
				displayString(0xBC20, 0x0F);
				longDelay();
			}
		}
		
		hideSprites();
		m_messageInWindow = true;
		draw3DView();
	}
	
	private void startFight(int monsterID, int count) {
		int fightingMonsterIndex = 0;
		// AR reduction due to protection spell
		int magicARReduction = 0;
		int deadMonsters = 0;
		int partyHitScoreBonus = 0;
		m_infightEscapeCounter = 0;
		
		int[] monstersHP = new int[count];
		Arrays.fill(monstersHP, getMem(0xA498+monsterID));
		
		boolean fightInProgress = true;
		boolean monstersEngage = doMonsterEngage(monsterID, 0);
		
		while (fightInProgress) {
			if (monstersEngage) {
				resetMessageWindowAndCursor();
				delayInMillis(50); // SMa : Added to separate turns clearly ! (display is too fast in Java)
				fightingMonsterIndex = 0;
				
				// Ouput screen codes 16,18,17,1C,1D,0E,1B,1C,24,0A,1D,1D,0A,0C,14 at (22,6)
				// that matches chars "MONSTERS ATTACK"
				displayString(0xA9A4, 0x0F);
				
				longDelay();
				resetMessageWindowAndCursor();
				
				// Look for next monster still alive
				for (int i=0;i<count;i++) {
					if (monstersHP[i] > 0) {
						// select a random party target
						int targetIndex = new Random().nextInt() & 0x03;
						// copy original game 
						if (targetIndex == 0x03) {
							targetIndex = 0; 
						}
						Character target = getGame().getCharacter(targetIndex);
						if (!target.isValid()) {
							targetIndex = 0;
							target = getGame().getCharacter(targetIndex);
						}
						int armorRating = target.getArmorRating();
						armorRating = Math.max(0, armorRating - magicARReduction);
						
						// load dodge score matching this AR (score = $1C for an AR of -10 and score = $08 for an AR of +10)
						int dodgeScore = getMem(0x5300+armorRating);
						// load a random value in range 5..20
						int attackScore = new Random().nextInt(16) + 5;
						// add monster attack bonus to get attack score
						attackScore += getMem(0xA470+monsterID);
						
						int damage = 0;
						// if monster attack score >= character dodge score, attack succeeded
						if (attackScore >= dodgeScore) {
							// Compute monster damage. It is the sum of a random number (1..8)
							// and N*rand(1..8) where N is the monster attack bonus
							// load a random value in range 1..8 (base attack)
							damage = new Random().nextInt(8)+1;
							int attackBonus = getMem(0xA470+monsterID);
							for (int j=0;j<attackBonus;j++) {
								damage += new Random().nextInt(8)+1;
							}
						}
						
						
						// Display monster attack result
						// display character name in message windows
						display(target.getNameAsBytes());
						nextRowInMessageWindow();
						
						if (damage == 0) {
							// If monster damage is zero, output screen codes 0D,18,0D,10,0E,1C,24,1D,11,0E,24,0B,15,18,20
							// that matches chars "DODGES THE BLOW"
							displayString(0xB7B1, 0x0F);
						}
						else {
							// Damages are not null.
							// Output various string according to damage value :
							// damage < 7 : bytes 12,1C,24,1C,0C,1B,0A,19,0E,0D
							// that matche char "IS SCRAPED"
							// damage < 25 : bytes 12,1C,24,1C,15,0A,1C,11,0E,0D
							// that matche char "IS SLASHED"
							// damage >= 25 : bytes 12,1C,24,0B,0A,1D,1D,0E,1B,0E,0D
							// that matche char "IS BATTERED"
							int messageOffset = 0x0F;
							if (damage >= 7) messageOffset = 0x1A;
							if (damage >= 25) messageOffset = 0x25;
							displayStringAt(0xB7B1+messageOffset);
							nextRowInMessageWindow();
							
							// Ouput screen codes 1D,0A,14,0E,1C,24 in message window
							// that matches chars "TAKES "
							// Ouput screen codes 24,0D,0A,16,0A,10,0E,0A in message window
							// that matches chars " DAMAGE"
							for (int j=0;j<0x0D;j++) {
								outputChar(getMem(0xBC6E+j));
								if (j==5) {
									outputWord(damage);
								}
							}
							
							// Update current character CND and kill him if his condition is 0
							target.setCondition(Math.max(0,  target.getCondition() - damage));
							if (target.getCondition() == 0) {
								nextRowInMessageWindow();
								// Ouput screen codes 0A,17,0D,24,12,1C,24,14,12,15,15,0E,0D in message window
								// that matches chars "AND IS KILLED"	
								displayString(0xBC7B, 0x0D);
								getGame().deleteCharacter(targetIndex);
							}
						}
						
						displayStatsLines();
						longDelay();
						resetMessageWindowAndCursor();
						delayInMillis(50);
						
						// Party dead ?
						if (!getGame().getCharacter(0).isValid()) {
							// hide sprites
							m_exitMaze = true;
							return;
						}
						
						if (i < count-1) {
							if (checkInfightEscape(monsterID, count)) {
								// Party engages and so, can run away	
								// Ouput screen codes 22,18,1E,24,10,18,1D,24,0A,20,0A,22,26,26,26 in message window
								// that matches chars "YOU GO AWAY..."
								displayString(0xBC20, 0x0F);
								longDelay();
								return;
							}
						}
					}
				}
			}

			monstersEngage =  true;
				
			// party engage
			for (int i=0;i<3;i++) {
				resetMessageWindowAndCursor();
				delayInMillis(50); // SMa : Added to separate turns clearly ! (display is too fast in Java)
				Character character = getGame().getCharacter(i);
				if (character.isValid()) {
					// is it a warrior ?
					if (character.getClassType() == 1) {
						// Compute number of strikes for warrior
						character.setNumberOfStrikes(character.getMazeXp() / 8192);
					}
					else {
						character.setNumberOfStrikes(0);
					}
						
					// display character name at (22,6)
					display(character.getNameAsBytes());
					nextRowInMessageWindow();
					
					// Ouput screen codes 4A,20,28,0E,0A,19,24,18,1B,24,4A,1C,28,19,0E,15 (22,7)
					// that matches chars "(W)EAP OR (S)PEL"
					displayString(0xBC10, 0x10);
					
					int readKeyboardAsPETSCII = readKeyboardAsPETSCII();
					while (readKeyboardAsPETSCII != 0x53 && readKeyboardAsPETSCII != 0x57) {
						readKeyboardAsPETSCII = readKeyboardAsPETSCII();
					}
					
					// Weapon selected
					if (readKeyboardAsPETSCII == 0x57) {
						character.setSpellNumber(0);
					}
					else {
						next2RowsInMessageWindow();
						// Ouput screen codes 1C,19,0E,15,15,24,17,1E,16,0B,0E,1B,2A,24 (22,7)
						// that matches chars "SPELL NUMBER: "
						displayString(0xBC88, 0x0E);
						byte[] spellNumberBytes = readChars(3);
						int spellNumber = convertToWord(spellNumberBytes) & 0x0FF;
						if (spellNumber > 18) {
							spellNumber = 0;
						}
						
						// table indexed with spell number, storing zero value for combat spells
						// if spell is not a combat spell, let character uses its weapon
						if (getMem(0xA3C0+spellNumber) == 0) {
							int requiredSpellPoints = getMem(0xA3D3+spellNumber);
							if (requiredSpellPoints > character.getSpellPoints()) {
								spellNumber = 0; // Not enough spell points. Use weapon.
							}
							else {
								character.setSpellPoints(character.getSpellPoints() - requiredSpellPoints);
							}
						}
						else {
							spellNumber = 0; // Not a combat spell
						}
						character.setSpellNumber(spellNumber);
					}
				}
				else {
					break;
				}
			}
			
			int fightingCharacterIndex = 0;
			while (fightingCharacterIndex < 3 && getGame().getCharacter(fightingCharacterIndex).isValid() && fightInProgress) {
			
				// process characters actions
				resetMessageWindowAndCursor();
				delayInMillis(50); // SMa : Added to separate turns clearly ! (display is too fast in Java)
				
				if (checkInfightEscape(monsterID, count)) {
					// Party engages and so, can run away	
					// Ouput screen codes 22,18,1E,24,10,18,1D,24,0A,20,0A,22,26,26,26 in message window
					// that matches chars "YOU GO AWAY..."
					displayString(0xBC20, 0x0F);
					longDelay();
					return;
				}
			
				Character character = getGame().getCharacter(fightingCharacterIndex);
				display(character.getNameAsBytes());
				nextRowInMessageWindow();
				int spellNumber = character.getSpellNumber();
				int numberOfTurns= character.getNumberOfStrikes();
				
				// Weapon used ?
				if (spellNumber == 0) {
					
					// Character attacks with weapon
					// Compute HIT score based on random value, dexterity, magical bonus, magic item
					// and warrior experience bonus
					// load an random value in range 2..17
					int hitScore = new Random().nextInt(16) + 2;
					int bonus = Math.max(0, character.getDexterity() - 15);
					hitScore += bonus;
					hitScore += partyHitScoreBonus;
					// look for a magic item in inventory
					// Is it a 'Ring of accuracy' ?
					if (character.getItemCode(3) == 0x02) {
						hitScore += 4;
					}
					if (character.getClassType() == 0x01) {
						hitScore += character.getMazeXp() / 2048;
					}
					
					// load monster AR (ranging $0..$14) <-> (-10..+10)
					int monsterAR = getMem(0xA4C0+monsterID);
					// load dodge score matching this AR (score = $1C for an AR of -10 and score = $08 for an AR of +10)
					int dodgeScore = getMem(0x5300+monsterAR);
					
					// if HIT score > monster dodge score, attack succeeded
					if (hitScore > dodgeScore) {
						// dammage for each weapon :
						// None =  1..4
						// Sword = 1..8
						// Magic sword = 1..16
						// Rune-mace = 1..32
						// Wrathblade = 1..64
						int weapon = character.getItemCode(0);
						// compute damage with weapon mask on random value : 
						int damage = 1 + (new Random().nextInt(256) & getMem(0xA407+weapon));
						// add strength bonus to damage
						damage += Math.max(0, character.getStrengh() - 15);
						// add (tmp experience / 2048) to damage
						damage += character.getMazeXp() / 2048;
						
						// Output attack text according to amount of damage :
						// damage < 5 : bytes 10,15,0A,17,0C,0E,1C,24,11,12,1C,24,0F,18,0E
						// that matche char "GLANCES HIS FOE"
						// damage < 20 : bytes 1C,15,0A,1C,11,0E,1C,24,11,12,1C,24,0F,18,0E
						// that matche char "SLASHES HIS FOE"
						// damage >= 20 : bytes 1C,1D,1B,12,14,0E,1C,24,16,12,10,11,1D,12,15,22
						// that matche char "STRIKES MIGHTILY"
						int textOffset = 0;
						if (damage >= 5) textOffset = 0x10;
						if (damage >= 20) textOffset = 0x20;
						displayStringAt(0xB780+textOffset);

						System.out.println(damage + " " + textOffset);
						nextRowInMessageWindow();
						
						// output byte 11,12,1D,1C,24,0F,18,1B,24 in message window
						// that matches chars "HITS FOR "
						// output byte 24,19,1D,1C at (22,8) in message window
						// that matches chars " PTS"
						for (int j=0;j<0x0D;j++) {
							outputChar(getMem(0xBC4E+j));
							if (j==8) {
								outputWord(damage);
							}
						}
						
						// Remove HP of the first monster still alive
						for (int m=0;m<monstersHP.length;m++) {
							if (monstersHP[m] > 0) {
								monstersHP[m] = Math.max(0, monstersHP[m]-damage);
								if (monstersHP[m] == 0) {
									deadMonsters++;
									nextRowInMessageWindow();
									// output byte 14,12,15,15,0E,0D,24,18,17,0E in message window
									// that matches chars "KILLED ONE"
									displayString(0xBC5B, 0x0A);
								}
								break;
							}
						}
						
						fightInProgress = count > deadMonsters;
//						longDelay();
					}
					else {
						// else character missed
						displayString(0xBC65+0x03, 0x09-0x03);
					}
					
					if (numberOfTurns > 0) {
//						character.setNumberOfStrikes(numberOfTurns-1);
						numberOfTurns--;
					}
					else {
						fightingCharacterIndex++;
					}
				}
				else {
					// Cast a spell
					// Character has casted a magic spell
					// Ouput screen codes 0C,0A,1C,1D,1C,24,0A,24,1C,19,0E,15,15,26,26,26 (22,7)
					// that matches chars "CAST A SPELL..."
					displayString(0xBC3E, 0x10);
					
					switch (spellNumber) {
						// Fireball
						case 1:
						case 5:
						case 9:
						case 17:
							deadMonsters = castAttackSpell(spellNumber, monsterID, monstersHP, deadMonsters);
							break;
						case 2:
							// Spell 2 : SHIELD 
							// This lowers the party's AR by 2 for the duration of the battle. This and all similar spells has a cumulative effect.
							magicARReduction = 0x02;
							break;
						case 6:
							// Spell 6 : PROTECT 
							// This spell will drop the AR of the party by 4 for the duration of the battle. 
							magicARReduction = 0x04;
							break;
						case 10:
							// Spell 10	: GUARDIAN 
							// This spell drops the party AR by 6 for the duration of the battle.
							magicARReduction = 0x06;
							break;
						case 13:
							// Spell 13 : ACCURACY 
							// This spell will improve the chances of all the characters scoring a hit in combat by approximately 25%. 
							partyHitScoreBonus = 0x04;
							break;
						default:
							throw new IllegalStateException();
					}
					
					fightInProgress = deadMonsters < count;
					fightingCharacterIndex++;
				}
				
				longDelay();
			}
			
			if (!fightInProgress) {
				processLoot(monsterID, count);
				return;
			}
		}
	}

	/**
	 * Cast spell 1 : Fireball
	 * This spell sends out a ball of flames, which does 1-32 points
	 * of damage to 1 monster..
	 * 
	 * Cast spell 5 : Flame blast
	 * This spell will send out a firey arc, burning all surviving 
	 * monsters for 1-16 points of damage.
	 * 
	 * Cast spell 9 : Mind fist
	 * This spell deals all surviving monsters a crushing blow. and 
	 * does 1-32 points of damage to each.
	 * 
	 * Cast spell 17 : Flame fury
	 * This spell causes an incendiary explosion to occur among your foes,
	 * doing 1-64 points of damage to each.
	 * 
	 * @param monsterID
	 * @param monstersHP 
	 * @param count
	 * @return 
	 */
	private int castAttackSpell(int spellNumber, int monsterID, int[] monstersHP, int deadMonsters) {
		int mask = getMem(0xA3F5+spellNumber);
		for (int i=0;i<monstersHP.length;i++) {
			int spellDamage = 2 * m_level + 1 + (new Random().nextInt(256) & mask);
			if (monstersHP[i] > 0) {
				monstersHP[i] = Math.max(0, monstersHP[i] - spellDamage);
				nextRowInMessageWindow();
				if (monstersHP[i] == 0) {
					// Current monster killed
					// output byte 14,12,15,15,0E,0D,24,18,17,0E at (22,8)
					// that matches chars "KILLED ONE"
					displayString(0xBC5B, 0x0A);
					deadMonsters++;
					if (deadMonsters == monstersHP.length) {
						return deadMonsters;
					}
				else {
					// output byte 11,12,1D,1C,24,0F,18,1B,24 at (22,8)
					// that matches chars "HITS FOR "
				}
					// output byte 24,19,1D,1C on same row
					// that matches chars " PTS"
					for (int j=0;j<0x0D;j++) {
						outputChar(getMem(0xBC4E+j));
						if (j==8) {
							outputWord(spellDamage);
						}
					}
				}
				delay();
				if (spellNumber == 1) {
					break;
				}
			}
		}
		return deadMonsters;
	}
	
	private void processLoot(int monsterID, int count) {
//		longDelay();
		resetMessageWindowAndCursor();

		// Compute gold gained
		// Display bytes 10,18,15,0D,2A,24 that matches string "GOLD: " at in message window
		int offset = 0x38;
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		
		byte[] triggers = getGame().getTriggers(m_level);
		// Modify trigger of current square : put $FF in north coordinate to deactivate it
		triggers[m_lastTriggerIndex << 1] = (byte) 0xff;
		
		int goldGained = getMem(0xA498+monsterID) << 1;
		outputWord(goldGained);
		
		// Display bytes 0E,21,19,0E,1B,12,0E,17,0C,0E,2A,24 that matches string "EXPERIENCE: " at in message window	
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		
		// Balrog ?
		int xpGained = 0;
		if (monsterID == 0x27) {
			xpGained = 0x2008; // 8200 XP
		}
		else {
			// Compute experience gain for this monster
			xpGained = getMem(0xA470+monsterID); // attack bonus
			xpGained <<= 4; // multiply by 16
			xpGained *= (count+1);
			
			if (getGame().getCharacter(1).isValid()) {
				xpGained >>= 1;
			}
			if (getGame().getCharacter(2).isValid()) {
				xpGained >>= 1;
			}
			outputWord(xpGained);
		}
		
		if (monsterID == 0x27) {
			// If the balrog just died, 
			// Display bytes 0C,18,17,10,1B,0A,1D,1E,15,0A,1D,12,18,17,1C,25,0B,1E,1D,24,1D,11,0E,24,0B
			// 0A,1D,1D,15,0E,24,24,10,18,0E,1C,24,18,17,26,24,1C,0E,0E,14,24,24,24,11,12,1C,24,0A,15,15
			// 12,0E,1C,24,12,17,24,24,24,0A,0D,1F,0E,17,1D,1E,1B,0E,24,02,2A,24,24,24,24,1C,11,0A,0D,18,20
			// 24,1C,17,0A,1B,0E,24,24,24,24
			// that matches string "CONGRATULATIONS,BUT THE BATTLE  GOES ON. SEEK   HIS ALLIES IN   ADVENTURE 2:    SHADOW SNARE    " 
			// in message window	
			next2RowsInMessageWindow();
			offset = 0;
			byte c = getMem(0xB480+offset);
			while (c != 0) {
				outputChar(c);
				if (m_charOutputCol == 0x26) {
					nextRowInMessageWindow();
				}
				offset++;
				c = getMem(0xB480+offset);
			}
		}
		
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.isValid()) {
				character.setGold(checkOverflow16bits(character.getGold()+goldGained));
				character.setMazeXp(checkOverflow16bits(character.getMazeXp()+xpGained));
			}
		}
	}

	private int checkOverflow16bits(int v) {
		if (v > 0x0FFFF) {
			v = 0x0FF00 | (v & 0x0FF);
		}
		return v;
	}

	public boolean checkInfightEscape(int monsterID, int count) {
		if (m_infightEscapeCounter == 0) {
			int readKeyboardAsPETSCIINoBlocking = readKeyboardAsPETSCIINoBlocking();
			if (readKeyboardAsPETSCIINoBlocking == 0x45) {
				m_infightEscapeCounter = 6;
				boolean doMonsterEngage = doMonsterEngage(monsterID, count * 2);
				return !doMonsterEngage;
			}
		}
		else {
			m_infightEscapeCounter--;
		}
		return false;
	}
	
	public void longDelay() {
		for (int i=0;i<5;i++) {
			delay();
		}
	}

	public boolean doMonsterEngage(int monsterID, int runningAwayMonsterBonus) {
		// Load Dexterity of first character
		int dexterity = 0;
		for (int i=0;i<3;i++) {
			if (getGame().getCharacter(i).isValid()) {
				dexterity += getGame().getCharacter(1).getDexterity();
			}
		}
		
		// if monster attack bonus > sum dext, monster engage
		int monsterAttackBonus = getMem(0xA470+monsterID);
		if (monsterAttackBonus > dexterity) {
			return true;
		}
		
		// if attack bonus + level > sum dext, monster engage
		if (monsterAttackBonus + m_level > dexterity) {
			return true;
		}
		
		// if attack bonus + level + 'other bonus ?' > sum dext, monster engage
		if (monsterAttackBonus + m_level + runningAwayMonsterBonus > dexterity) {
			return true;
		}
		
		// get a random value in range 0..63
		// is (sum dext - monster attack bonus - level - 'other bonus') > random (0..63) ?
		// if true, party engages
		// else monster engage
		if (dexterity - monsterAttackBonus - m_level - runningAwayMonsterBonus > new Random().nextInt(64)) {
			return false;
		}
		return true;
	}
	
	public void toggleLogoTwice() {
		useWhiteLogoOnRedBackground();
		delay();
		useRedLogoOnWhiteBackground();
		delay();
	}

	private void hurtWall() {
		drawHitStar();
		playHitSound();
		clearStarArea();
	}

	/**
	 * $884F
	 */
	private void drawHitStar() {
		for (int i=0;i<8;i++) {
			int startY = getMem(0xA56D+i);
			int startX = getMem(0xA575+i);
			int endY = getMem(0xA57D+i);
			int endX = getMem(0xA585+i);
			draw3DViewLine(startX, startY, endX, endY);
		}
	}

	/**
	 * $8810
	 */
	private void playHitSound() {
		// WAIT 196000 cycles (TODO play sound)
		delayCycles(19600*5);
	}

	/**
	 * $8870
	 * Clear a region of 3D covering the hurting star :
	 * Screen blocks (8,8)->(11,11) (32x32 pixels)
	 */
	private void clearStarArea() {
		VicIIDisplay vicII = getGame().getVicII();
		for (int y=8;y<12;y++) {
			for (int x=8;x<12;x++) {
				int startAddress = y * 40 * 8 + x * 8;
				for (int i=0;i<8;i++) {
					vicII.setBitmapValue(startAddress+i, (byte)0);
				}
			}
		}
		
	}
	private WallType getFrontWallType() {
		return m_facingWalls[0][2];
	}

	private void clearBitmapAndInitScreenColor() {
		VicIIDisplay vicII = getGame().getVicII();
		for (int i=0;i<0x2000;i++) {
			vicII.setBitmapValue(i, (byte)0);
		}
		// Fill memory $0400-$0800 with $B1
		// En mode bitmap, la Screen RAM sert √† d√©finir les couleurs.
		// The Background Pixel Color is defined by Bits#0 - Bit#3 of the corresponding Byte in Screen RAM.  
		// The Foreground Pixel Color is defined by Bits#4 - Bits#7 - again from the corresponding Byte in Screen RAM. 
		// Remplissage de la m√©moire $0400 -> $0800 avec $B1 (gris fonc√© sur fond blanc)
		for (int i=0;i<8192;i++) {
			vicII.setCharAt(i, (byte)(0xB1));
		}
		
		// Modifie de la couleur de fond de la zone bitmap correspondant
		// a la vue 3D du labyrinthe
		for (int y=0;y<20;y++) {
			for (int x=0;x<20;x++) {
				vicII.setCharAt(y*40+x, (byte)(0xBF));
			}
		}
		
		useRedLogoOnWhiteBackground();
		
		// Modify background color of messages area on the right (18x15 blocks)
		// Use a white foreground on grey background
		for (int y=5;y<5+15;y++) {
			for (int x=21;x<21+18;x++) {
				vicII.setCharAt(y*40+x, (byte)(0x1C)); //  WHITE foreground on GREY background
			}
		}
	}

	/**
	 * Modifie les couleurs du logo MAZE MASTER en jeu
	 * RED foreground on WHITE background
	 */
	private void useRedLogoOnWhiteBackground() {
		VicIIDisplay vicII = getGame().getVicII();
		for (int y=0;y<4;y++) {
			for (int x=20;x<40;x++) {
				vicII.setCharAt(y*40+x, (byte)(0x21)); //  WHITE foreground on GREY background
			}
		}
	}
	
	/**
	 * Modifie les couleurs du logo MAZE MASTER en jeu
	 * WHITE foreground on RED background
	 */
	private void useWhiteLogoOnRedBackground() {
		VicIIDisplay vicII = getGame().getVicII();
		for (int y=0;y<4;y++) {
			for (int x=20;x<40;x++) {
				vicII.setCharAt(y*40+x, (byte)(0x12)); //  WHITE foreground on GREY background
			}
		}
	}

	/**
	 * Clear 3D view and draw border
	 */
	private void clear3DViewAndDrawBorder() {
		// Clear bitmap memory of 3D view area
		clear3DView();
		draw3DViewLine(0, 0, 159, 0);
		draw3DViewLine(159, 0, 159, 159);
		draw3DViewLine(159, 159, 0, 159);
		draw3DViewLine(0, 159, 0, 0);
	}

	/**
	 * Clear bitmap memory of 3D view area
	 */
	private void clear3DView() {
		VicIIDisplay vicII = getGame().getVicII();
		for (int y=0;y<20;y++) {
			for (int x=0;x<20;x++) {
				int startAddress = y * 40 * 8 + x * 8;
				for (int i=0;i<8;i++) {
					vicII.setBitmapValue(startAddress+i, (byte)0);
				}
			}
		}
	}
	
	public void drawPixel(int x, int y) {
		VicIIDisplay vicII = getGame().getVicII();
		int blockX = x >> 3;
		int blockY = y >> 3;
		int startAddress = blockY * 40 * 8 + blockX * 8;
		startAddress += y & 0x07;
		int bit = 7 - (x & 0x07);
		byte b = vicII.getBitmapContent()[startAddress];
		b = (byte) (b | (1 << bit));
		vicII.setBitmapValue(startAddress, b);
	}

	public void draw3DViewLine(int fromX, int fromY, int toX, int toY) {
		int x = fromX;
		int y = fromY;
		
		boolean stop = false;
		while (!stop) {
			drawPixel(x, y);
			stop = x == toX && y == toY;
			if (x != toX) { x += (x < toX) ? 1 : -1; }
			if (y != toY) { y += (y < toY) ? 1 : -1; }
		}
	}

	/**
	 * 0x93A2
	 * Draw characters footer in game view
	 * Draw maze master logo in top of message box
	 * Initialize sprites 0-3
	 */
	private void initMazeViewFooterLogoAndSprites() {
		// Display footer of game view with characters attributes
		// Ouput screen codes 0C,11,0A,1B,0A,0C,1D,0E,1B,24,17,0A,16,0E,24,24,24,0A,1B,16,24,0C,18,17,24
		// 0C,17,0D,24,0C,15,0A,1C,1C at (3,21)
		// that matches chars "CHARACTER NAME   ARM CON CND CLASS"	
		m_charOutputCol = 3;
		m_charOutputRow = 21;
		
		displayString(0xA58D, 0x22);
		displayStatsLines();
		
		// Display maze master logo in top of message window
		m_charOutputCol = 22;
		m_charOutputRow = 0;
		outputChar((byte)0x2C);
		for (int i=0;i<14;i++) {
			outputChar((byte)0x29);
		}
		outputChar((byte)0x2D);
		nextRowInMessageWindow();
		// display the upper part '|   MAZE   |' of maze master logo
		displayString(0xA5B0, 0x10);
		nextRowInMessageWindow();
		// display the lower part '|  MASTER  |' of maze master logo
		displayString(0xA5C0, 0x10);
		nextRowInMessageWindow();
		outputChar((byte)0x2E);
		for (int i=0;i<14;i++) {
			outputChar((byte)0x29);
		}
		outputChar((byte)0x2F);
		
		// Init SPRITES
		VicIIDisplay vicII = getGame().getVicII();
		int[] spriteX = new int[] { 56, 104, 56, 104 };
		int[] spriteY = new int[] { 88, 88, 130, 130 };
		for (int i=0;i<4;i++) {
			vicII.getSprite(i).color = 1;
			vicII.getSprite(i).multicolor = true;
			vicII.getSprite(i).doubleWidth = true;
			vicII.getSprite(i).doubleHeight = true;
			vicII.getSprite(i).x = spriteX[i];
			vicII.getSprite(i).y = spriteY[i];

			// Hide all sprites (for now...)
			vicII.getSprite(i).color = 1;
		}
	}

	private void next2RowsInMessageWindow() {
		m_charOutputRow++;
		nextRowInMessageWindow();
	}

	private void nextRowInMessageWindow() {
		m_charOutputRow++;
		m_charOutputCol = 0x16;
	}

	private void displayCharacter(int index) {
		resetMessageWindowAndCursor();
		Character character = getGame().getCharacter(index);
		if (!character.isValid()) {
			return;
		}
		
		for (byte c : character.getNameAsBytes()) {
			outputChar(c);
		}
		
		// Display chars '----------------' on next message window row (22,7)	
		nextRowInMessageWindow();
		for (int i=0;i<0x10;i++) {
			outputChar((byte) 0x29);
		}
		
		// Display string "STRENGTH: " on next message window row (22,8)	
		int offset = 6;
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		outputWord(character.getStrengh());
		
		// Display string "INTELLECT: " on next message window row (22,9)	
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		outputWord(character.getIntellect());
		
		// Display string "INTELLECT: " on next message window row (22,10)	
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		outputWord(character.getDexterity());
		
		// Display string "GOLD: " on next message window row (22,11)
		// Skip constitution
		offset += 0x0F;
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		outputWord(character.getGold());
		
		// Display string "EXPERIENCE: " on next message window row (22,12)	
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		outputWord(character.getMazeXp());
		
		// Display string "ITEMS: " on next message window row (22,13)	
		nextRowInMessageWindow();
		displayString(0xA753+0x4, 0x0A-0x4);
		
		// For each item slot, display a blank line if no item is in slot
		// else display item name	
		for (int i=0;i<4;i++) {
			nextRowInMessageWindow();
			byte itemCode = character.getItemCode(i);
			if (itemCode > 0) {
				int nameOffset = getMem(0xA42C+itemCode+i*4);
				displayStringAt(0xBF00+nameOffset);
			}
		}
		nextRowInMessageWindow();
		
		// Ouput screen codes 1C,19,0E,15,15,24,19,1D,1C,2A,24 at (22,12) 
		// that matches chars "SPELL PTS: "	
		displayString(0xA771, 0x0B);
		outputWord(character.getSpellPoints());
		
		m_messageInWindow = true;
	}
	
	private void resetMessageWindowAndCursor() {
		m_charOutputRow = 5;

		while (m_charOutputRow++ <= 19) {
			m_charOutputCol = 21;
			while (m_charOutputCol <= 38) {
				outputChar((byte) 0x24);
			}
		}
		
		m_charOutputRow = 6;
		m_charOutputCol = 22;
	}

	/**
	 * Redraw 3 stats lines of characters in maze view footer
	 * sA0DF
	 */
	private void displayStatsLines() {
		VicIIDisplay vicII = getGame().getVicII();
		
		// Clear stats area
		for (int y=22;y<25;y++) {
			for (int x=0;x<40;x++) {
				int startAddress = y * 40 * 8 + x * 8;
				for (int i=0;i<8;i++) {
					vicII.setBitmapValue(startAddress+i, (byte)0);
				}
			}
		}
		
		m_charOutputRow = 22;
		for (int i=0;i<3;i++) {
			m_charOutputCol = 0;
			Character character = getGame().getCharacter(i);
			if (!character.isValid()) {
				return;
			}
			outputChar((byte)((i+1)));
			outputChar((byte)0x28);
			m_charOutputCol++;
			
			// Display character name
			for (int j=0;j<0x10;j++) {
				outputChar(character.getNameAsBytes()[j]);
			}
			m_charOutputCol++;
			
			// Compute and display character armor (the lower, the better)
			// Once computed, value is store at offset $20 of character data
			byte armorId = character.getItemCode(1);
			int armorRating = getMem(0xA3F0+armorId);
			byte shieldId = character.getItemCode(2);
			armorRating -= getMem(0xA3EB+shieldId);
			if (character.getDexterity() > 15) {
				armorRating -= character.getDexterity()-15;
			}
			armorRating -= m_magicArmor;
			armorRating = Math.max(0, armorRating);
			
			// Does character wear a hawk blason ?
			if (character.getItemCode(3) == 0x04) {
				armorRating = 0;
			}
			
			character.setArmorRating(armorRating);
			byte signChar = (byte)0x4B; // '+'
			if (armorRating < 10) {
				signChar = (byte)0x49; // '-'
				armorRating = 10-armorRating;
			}
			else {
				armorRating = armorRating - 10;
			}
			outputChar(signChar);
			outputWord(armorRating);
			
			// Display character constitution	
			m_charOutputCol = 24;
			outputWord(character.getConstitution());
			
			// Display character condition
			m_charOutputCol = 28;
			outputWord(character.getCondition());
			
			// Display character class (WAR or WIZ)	
			m_charOutputCol = 33;
			int textOffset = character.getClassType() == 1 ? 0 : 7;
			displayString(0xA72C+textOffset, 3);
			
			m_charOutputRow++;
		}
	}

	/**
	 * Deactivate wandering monsters according 
	 * to party level and maze floor number.
	 * 
	 * If the sum of character level is lower than
	 * the level threshold, wandering monsers are
	 * activated :
	 * 
	 * floor 0 : $00 (sum xp < 1024)
	 * floor 1 : $05 (sum xp < 5120)
	 * floor 2 : $0B (sum xp < 11264)
	 * floor 3 : $12 (sum xp < 18432)
	 * floor 4 : $1A (sum xp < 26624)
	 */
	private void initWanderingMonsters() {
		int xp = 0;
		for (int i=0;i<3;i++) {
			Character character = getGame().getCharacter(i);
			if (character.isValid()) {
				xp += character.getXp() >> 10;  // On utilise l'XP et pas l'XP temp ???? (surement un bug dans l'original)
			}
		}
		int threshold = getMem(0xA3E6+m_level) & 0x0FF;
		m_wanderingMonsters = xp <= threshold;
		
	}

	/**
	 * $8DB1
	 * Draw 3D view content
	 */
	private void draw3DView() {
		long startTime = System.nanoTime();

		m_facingWalls = getFacingWalls(m_orientation);
		drawWalls(m_facingWalls);
		
		// Simulate original loop duration
		while (System.nanoTime() - startTime < 200000000) {
			Thread.yield();
		}
	}

	/**
	 * Level 0 is stored at $AA00-$AB90
	 * Level 1 is stored at $AC00-$AD90
	 * Level 2 is stored at $AE00-$AE90
	 * Level 3 is stored at $B000-$B190
	 * Level 4 is stored at $B200-$B390

	 * @param direction
	 * @return
	 */
	private WallType[][] getFacingWalls(int direction) {
		// Stores for each depth[0,4] an array of 5 wall types : 
		// WallType on the [left, right, front, left square front, right square front] 
		WallType[][] facingWalls = new WallType[5][];
		int squareX = m_xPos;
		int squareY = m_yPos;

		switch (direction) {
			case 0: // NORTH
				for (int i=0;i<5;i++) {
					facingWalls[i] = getWallsFacingNorth(squareX, squareY);
					squareY = (squareY + 1) % 20;
				}
				break;
			case 1: // EAST
				for (int i=0;i<5;i++) {
					facingWalls[i] = getWallsFacingEast(squareX, squareY);
					squareX = (squareX + 1) % 20;
				}
				break;
			case 2: // SOUTH
				for (int i=0;i<5;i++) {
					facingWalls[i] = getWallsFacingSouth(squareX, squareY);
					squareY = (squareY - 1 + 20) % 20;
				}
				break;
			case 3: // WEST
				for (int i=0;i<5;i++) {
					facingWalls[i] = getWallsFacingWest(squareX, squareY);
					squareX = (squareX - 1 + 20) % 20;
				}
				break;
			default:
				throw new IllegalStateException();
		}
		
		return facingWalls;
	}
	
	private void drawWalls(WallType[][] facingWalls) {

		clear3DViewAndDrawBorder();
		
		for (int depth=0;depth<5;depth++) {
			
			drawLeftWalls(facingWalls, depth);
			drawRightWalls(facingWalls, depth);
			drawFrontWalls(facingWalls, depth);
			
			// Is there a wall in front of us at current depth ?
			if (facingWalls[depth][2] != WallType.NONE) {
				// if true, no more rendering is required, stop drawing and return
				return;
			}
			
			// At depth 3, check light presence
			if (depth == 2 && m_lightCounter == 0) {
				// if no light, stop rendering at view depth 3
				return;
			}
		}
	}

	private void drawLeftWalls(WallType[][] facingWalls, int depth) {
		// draw left wall
		// Coordinates of left wall vertices for all 5 depth :
		// ($00,$9F) ($00,$00) ($0F,$0F) ($0F,$91)
		// ($0F,$91) ($0F,$0F) ($28,$28) ($28,$78)
		// ($28,$78) ($28,$28) ($3C,$3C) ($3C,$64)
		// ($3C,$64) ($3C,$3C) ($46,$46) ($46,$5A)
		// ($46,$5A) ($46,$46) ($4B,$4B) ($4B,$55)
		int[][] coordinates = new int[][] {
			{0x00, 0x9F, 0x00, 0x00, 0x0F, 0x0F, 0x0F, 0x91},
			{0x0F, 0x91, 0x0F, 0x0F, 0x28, 0x28, 0x28, 0x78},
			{0x28, 0x78, 0x28, 0x28, 0x3C, 0x3C, 0x3C, 0x64},
			{0x3C, 0x64, 0x3C, 0x3C, 0x46, 0x46, 0x46, 0x5A},
			{0x46, 0x5A, 0x46, 0x46, 0x4B, 0x4B, 0x4B, 0x55}
		};
		WallType leftWall = facingWalls[depth][0];
		if (leftWall != WallType.NONE) {
			int[] toDraw = coordinates[depth];
			for (int i=0;i<toDraw.length;i+=2) {
				draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
			}
			// Should draw a door ?
			if (leftWall == WallType.DOOR || (leftWall == WallType.HIDDEN && m_lightCounter > 0)) {
				// draw door on left wall
				// Draw door on left
				// Coordinates of left door vertices for all 5 depth :
				// ($00,$9F) ($00,$0A) ($06,$10) ($06,$9A) 
				// ($14,$8C) ($14,$1C) ($24,$2C) ($24,$7C) 
				// ($2C,$74) ($2C,$32) ($39,$3F) ($39,$67) 
				// ($3F,$61) ($3F,$43) ($44,$48) ($44,$5C) 
				// ($47,$59) ($47,$49) ($4A,$4C) ($4A,$56) 
				int[][] doorCoordinates = new int[][] {
					{0x00, 0x9F, 0x00, 0x0A, 0x06, 0x10, 0x06, 0x9A},
					{0x14, 0x8C, 0x14, 0x1C, 0x24, 0x2C, 0x24, 0x7C},
					{0x2C, 0x74, 0x2C, 0x32, 0x39, 0x3F, 0x39, 0x67},
					{0x3F, 0x61, 0x3F, 0x43, 0x44, 0x48, 0x44, 0x5C},
					{0x47, 0x59, 0x47, 0x49, 0x4A, 0x4C, 0x4A, 0x56}
				};
				toDraw = doorCoordinates[depth];
				for (int i=0;i<toDraw.length;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
				}
			}
		}
		else {
			// Draw wall in front, at our left
			// Warning : it does not draw the part that could be hidden by possible left wall at previous depth
			WallType leftFacingWall = facingWalls[depth][3];
			if (leftFacingWall == WallType.NONE) {
				return;
			}
			// ($00,$0F) ($0F,$0F) ($0F,$91) ($00,$91)
			// ($0F,$28) ($28,$28) ($28,$78) ($0F,$78)
			// ($28,$3C) ($3C,$3C) ($3C,$64) ($28,$64)
			// ($3C,$46) ($46,$46) ($46,$5A) ($3C,$5A)
			// ($46,$4B) ($4B,$4B) ($4B,$55) ($46,$55)
			coordinates = new int[][] {
				{0x00, 0x0F, 0x0F, 0x0F, 0x0F, 0x91, 0x00, 0x91},
				{0x0F, 0x28, 0x28, 0x28, 0x28, 0x78, 0x0F, 0x78},
				{0x28, 0x3C, 0x3C, 0x3C, 0x3C, 0x64, 0x28, 0x64},
				{0x3C, 0x46, 0x46, 0x46, 0x46, 0x5A, 0x3C, 0x5A},
				{0x46, 0x4B, 0x4B, 0x4B, 0x4B, 0x55, 0x46, 0x55}
			};
			int[] toDraw = coordinates[depth];
			for (int i=0;i<toDraw.length-2;i+=2) {
				draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
			}
			// Should draw a door ?
			if (leftFacingWall == WallType.DOOR || (leftFacingWall == WallType.HIDDEN && m_lightCounter > 0)) {
				// Draw door in front, at our left
				// Coordinates of left door vertices for all 5 depth :
				// Warning : it does not draw the part that could be hidden by possible left wall at previous depth
				// ($00,$1E) ($00,$1E) ($00,$91) 
				// ($0F,$32) ($1E,$32) ($1E,$78) 
				// ($28,$42) ($36,$42) ($36,$64) 
				// ($3C,$4A) ($42,$4A) ($42,$5A) 
				// ($46,$4D) ($49,$4D) ($49,$55)
				int[][] doorCoordinates = new int[][] {
					{0x00, 0x1E, 0x00, 0x1E, 0x00, 0x91},
					{0x0F, 0x32, 0x1E, 0x32, 0x1E, 0x78},
					{0x28, 0x42, 0x36, 0x42, 0x36, 0x64},
					{0x3C, 0x4A, 0x42, 0x4A, 0x42, 0x5A},
					{0x46, 0x4D, 0x49, 0x4D, 0x49, 0x55,}
				};
				toDraw = doorCoordinates[depth];
				for (int i=0;i<toDraw.length-2;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
				}
			}
			
			// Draw the hidden part of front wall at left
			if (depth > 0 && facingWalls[depth-1][0] == WallType.NONE && facingWalls[depth-1][3] == WallType.NONE) {
				// Coordinates of front wall at left vertices for all 5 depth :
				// Note : this wall has no part hidden by other wall
				// ($00,$0F) ($00,$00) ($00,$91) ($00,$91) <- may never be use...
				// ($0F,$28) ($00,$28) ($00,$78) ($0F,$78)
				// ($28,$3C) ($14,$3C) ($14,$64) ($28,$64)
				// ($3C,$46) ($32,$46) ($32,$5A) ($3C,$5A)
				// ($46,$4B) ($41,$4B) ($41,$55) ($46,$55)
				coordinates = new int[][] {
					{0x00, 0x0F, 0x00, 0x00, 0x00, 0x91, 0x00, 0x91},
					{0x0F, 0x28, 0x00, 0x28, 0x00, 0x78, 0x0F, 0x78},
					{0x28, 0x3C, 0x14, 0x3C, 0x14, 0x64, 0x28, 0x64},
					{0x3C, 0x46, 0x32, 0x46, 0x32, 0x5A, 0x3C, 0x5A},
					{0x46, 0x4B, 0x41, 0x4B, 0x41, 0x55, 0x46, 0x55}
				};
				toDraw = coordinates[depth];
				for (int i=0;i<toDraw.length-2;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
				}
				// Should draw a door ?
				if (leftFacingWall == WallType.DOOR || (leftFacingWall == WallType.HIDDEN && m_lightCounter > 0)) {
					// Coordinates of front door at left vertices for all 5 depth :
					// Note : this door has no part hidden by other wall
					// ($00,$1E) ($00,$00) ($00,$91) 
					// ($0F,$32) ($00,$32) ($00,$78) 
					// ($28,$42) ($1A,$42) ($1A,$64) 
					// ($3C,$4A) ($36,$4A) ($36,$5A)
					// ($46,$4D) ($43,$4D) ($43,$55)
					int[][] doorCoordinates = new int[][] {
						{0x00, 0x1E, 0x00, 0x00, 0x00, 0x91},
						{0x0F, 0x32, 0x00, 0x32, 0x00, 0x78},
						{0x28, 0x42, 0x1A, 0x42, 0x1A, 0x64},
						{0x3C, 0x4A, 0x36, 0x4A, 0x36, 0x5A},
						{0x46, 0x4D, 0x43, 0x4D, 0x43, 0x55,}
					};
					toDraw = doorCoordinates[depth];
					for (int i=0;i<toDraw.length-2;i+=2) {
						draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
					}
				}
			}
		}
	}
	
	private void drawRightWalls(WallType[][] facingWalls, int depth) {
		// draw right wall
		// Coordinates of left wall vertices for all 5 depth :
		// ($9F,$9F) ($9F,$00) ($91,$0F) ($91,$91)
		// ($91,$91) ($91,$0F) ($78,$28) ($78,$78)
		// ($78,$78) ($78,$28) ($64,$3C) ($64,$64)
		// ($64,$64) ($64,$3C) ($5A,$46) ($5A,$5A)
		// ($5A,$5A) ($5A,$46) ($55,$4B) ($55,$55)
		int[][] coordinates = new int[][] {
			{0x9F, 0x9F, 0x9F, 0x00, 0x91, 0x0F, 0x91, 0x91},
			{0x91, 0x91, 0x91, 0x0F, 0x78, 0x28, 0x78, 0x78},
			{0x78, 0x78, 0x78, 0x28, 0x64, 0x3C, 0x64, 0x64},
			{0x64, 0x64, 0x64, 0x3C, 0x5A, 0x46, 0x5A, 0x5A},
			{0x5A, 0x5A, 0x5A, 0x46, 0x55, 0x4B, 0x55, 0x55}
		};
		WallType rightWall = facingWalls[depth][1];
		if (rightWall != WallType.NONE) {
			int[] toDraw = coordinates[depth];
			for (int i=0;i<toDraw.length;i+=2) {
				draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
			}
			// Should draw a door ?
			if (rightWall == WallType.DOOR || (rightWall == WallType.HIDDEN && m_lightCounter > 0)) {
				// draw door on right wall
				// Draw door on right
				// Coordinates of right door vertices for all 5 depth :
				// ($9F,$9F) ($9F,$0A) ($9A,$10) ($9A,$9A) 
				// ($8C,$8C) ($8C,$1C) ($7C,$2C) ($7C,$7C) 
				// ($74,$74) ($74,$32) ($67,$3F) ($67,$67) 
				// ($61,$61) ($61,$43) ($5C,$48) ($5C,$5C) 
				// ($59,$59) ($59,$49) ($56,$4C) ($56,$56) 
				int[][] doorCoordinates = new int[][] {
					{0x9F, 0x9F, 0x9F, 0x0A, 0x9A, 0x10, 0x9A, 0x9A},
					{0x8C, 0x8C, 0x8C, 0x1C, 0x7C, 0x2C, 0x7C, 0x7C},
					{0x74, 0x74, 0x74, 0x32, 0x67, 0x3F, 0x67, 0x67},
					{0x61, 0x61, 0x61, 0x43, 0x5C, 0x48, 0x5C, 0x5C},
					{0x59, 0x59, 0x59, 0x49, 0x56, 0x4C, 0x56, 0x56}
				};

				toDraw = doorCoordinates[depth];
				for (int i=0;i<toDraw.length;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
				}
			}
		}
		else {
			// Draw wall in front, at our right
			// Warning : it does not draw the part that could be hidden by possible right wall at previous depth
			WallType rightFacingWall = facingWalls[depth][4];
			if (rightFacingWall == WallType.NONE) {
				return;
			}
			// ($9F,$0F) ($91,$0F) ($91,$91) ($9F,$91)
			// ($91,$28) ($78,$28) ($78,$78) ($91,$78)
			// ($78,$3C) ($64,$3C) ($64,$64) ($78,$64)
			// ($64,$46) ($5A,$46) ($5A,$5A) ($64,$5A)
			// ($5A,$4B) ($55,$4B) ($55,$55) ($5A,$55)
			coordinates = new int[][] {
				{0x9F, 0x0F, 0x91, 0x0F, 0x91, 0x91, 0x9F, 0x91},
				{0x91, 0x28, 0x78, 0x28, 0x78, 0x78, 0x91, 0x78},
				{0x78, 0x3C, 0x64, 0x3C, 0x64, 0x64, 0x78, 0x64},
				{0x64, 0x46, 0x5A, 0x46, 0x5A, 0x5A, 0x64, 0x5A},
				{0x5A, 0x4B, 0x55, 0x4B, 0x55, 0x55, 0x5A, 0x55}
			};
			int[] toDraw = coordinates[depth];
			for (int i=0;i<toDraw.length-2;i+=2) {
				draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
			}
			// Should draw a door ?
			if (rightFacingWall == WallType.DOOR || (rightFacingWall == WallType.HIDDEN && m_lightCounter > 0)) {
				// Draw door in front, at our right
				// Coordinates of right door vertices for all 5 depth :
				// Warning : it does not draw the part that could be hidden by possible left wall at previous depth
				// ($9F,$1E) ($9F,$1E) ($9F,$91) 
				// ($91,$32) ($82,$32) ($82,$78) 
				// ($78,$42) ($6A,$42) ($6A,$64) 
				// ($64,$4A) ($5E,$4A) ($5E,$5A) 
				// ($5A,$4D) ($57,$4D) ($57,$55) 
				int[][] doorCoordinates = new int[][] {
					{0x9F, 0x1E, 0x9F, 0x1E, 0x9F, 0x91},
					{0x91, 0x32, 0x82, 0x32, 0x82, 0x78},
					{0x78, 0x42, 0x6A, 0x42, 0x6A, 0x64},
					{0x64, 0x4A, 0x5E, 0x4A, 0x5E, 0x5A},
					{0x5A, 0x4D, 0x57, 0x4D, 0x57, 0x55}
				};

				toDraw = doorCoordinates[depth];
				for (int i=0;i<toDraw.length-2;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
				}
			}
			
			// Draw the hidden part of front wall at right
			if (depth > 0 && facingWalls[depth-1][1] == WallType.NONE && facingWalls[depth-1][4] == WallType.NONE) {
				// Coordinates of front wall at left vertices for all 5 depth :
				// Note : this wall has no part hidden by other wall
				// ($9F,$0F) ($00,$00) ($00,$91) ($9F,$91) 
				// ($91,$28) ($9F,$28) ($9F,$78) ($91,$78)
				// ($78,$3C) ($8C,$3C) ($8C,$64) ($78,$64)
				// ($64,$46) ($6E,$46) ($6E,$5A) ($64,$5A)
				// ($5A,$4B) ($5F,$4B) ($5F,$55) ($5A,$55)
				coordinates = new int[][] {
					{0x9F, 0x0F, 0x00, 0x00, 0x00, 0x91, 0x9D, 0x91},
					{0x91, 0x28, 0x9F, 0x28, 0x9F, 0x78, 0x91, 0x78},
					{0x78, 0x3C, 0x8C, 0x3C, 0x8C, 0x64, 0x78, 0x64},
					{0x64, 0x46, 0x6E, 0x46, 0x6E, 0x5A, 0x64, 0x5A},
					{0x5A, 0x4B, 0x5F, 0x4B, 0x5F, 0x55, 0x5A, 0x55}
				};
				toDraw = coordinates[depth];
				for (int i=0;i<toDraw.length-2;i+=2) {
					draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
				}
				// Should draw a door ?
				if (rightFacingWall == WallType.DOOR || (rightFacingWall == WallType.HIDDEN && m_lightCounter > 0)) {
					// Coordinates of front door at left vertices for all 5 depth :
					// Note : this door has no part hidden by other wall
					// ($9F,$1E) ($00,$00) ($00,$91) 
					// ($91,$32) ($9F,$32) ($9F,$78) 
					// ($78,$42) ($86,$42) ($86,$64) 
					// ($64,$4A) ($6A,$4A) ($6A,$5A)
					// ($5A,$4D) ($5D,$4D) ($5D,$55)
					int[][] doorCoordinates = new int[][] {
						{0x9F, 0x1E, 0x00, 0x00, 0x00, 0x91},
						{0x91, 0x32, 0x9F, 0x32, 0x9F, 0x78},
						{0x78, 0x42, 0x86, 0x42, 0x86, 0x64},
						{0x64, 0x4A, 0x6A, 0x4A, 0x6A, 0x5A},
						{0x5A, 0x4D, 0x5D, 0x4D, 0x5D, 0x55,}
					};
					toDraw = doorCoordinates[depth];
					for (int i=0;i<toDraw.length-2;i+=2) {
						draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[i+2], toDraw[i+3]);
					}
				}
			}
		}
	}
	
	private void drawFrontWalls(WallType[][] facingWalls, int depth) {
		
		WallType frontWall = facingWalls[depth][2];
		if (frontWall == WallType.NONE) {
			return;
		}

		// Coordinates of left wall vertices for all 5 depth :
		// ($0F,$0F) ($91,$0F) ($91,$91) ($0F,$91)
		// ($28,$28) ($78,$28) ($78,$78) ($28,$78)
		// ($3C,$3C) ($64,$3C) ($64,$64) ($3C,$64)
		// ($46,$46) ($5A,$46) ($5A,$5A) ($46,$5A)
		// ($4B,$4B) ($55,$4B) ($55,$55) ($4B,$55)
		int[][] coordinates = new int[][] {
			{0x0F, 0x0F, 0x91, 0x0F, 0x91, 0x91, 0x0F, 0x91},
			{0x28, 0x28, 0x78, 0x28, 0x78, 0x78, 0x28, 0x78},
			{0x3C, 0x3C, 0x64, 0x3C, 0x64, 0x64, 0x3C, 0x64},
			{0x46, 0x46, 0x5A, 0x46, 0x5A, 0x5A, 0x46, 0x5A},
			{0x4B, 0x4B, 0x55, 0x4B, 0x55, 0x55, 0x4B, 0x55}
		};

		int[] toDraw = coordinates[depth];
		for (int i=0;i<toDraw.length;i+=2) {
			draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
		}
		// Should draw a door ?
		if (frontWall == WallType.DOOR || (frontWall == WallType.HIDDEN && m_lightCounter > 0)) {
			// draw door on right wall
			// Draw door on right
			// Coordinates of right door vertices for all 5 depth :
			// ($1E,$91) ($1E,$1E) ($82,$1E) ($82,$91) 
			// ($32,$78) ($32,$32) ($6E,$32) ($6E,$78) 
			// ($42,$64) ($42,$42) ($5E,$42) ($5E,$64) 
			// ($4A,$5A) ($4A,$4A) ($56,$4A) ($56,$5A) 
			// ($4D,$55) ($4D,$4D) ($53,$4D) ($53,$55)
			int[][] doorCoordinates = new int[][] {
				{0x1E, 0x91, 0x1E, 0x1E, 0x82, 0x1E, 0x82, 0x91},
				{0x32, 0x78, 0x32, 0x32, 0x6E, 0x32, 0x6E, 0x78},
				{0x42, 0x64, 0x42, 0x42, 0x5E, 0x42, 0x5E, 0x64},
				{0x4A, 0x5A, 0x4A, 0x4A, 0x56, 0x4A, 0x56, 0x5A},
				{0x4D, 0x55, 0x4D, 0x4D, 0x53, 0x4D, 0x53, 0x55}
			};
			toDraw = doorCoordinates[depth];
			for (int i=0;i<toDraw.length;i+=2) {
				draw3DViewLine(toDraw[i], toDraw[i+1], toDraw[(i+2)%8], toDraw[(i+3)%8]);
			}
		}
	}

	private enum WallType {
		NONE,
		WALL,
		DOOR,
		HIDDEN
	}
	
	private WallType[] getWallsFacingNorth(int x, int y) {
		 WallType[] result = new WallType[5];
		 
		// get current pos walls on N,S,E,W
		WallType[] walls = getWalls(m_level, x, y);
		result[0] = walls[3]; // WallType on our left
		result[1] = walls[2]; // WallType on our right
		result[2] = walls[0]; // WallType in front of us
		if (result[0] == WallType.NONE) { // if no wall on our left, get the facing wall of the left square
			int leftSquareX = (x-1+20) % 20;
			WallType[] leftWalls = getWalls(m_level, leftSquareX, y);
			result[3] = leftWalls[0]; // WallType of the left square facing us
		}
		if (result[1] == WallType.NONE) { // if no wall on our right, get the facing wall of the right square
			int rightSquareX = (x+1) % 20;
			WallType[] rightWalls = getWalls(m_level, rightSquareX, y);
			result[4] = rightWalls[0]; // WallType of the left square facing us
		}
		return result;
	}
	
	private WallType[] getWallsFacingEast(int x, int y) {
		WallType[] result = new WallType[5];
		 
		// get current pos walls on N,S,E,W
		WallType[] walls = getWalls(m_level, x, y);
		result[0] = walls[0]; // WallType on our left
		result[1] = walls[1]; // WallType on our right
		result[2] = walls[2]; // WallType in front of us
		if (result[0] == WallType.NONE) { // if no wall on our left, get the facing wall of the left square
			int leftSquareY = (y+1) % 20;
			WallType[] leftWalls = getWalls(m_level, x, leftSquareY);
			result[3] = leftWalls[2]; // WallType of the left square facing us
		}
		if (result[1] == WallType.NONE) { // if no wall on our right, get the facing wall of the right square
			int rightSquareY = (y-1+20) % 20;
			WallType[] rightWalls = getWalls(m_level, x, rightSquareY);
			result[4] = rightWalls[2]; // WallType of the left square facing us
		}
		return result;
	}
	
	private WallType[] getWallsFacingSouth(int x, int y) {
		 WallType[] result = new WallType[5];
		 
		// get current pos walls on N,S,E,W
		WallType[] walls = getWalls(m_level, x, y);
		result[0] = walls[2]; // WallType on our left
		result[1] = walls[3]; // WallType on our right
		result[2] = walls[1]; // WallType in front of us
		if (result[0] == WallType.NONE) { // if no wall on our left, get the facing wall of the left square
			int leftSquareX = (x+1) % 20;
			WallType[] leftWalls = getWalls(m_level, leftSquareX, y);
			result[3] = leftWalls[1]; // WallType of the left square facing us
		}
		if (result[1] == WallType.NONE) { // if no wall on our right, get the facing wall of the right square
			int rightSquareX = (x-1+20) % 20;
			WallType[] rightWalls = getWalls(m_level, rightSquareX, y);
			result[4] = rightWalls[1]; // WallType of the left square facing us
		}
		return result;
	}
	
	private WallType[] getWallsFacingWest(int x, int y) {
		WallType[] result = new WallType[5];
		 
		// get current pos walls on N,S,E,W
		WallType[] walls = getWalls(m_level, x, y);
		result[0] = walls[1]; // WallType on our left
		result[1] = walls[0]; // WallType on our right
		result[2] = walls[3]; // WallType in front of us
		if (result[0] == WallType.NONE) { // if no wall on our left, get the facing wall of the left square
			int leftSquareY = (y-1+20) % 20;
			WallType[] leftWalls = getWalls(m_level, x, leftSquareY);
			result[3] = leftWalls[3]; // WallType of the left square facing us
		}
		if (result[1] == WallType.NONE) { // if no wall on our right, get the facing wall of the right square
			int rightSquareY = (y+1) % 20;
			WallType[] rightWalls = getWalls(m_level, x, rightSquareY);
			result[4] = rightWalls[3]; // WallType of the left square facing us
		}
		return result;
	}
	
	/**
	 * @param level
	 * @param x
	 * @param y
	 * @return the wall type in this order : north, south, east, west
	 */
	private WallType[] getWalls(int level, int x, int y) {
		int address = 0xAA00 + (level * 2 << 8) + y * 20 + x;
		byte squareDefinition = getMem(address);
		WallType northWall = WallType.values()[squareDefinition & 0x03];
		WallType southWall = WallType.values()[(squareDefinition >> 2) & 0x03];
		WallType eastWall = WallType.values()[(squareDefinition >> 4) & 0x03];
		WallType westWall = WallType.values()[(squareDefinition >> 6 ) & 0x03];
		return new WallType[] {
			northWall, southWall, eastWall, westWall
		};
	}

	public void hideSprites() {
		for (int i=0;i<4;i++) {
			getGame().getVicII().getSprite(i).enabled = false;
		}
	}
	
	@Override
	public void exit() {
	}
}
