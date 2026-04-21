package org.pixs.mazemaster.states;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

import org.pixs.JoystickButton;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.Character;
import org.pixs.mazemaster.Direction;
import org.pixs.mazemaster.Game;
import org.pixs.mazemaster.MazeMap;
import org.pixs.mazemaster.Party;
import org.pixs.mazemaster.WallType;

public class MazeState extends GameState {

	private static final Random RANDOM = new Random();

	int m_lightCounter = 0;
	
	private int m_xPos;
	private int m_yPos;

	private Direction m_orientation;
	private int m_level = 0;

	private int m_magicArmor;

	private boolean m_wanderingMonsters;

	private boolean m_messageInWindow;

	private WallType[][] m_facingWalls;

	private boolean m_exitMaze;

	private int m_infightEscapeCounter;

	private int m_lastTriggerIndex;

	// Fight-scoped state. Live only for the duration of startFight and are
	// reset at its entry; pulled to instance fields so the per-phase helpers
	// can mutate them without dragging 3 parameters through every call.
	private int m_fightMagicARReduction;
	private int m_fightPartyHitScoreBonus;
	private int m_fightDeadMonsters;

	public MazeState(Game game) {
		super(game);
	}

	@Override
	public void enter() {
		VicIIDisplay vicII = getGame().getVicII();
		
		m_lightCounter = 0;
		
		// Init characters conditions with constitution
		for (Character character : party()) {
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
		
		vicII.setBorderColor(0x06);
		
		m_xPos = 0;
		m_yPos = 0;
		m_orientation = Direction.NORTH;
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
		
		// Main loop duration on original game is 68 CPU cycles = 69.04 �s = 0.069 ms
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
			
			Set<JoystickButton> pressedButton = getGame().getInputState().getPressedButton();
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
				if (m_wanderingMonsters && ((RANDOM.nextInt(32) & 0x1F) == 0)) {
					encounterRandomMonster();
				}
				else {
					// Execute some code here half the time (light decline and characters healing)
					// (nearly every 9.7 seconds, =9566743 cpu cycles)
					if (timerToggleFlag) {
						// Process light decrease
						if (m_lightCounter > 0 && m_lightCounter != 0xF0) {
							m_lightCounter--;
							if (m_lightCounter == 0) {
								playRingSound();
								draw3DView();
							}
						}
						
						// Heal characters holding an 'Amulet of Healing'
						boolean[] healPerformed = { false };
						party().forEachAlive(c -> {
							if (c.getItemCode(3) == 0x03 && c.getCondition() < c.getConstitution()) {
								c.setCondition(Math.min(c.getConstitution(), c.getCondition() + 1));
								healPerformed[0] = true;
							}
						});
						if (healPerformed[0]) {
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
		// j94C5: random monster ID in 0..15; processMonsterEncounter adds level*6 itself.
		processMonsterEncounter(RANDOM.nextInt(16));
	}

	/**
	 * $897F
	 * Part has come back to base camp
	 */
	private void returnToBaseCamp() {
		party().forEachAlive(character -> {
			// j897F: levelsGained = (mazeXp >> 10) - (xp >> 10), then persist mazeXp → xp
			int levelsGained = (character.getMazeXp() >> 10) - (character.getXp() >> 10);
			character.setXP(character.getMazeXp());
			// Apply the level-up bonuses once per level gained (ASM DEX/BNE loop at b89B9)
			for (int l = 0; l < levelsGained; l++) {
				// Constitution += 1..4, clamped to 255
				character.setConstitution(Math.min(255, character.getConstitution() + RANDOM.nextInt(4) + 1));
				// One random attribute among STR/INT/DEX gets +1..2, capped at 18
				int attributeBonus = RANDOM.nextInt(2) + 1;
				switch (RANDOM.nextInt(3)) {
					case 0:
						character.setStrength(Math.min(18, character.getStrength() + attributeBonus));
						break;
					case 1:
						character.setIntellect(Math.min(18, character.getIntellect() + attributeBonus));
						break;
					case 2:
						character.setDexterity(Math.min(18, character.getDexterity() + attributeBonus));
						break;
				}
			}
		});
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
		
		// if combat spell, display warning message in window and return to main loop
		if (rom().isCombatSpell(spellNumber)) {
			m_charOutputRow++;
			m_charOutputCol = 22;
			m_messageInWindow = true;
			
			// Ouput screen codes 0F,18,1B,24,0C,18,16,0B,0A,1D,24,18,17,15,22 at (22,10)
			// that matches chars "FOR COMBAT ONLY"
			displayString(0xA7F0, 0x0F);
			return;
		}
		
		// load required spell points for this spell
		int requiredSpellPoints = rom().spellPointsCost(spellNumber);
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
	 * This spell will heal 1-32 points of the spellcaster�s own CND.
	 * @param selectedCharacter 
	 */
	private void castHeal(Character selectedCharacter) {
		// j8BA2: (rand & $1F) + 1 → 1..32
		int heal = RANDOM.nextInt(32) + 1;
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
		int offset = getMemU(0xA424+m_orientation.ordinal());
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
		// j8BCD: (rand & $0F) + 1 → 1..16
		healParty(RANDOM.nextInt(16)+1);
	}

	/**
	 * Cast spell 11 : regenerate
	 * This spell is similar to spell 7, except that it heals 1-32 points.
	 */
	private void castRegenerate() {
		// j8BD5: (rand & $1F) + 1 → 1..32
		healParty(RANDOM.nextInt(32)+1);
	}
	
	private void healParty(int healValue) {
		party().forEachAlive(c ->
			c.setCondition(Math.min(c.getCondition() + healValue, c.getConstitution())));
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

		// ASM j8C73: re-query the value until it yields a valid target level.
		int newLevel = m_level;
		while (true) {
			int value = selectValueInRange20();
			if (value == 20) {
				// User selected 0 — stay on current level.
				break;
			}
			int candidate = m_level + value - 20;
			if (value > 20) {
				// Going down.
				if (candidate < 5) {
					// Bounce off the bottom: level 4 wraps to level 0 (ASM b8C8C).
					newLevel = (candidate == 4) ? 0 : candidate;
					break;
				}
			}
			else {
				// Going up.
				if (candidate >= 0) {
					newLevel = candidate;
					break;
				}
			}
			waitForJoystickRelease();
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
		while (!getGame().getInputState().getPressedButton().isEmpty()) {
			try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
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
			
			Set<JoystickButton> pressedButton = getGame().getInputState().getPressedButton();
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
		party().forEachAlive(c -> c.setCondition(c.getConstitution()));
		displayStatsLines();
	}

	/**
	 * Convert a sequence of screen-code digits read from the keyboard into its
	 * numeric value. Mirrors ASM s9F9D:
	 * <ul>
	 *   <li>a screen-code $24 (space) ends the number (we've hit an unfilled
	 *       slot in the input buffer),</li>
	 *   <li>any non-digit code (screen codes &gt; 9) makes the whole string
	 *       invalid and the routine returns 0.</li>
	 * </ul>
	 */
	public static int convertToWord(byte[] chars) {
		int result = 0;
		for (byte v : chars) {
			if (v == 0x24) return result;
			if (v < 0 || v > 9) return 0;
			result = result * 10 + v;
		}
		return result;
	}

	public void delay() {
		delayCycles(352788);
	}
	
	public void delayCycles(int cyclesCount) {
		long nanos = (long) (cyclesCount * 1014.97288d);
		java.util.concurrent.locks.LockSupport.parkNanos(nanos);
	}

	/**
	 * Entre en mode PAUSE
	 * 
	 * Fait clignoter le logo MAZE MASTER
	 * et boucle tant qu'aucune touche n'est press�e
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
		m_orientation = m_orientation.turnLeft();
		draw3DView();
	}

	private void turnRight() {
		m_orientation = m_orientation.turnRight();
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
		
		m_xPos = (m_xPos + m_orientation.dx + 20) % 20;
		m_yPos = (m_yPos + m_orientation.dy + 20) % 20;
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
		int messageAddress = getMemU(0xA43D) | (getMemU(0xB4E1) << 8);
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
		int messageAddress = getMemU(0xA43D+1) | (getMemU(0xB4E1+1) << 8);
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
		int messageAddress = getMemU(0xA43D+2+(m_level<<1)) | (getMemU(0xB4E1+2+(m_level<<1)) << 8);
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
		m_level++;
		draw3DView();
		initWanderingMonsters();
	}

	private void processMonsterEncounter(int monsterIndex) {
		// add 6*level to monster ID -> final monster ID in range ($0..$27=Balrog)
		monsterIndex = m_level * 6 + monsterIndex;

		// If monster ID is 39 ($27), ensure that party position is (3,19) (BALROG location)
		// If party is anywhere else, load monster ID 25 ($19) instead...
		// ASM b94D7: must match BOTH coords; any mismatch downgrades to $19
		if (monsterIndex == 0x27 && (m_xPos != 3 || m_yPos != 19)) {
			monsterIndex = 0x19;
		}
		
		// Load monster sprite descriptors
		int offset = monsterIndex * 4;
		int multiColor0 = getMemU(0xBE60+offset);
		int multiColor1 = getMemU(0xBE61+offset);
		int spriteBottomAddress = getMemU(0xBE63+offset) << 8;
		int spriteTopAddress = getMemU(0xBE62+offset) << 8;
		if (spriteTopAddress < 0x0B0) {
			spriteTopAddress = ((spriteTopAddress + 0x0B0) & 0x0FF00) | 0x080;
		}

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
		
		int nameAddr = rom().monsterNameAddress(monsterIndex);
		displayStringAt(nameAddr);
		// And also at bottom of 3D view
		m_charOutputCol = 5;
		m_charOutputRow = 18;
		displayStringAt(nameAddr);
		
		// Compute and display number of fighting monsters
		m_charOutputRow = 6;
		// Get a random value between 1 & 4
		int count = RANDOM.nextInt(4) + 1;
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
		m_fightMagicARReduction = 0;
		m_fightPartyHitScoreBonus = 0;
		m_fightDeadMonsters = 0;
		m_infightEscapeCounter = 0;

		int[] monstersHP = new int[count];
		// Monster HP table holds unsigned bytes; the Balrog entry is $FF and would
		// flip to -1 without the mask, making him spawn already dead.
		Arrays.fill(monstersHP, rom().monsterHP(monsterID));

		// First round: doMonsterEngage decides who starts. Subsequent rounds
		// always give monsters a turn (matches ASM flow at j9668 and j992F).
		boolean monstersEngage = doMonsterEngage(monsterID, 0);

		while (true) {
			if (monstersEngage && !monstersAttackPhase(monsterID, count, monstersHP)) {
				return;
			}
			monstersEngage = true;

			askPartyActions();

			if (!partyAttackPhase(monsterID, count, monstersHP)) {
				return;
			}

			if (m_fightDeadMonsters >= count) {
				processLoot(monsterID, count);
				return;
			}
		}
	}

	/**
	 * All living monsters attack a random living character. Returns {@code true}
	 * to continue the fight, {@code false} if the fight must end (party dead → sets
	 * {@link #m_exitMaze}, or party successfully escaped mid-round).
	 */
	private boolean monstersAttackPhase(int monsterID, int count, int[] monstersHP) {
		resetMessageWindowAndCursor();
		delayInMillis(50); // SMa : separate turns clearly, display is too fast in Java

		// "MONSTERS ATTACK" at (22,6)
		displayString(0xA9A4, 0x0F);
		longDelay();
		resetMessageWindowAndCursor();

		for (int i = 0; i < count; i++) {
			if (monstersHP[i] <= 0) continue;

			// Pick a random target (ASM b995A loads D41B, AND #$03, remaps 3 → 0).
			int targetIndex = RANDOM.nextInt() & 0x03;
			if (targetIndex == 0x03) targetIndex = 0;
			Character target = getGame().getCharacter(targetIndex);
			if (!target.isValid()) {
				targetIndex = 0;
				target = getGame().getCharacter(targetIndex);
			}

			int armorRating = Math.max(0, target.getArmorRating() - m_fightMagicARReduction);
			int dodgeScore = rom().dodgeScore(armorRating);
			int attackBonus = rom().monsterAttackBonus(monsterID);
			int attackScore = RANDOM.nextInt(16) + 5 + attackBonus;

			int damage = 0;
			if (attackScore >= dodgeScore) {
				// Base 1..8 plus N further 1..8 rolls, where N = monster attack bonus.
				damage = RANDOM.nextInt(8) + 1;
				for (int j = 0; j < attackBonus; j++) {
					damage += RANDOM.nextInt(8) + 1;
				}
			}

			display(target.getNameAsBytes());
			nextRowInMessageWindow();

			if (damage == 0) {
				// "DODGES THE BLOW"
				displayString(0xB7B1, 0x0F);
			} else {
				// Hit text varies with damage: "IS SCRAPED" / "IS SLASHED" / "IS BATTERED".
				int messageOffset = 0x0F;
				if (damage >= 7) messageOffset = 0x1A;
				if (damage >= 25) messageOffset = 0x25;
				displayStringAt(0xB7B1 + messageOffset);
				nextRowInMessageWindow();

				// "TAKES <damage> DAMAGE"
				for (int j = 0; j < 0x0D; j++) {
					outputChar(getMem(0xBC6E + j));
					if (j == 5) outputWord(damage);
				}

				target.setCondition(Math.max(0, target.getCondition() - damage));
				if (target.getCondition() == 0) {
					nextRowInMessageWindow();
					// "AND IS KILLED"
					displayString(0xBC7B, 0x0D);
					getGame().deleteCharacter(targetIndex);
				}
			}

			displayStatsLines();
			longDelay();
			resetMessageWindowAndCursor();
			delayInMillis(50);

			// Party dead: exit the maze entirely.
			if (party().allDead()) {
				m_exitMaze = true;
				return false;
			}

			// Mid-round escape attempt ('E' key) — not on the last monster.
			if (i < count - 1 && checkInfightEscape(monsterID, count)) {
				// "YOU GO AWAY..."
				displayString(0xBC20, 0x0F);
				longDelay();
				return false;
			}
		}
		return true;
	}

	/**
	 * Prompts every living character in turn for weapon vs spell, and stores
	 * their choice on the Character. Mirrors ASM b9668..j96FF.
	 */
	private void askPartyActions() {
		for (Character character : party()) {
			resetMessageWindowAndCursor();
			delayInMillis(50);

			// Warriors get (mazeXp / 8192) extra strikes; wizards get 0.
			character.setNumberOfStrikes(character.getClassType() == 1
					? character.getMazeXp() / 8192
					: 0);

			display(character.getNameAsBytes());
			nextRowInMessageWindow();

			// "(W)EAP OR (S)PEL"
			displayString(0xBC10, 0x10);

			int key = readKeyboardAsPETSCII();
			while (key != 0x53 && key != 0x57) {
				key = readKeyboardAsPETSCII();
			}

			if (key == 0x57) { // 'W'
				character.setSpellNumber(0);
				continue;
			}

			next2RowsInMessageWindow();
			// "SPELL NUMBER: "
			displayString(0xBC88, 0x0E);
			int spellNumber = convertToWord(readChars(3)) & 0x0FF;
			if (spellNumber > 18) spellNumber = 0;

			// A3C0[n] == 0 marks combat spells. Non-combat or underfunded → fall back to weapon.
			if (rom().isCombatSpell(spellNumber)) {
				int requiredSpellPoints = rom().spellPointsCost(spellNumber);
				if (requiredSpellPoints > character.getSpellPoints()) {
					spellNumber = 0;
				} else {
					character.setSpellPoints(character.getSpellPoints() - requiredSpellPoints);
				}
			} else {
				spellNumber = 0;
			}
			character.setSpellNumber(spellNumber);
		}
	}

	/**
	 * Each character executes the action queued by {@link #askPartyActions()}.
	 * Warriors may attack multiple times (strikes counter on the Character).
	 * Returns {@code true} to continue the fight, {@code false} if the party
	 * successfully escaped via the 'E' key.
	 */
	private boolean partyAttackPhase(int monsterID, int count, int[] monstersHP) {
		int fightingCharacterIndex = 0;
		while (fightingCharacterIndex < Party.MAX_SIZE
				&& party().at(fightingCharacterIndex).isValid()
				&& m_fightDeadMonsters < count) {
			resetMessageWindowAndCursor();
			delayInMillis(50);

			if (checkInfightEscape(monsterID, count)) {
				// "YOU GO AWAY..."
				displayString(0xBC20, 0x0F);
				longDelay();
				return false;
			}

			Character character = party().at(fightingCharacterIndex);
			display(character.getNameAsBytes());
			nextRowInMessageWindow();
			int spellNumber = character.getSpellNumber();
			int numberOfTurns = character.getNumberOfStrikes();

			if (spellNumber == 0) {
				characterWeaponAttack(character, monsterID, monstersHP);
				// Warrior strikes persist on the character (ASM j990B).
				if (numberOfTurns > 0) {
					character.setNumberOfStrikes(numberOfTurns - 1);
				} else {
					fightingCharacterIndex++;
				}
			} else {
				// "CAST A SPELL..."
				displayString(0xBC3E, 0x10);
				applyCombatSpell(spellNumber, monsterID, monstersHP);
				fightingCharacterIndex++;
			}

			longDelay();
		}
		return true;
	}

	/**
	 * One weapon swing from a single character against the first living monster.
	 * Updates {@code monstersHP} and {@link #m_fightDeadMonsters}.
	 */
	private void characterWeaponAttack(Character character, int monsterID, int[] monstersHP) {
		// HIT score = rand(2..17) + dex bonus + party bonus + item bonus + warrior XP bonus
		int hitScore = RANDOM.nextInt(16) + 2;
		hitScore += Math.max(0, character.getDexterity() - 15);
		hitScore += m_fightPartyHitScoreBonus;
		if (character.getItemCode(3) == 0x02) hitScore += 4;           // Ring of accuracy
		if (character.getClassType() == 0x01) {
			hitScore += character.getMazeXp() / 2048;                   // warrior only
		}

		int dodgeScore = rom().dodgeScore(rom().monsterAR(monsterID));

		if (hitScore <= dodgeScore) {
			// "MISSED"
			displayString(0xBC65 + 0x03, 0x09 - 0x03);
			return;
		}

		// Weapon damage mask at A407: None=$03, Sword=$07, MagicSword=$0F, RuneMace=$1F, Wrathblade=$3F
		int weapon = character.getItemCode(0);
		int damage = 1 + (RANDOM.nextInt(256) & rom().weaponDamageMask(weapon));
		damage += Math.max(0, character.getStrength() - 15);
		if (character.getClassType() == 0x01) {
			damage += character.getMazeXp() / 2048;
		}

		// "GLANCES HIS FOE" / "SLASHES HIS FOE" / "STRIKES MIGHTILY" depending on damage.
		int textOffset = 0;
		if (damage >= 5) textOffset = 0x10;
		if (damage >= 20) textOffset = 0x20;
		displayStringAt(0xB780 + textOffset);

		nextRowInMessageWindow();
		// "HITS FOR <damage> PTS"
		for (int j = 0; j < 0x0D; j++) {
			outputChar(getMem(0xBC4E + j));
			if (j == 8) outputWord(damage);
		}

		// Damage hits the first still-alive monster in the array (ASM b98D0).
		for (int m = 0; m < monstersHP.length; m++) {
			if (monstersHP[m] > 0) {
				monstersHP[m] = Math.max(0, monstersHP[m] - damage);
				if (monstersHP[m] == 0) {
					m_fightDeadMonsters++;
					nextRowInMessageWindow();
					// "KILLED ONE"
					displayString(0xBC5B, 0x0A);
				}
				break;
			}
		}
	}

	/**
	 * Dispatches a combat spell: 1/5/9/17 are damage spells, 2/6/10 lower the
	 * party's AR, 13 boosts the party's hit score. Updates state shared with
	 * {@link #characterWeaponAttack}.
	 */
	private void applyCombatSpell(int spellNumber, int monsterID, int[] monstersHP) {
		switch (spellNumber) {
			case 1: case 5: case 9: case 17:
				m_fightDeadMonsters = castAttackSpell(spellNumber, monsterID, monstersHP, m_fightDeadMonsters);
				return;
			case 2:  m_fightMagicARReduction = 0x02; return;   // SHIELD
			case 6:  m_fightMagicARReduction = 0x04; return;   // PROTECT
			case 10: m_fightMagicARReduction = 0x06; return;   // GUARDIAN
			case 13: m_fightPartyHitScoreBonus = 0x04; return; // ACCURACY
			default:
				// Unreachable: askPartyActions filters non-combat spells to 0 (weapon).
				throw new IllegalStateException("unexpected spell number " + spellNumber);
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
		int mask = rom().spellDamageMask(spellNumber);
		for (int i=0;i<monstersHP.length;i++) {
			// Skip already-dead monsters (ASM b97D5). Fireball must keep
			// searching until it finds a live target before stopping.
			if (monstersHP[i] <= 0) {
				continue;
			}
			int spellDamage = 2 * m_level + 1 + (RANDOM.nextInt(256) & mask);
			monstersHP[i] = Math.max(0, monstersHP[i] - spellDamage);
			nextRowInMessageWindow();
			if (monstersHP[i] == 0) {
				// Current monster killed — print "KILLED ONE" only.
				displayString(0xBC5B, 0x0A);
				deadMonsters++;
				if (deadMonsters == monstersHP.length) {
					return deadMonsters;
				}
			}
			else {
				// Monster wounded — print "HITS FOR <n> PTS".
				for (int j=0;j<0x0D;j++) {
					outputChar(getMem(0xBC4E+j));
					if (j==8) {
						outputWord(spellDamage);
					}
				}
			}
			delay();
			// Fireball (spell 1) is single-target; other attack spells hit every monster.
			if (spellNumber == 1) {
				break;
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
		
		// ASM b9B09: LDA/ASL on an unsigned byte — result is (HP*2) mod 256.
		int goldGained = (rom().monsterHP(monsterID) << 1) & 0xFF;
		outputWord(goldGained);
		
		// Display bytes 0E,21,19,0E,1B,12,0E,17,0C,0E,2A,24 that matches string "EXPERIENCE: " at in message window	
		nextRowInMessageWindow();
		offset += displayStringAt(0xA6A7+offset);
		
		// Balrog ?
		int xpGained;
		if (monsterID == 0x27) {
			xpGained = 0x2008; // 8200 XP
		}
		else {
			// Compute experience gain for this monster (ASM b9B3F).
			xpGained = rom().monsterAttackBonus(monsterID) << 4;
			xpGained *= (count+1);

			// ASM b9B5F halves the XP for each extra character beyond slot 0.
			if (party().at(1).isValid()) xpGained >>= 1;
			if (party().at(2).isValid()) xpGained >>= 1;
		}
		// j9B7B: the ASM displays the XP word for both branches.
		outputWord(xpGained);
		
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
		
		// Share gold and experience across all living characters.
		final int goldFinal = goldGained;
		final int xpFinal = xpGained;
		party().forEachAlive(c -> {
			c.setGold(checkOverflow16bits(c.getGold() + goldFinal));
			c.setMazeXp(checkOverflow16bits(c.getMazeXp() + xpFinal));
		});
	}

	/**
	 * Caps a computed gold / experience gain using the same 8-bit-wrap-and-
	 * saturate pattern as the ASM (b9BBF / b9BD3): once the MSB would overflow,
	 * it is frozen at $FF and the LSB keeps its wrapped value, so totals
	 * beyond 65535 become 0xFFxx where xx is the carry-byte.
	 */
	static int checkOverflow16bits(int v) {
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
		// ASM j9AAB reads $0812/$0912/$0A12 — one dexterity per filled slot.
		int dexterity = party().sumOverAlive(Character::getDexterity);
		
		// if monster attack bonus > sum dext, monster engage
		int monsterAttackBonus = rom().monsterAttackBonus(monsterID);
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
		if (dexterity - monsterAttackBonus - m_level - runningAwayMonsterBonus > RANDOM.nextInt(64)) {
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
			int startY = getMemU(0xA56D+i);
			int startX = getMemU(0xA575+i);
			int endY = getMemU(0xA57D+i);
			int endX = getMemU(0xA585+i);
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
		// En mode bitmap, la Screen RAM sert à définir les couleurs.
		// The Background Pixel Color is defined by Bits#0 - Bit#3 of the corresponding Byte in Screen RAM.  
		// The Foreground Pixel Color is defined by Bits#4 - Bits#7 - again from the corresponding Byte in Screen RAM. 
		// Remplissage de la mémoire $0400 -> $0800 avec $B1 (gris foncé sur fond blanc)
		// ASM b940C fills 1024 bytes; setCharAt silently guards past m_screenRam.
		for (int i=0;i<1024;i++) {
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
		outputWord(character.getStrength());
		
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
				displayStringAt(rom().itemNameAddress(i, itemCode));
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
		// ASM sA347: clear rows 5..19 inclusive, columns 21..38 inclusive.
		// The previous post-increment loop started writing at row 6 (off by one).
		for (m_charOutputRow = 5; m_charOutputRow <= 19; m_charOutputRow++) {
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
			int armorRating = rom().armorRating(armorId);
			byte shieldId = character.getItemCode(2);
			armorRating -= rom().shieldReduction(shieldId);
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
		// On utilise l'XP et pas l'XP temp : c'est bien ce que l'ASM s9BEA fait
		// (il lit la MSB du champ XP permanent à $0818/$0918/$0A18).
		int xp = party().sumOverAlive(c -> c.getXp() >> 10);
		m_wanderingMonsters = xp <= rom().wanderingThreshold(m_level);
	}

	/**
	 * $8DB1
	 * Draw 3D view content
	 */
	private void draw3DView() {
		long startTime = System.nanoTime();

		m_facingWalls = collectFacingWalls();
		drawWalls(m_facingWalls);

		// Simulate original loop duration (200 ms) without burning a core.
		long remaining = 200_000_000L - (System.nanoTime() - startTime);
		if (remaining > 0) {
			java.util.concurrent.locks.LockSupport.parkNanos(remaining);
		}
	}

	/**
	 * Walks five squares ahead of the party (the render depth) and collects the
	 * 5-slot {@link WallType} array returned by {@link MazeMap#facingWalls} for
	 * each. Replaces the former four-way dispatcher + getWallsFacing&lt;Dir&gt;.
	 */
	private WallType[][] collectFacingWalls() {
		WallType[][] facingWalls = new WallType[5][];
		MazeMap map = getGame().getMazeMap();
		int x = m_xPos;
		int y = m_yPos;
		for (int depth = 0; depth < 5; depth++) {
			facingWalls[depth] = map.facingWalls(m_level, x, y, m_orientation);
			x = (x + m_orientation.dx + MazeMap.SIZE) % MazeMap.SIZE;
			y = (y + m_orientation.dy + MazeMap.SIZE) % MazeMap.SIZE;
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
		WallType leftWall = facingWalls[depth][0];
		if (leftWall != WallType.NONE) {
			// Wall directly on our left.
			drawClosedPolyline(LEFT_WALL[depth]);
			if (shouldDrawDoor(leftWall)) {
				drawClosedPolyline(LEFT_DOOR[depth]);
			}
			return;
		}
		// No left wall: we can see the left neighbour's front wall instead.
		WallType leftFacingWall = facingWalls[depth][3];
		if (leftFacingWall == WallType.NONE) {
			return;
		}
		drawOpenPolyline(LEFT_FRONT_WALL[depth]);
		if (shouldDrawDoor(leftFacingWall)) {
			drawOpenPolyline(LEFT_FRONT_DOOR[depth]);
		}
		// Finally, the part of the neighbour's front wall hidden by any wall
		// of the previous (nearer) depth — drawn only when that depth was fully open.
		if (depth > 0
				&& facingWalls[depth-1][0] == WallType.NONE
				&& facingWalls[depth-1][3] == WallType.NONE) {
			drawOpenPolyline(LEFT_HIDDEN_WALL[depth]);
			if (shouldDrawDoor(leftFacingWall)) {
				drawOpenPolyline(LEFT_HIDDEN_DOOR[depth]);
			}
		}
	}

	private void drawRightWalls(WallType[][] facingWalls, int depth) {
		WallType rightWall = facingWalls[depth][1];
		if (rightWall != WallType.NONE) {
			drawClosedPolyline(RIGHT_WALL[depth]);
			if (shouldDrawDoor(rightWall)) {
				drawClosedPolyline(RIGHT_DOOR[depth]);
			}
			return;
		}
		WallType rightFacingWall = facingWalls[depth][4];
		if (rightFacingWall == WallType.NONE) {
			return;
		}
		drawOpenPolyline(RIGHT_FRONT_WALL[depth]);
		if (shouldDrawDoor(rightFacingWall)) {
			drawOpenPolyline(RIGHT_FRONT_DOOR[depth]);
		}
		if (depth > 0
				&& facingWalls[depth-1][1] == WallType.NONE
				&& facingWalls[depth-1][4] == WallType.NONE) {
			drawOpenPolyline(RIGHT_HIDDEN_WALL[depth]);
			if (shouldDrawDoor(rightFacingWall)) {
				drawOpenPolyline(RIGHT_HIDDEN_DOOR[depth]);
			}
		}
	}

	private void drawFrontWalls(WallType[][] facingWalls, int depth) {
		WallType frontWall = facingWalls[depth][2];
		if (frontWall == WallType.NONE) {
			return;
		}
		drawClosedPolyline(FRONT_WALL[depth]);
		if (shouldDrawDoor(frontWall)) {
			drawClosedPolyline(FRONT_DOOR[depth]);
		}
	}

	/** A door is visible when it's an explicit door, or when light lets us see hidden passages. */
	private boolean shouldDrawDoor(WallType wall) {
		return wall == WallType.DOOR || (wall == WallType.HIDDEN && m_lightCounter > 0);
	}

	/** Connect each pair of vertices AND close back from the last to the first. */
	private void drawClosedPolyline(int[] coords) {
		for (int i = 0; i < coords.length; i += 2) {
			int next = (i + 2) % coords.length;
			draw3DViewLine(coords[i], coords[i+1], coords[next], coords[next+1]);
		}
	}

	/** Connect each pair of vertices without closing the shape. */
	private void drawOpenPolyline(int[] coords) {
		for (int i = 0; i < coords.length - 2; i += 2) {
			draw3DViewLine(coords[i], coords[i+1], coords[i+2], coords[i+3]);
		}
	}

	// ==============================================================================
	// 3D view polyline tables. Each table has one entry per view depth (0..4). The
	// "closed" ones (wall on our left/right/front, or the corresponding doors) loop
	// the last vertex back to the first; the "open" ones (neighbours seen through
	// an absent side wall + the hidden part drawn in front) only connect sequential
	// vertices. Values are screen pixels within the 160x160 3D viewport.
	// ==============================================================================

	// Wall directly on our left at each depth (closed rectangle, 4 vertices).
	private static final int[][] LEFT_WALL = {
		{0x00, 0x9F, 0x00, 0x00, 0x0F, 0x0F, 0x0F, 0x91},
		{0x0F, 0x91, 0x0F, 0x0F, 0x28, 0x28, 0x28, 0x78},
		{0x28, 0x78, 0x28, 0x28, 0x3C, 0x3C, 0x3C, 0x64},
		{0x3C, 0x64, 0x3C, 0x3C, 0x46, 0x46, 0x46, 0x5A},
		{0x46, 0x5A, 0x46, 0x46, 0x4B, 0x4B, 0x4B, 0x55}
	};
	private static final int[][] LEFT_DOOR = {
		{0x00, 0x9F, 0x00, 0x0A, 0x06, 0x10, 0x06, 0x9A},
		{0x14, 0x8C, 0x14, 0x1C, 0x24, 0x2C, 0x24, 0x7C},
		{0x2C, 0x74, 0x2C, 0x32, 0x39, 0x3F, 0x39, 0x67},
		{0x3F, 0x61, 0x3F, 0x43, 0x44, 0x48, 0x44, 0x5C},
		{0x47, 0x59, 0x47, 0x49, 0x4A, 0x4C, 0x4A, 0x56}
	};

	// Front wall of the left neighbour, visible when no wall is on our immediate left.
	private static final int[][] LEFT_FRONT_WALL = {
		{0x00, 0x0F, 0x0F, 0x0F, 0x0F, 0x91, 0x00, 0x91},
		{0x0F, 0x28, 0x28, 0x28, 0x28, 0x78, 0x0F, 0x78},
		{0x28, 0x3C, 0x3C, 0x3C, 0x3C, 0x64, 0x28, 0x64},
		{0x3C, 0x46, 0x46, 0x46, 0x46, 0x5A, 0x3C, 0x5A},
		{0x46, 0x4B, 0x4B, 0x4B, 0x4B, 0x55, 0x46, 0x55}
	};
	private static final int[][] LEFT_FRONT_DOOR = {
		{0x00, 0x1E, 0x00, 0x1E, 0x00, 0x91},
		{0x0F, 0x32, 0x1E, 0x32, 0x1E, 0x78},
		{0x28, 0x42, 0x36, 0x42, 0x36, 0x64},
		{0x3C, 0x4A, 0x42, 0x4A, 0x42, 0x5A},
		{0x46, 0x4D, 0x49, 0x4D, 0x49, 0x55}
	};

	// The chunk of neighbour's front wall that would otherwise be hidden by the
	// previous-depth walls — drawn only when the previous depth is fully open.
	// Depth-0 rows are never used at runtime (the guard requires depth > 0).
	private static final int[][] LEFT_HIDDEN_WALL = {
		{0x00, 0x0F, 0x00, 0x00, 0x00, 0x91, 0x00, 0x91},
		{0x0F, 0x28, 0x00, 0x28, 0x00, 0x78, 0x0F, 0x78},
		{0x28, 0x3C, 0x14, 0x3C, 0x14, 0x64, 0x28, 0x64},
		{0x3C, 0x46, 0x32, 0x46, 0x32, 0x5A, 0x3C, 0x5A},
		{0x46, 0x4B, 0x41, 0x4B, 0x41, 0x55, 0x46, 0x55}
	};
	private static final int[][] LEFT_HIDDEN_DOOR = {
		{0x00, 0x1E, 0x00, 0x00, 0x00, 0x91},
		{0x0F, 0x32, 0x00, 0x32, 0x00, 0x78},
		{0x28, 0x42, 0x1A, 0x42, 0x1A, 0x64},
		{0x3C, 0x4A, 0x36, 0x4A, 0x36, 0x5A},
		{0x46, 0x4D, 0x43, 0x4D, 0x43, 0x55}
	};

	// Mirror of LEFT_* on the right side of the viewport.
	private static final int[][] RIGHT_WALL = {
		{0x9F, 0x9F, 0x9F, 0x00, 0x91, 0x0F, 0x91, 0x91},
		{0x91, 0x91, 0x91, 0x0F, 0x78, 0x28, 0x78, 0x78},
		{0x78, 0x78, 0x78, 0x28, 0x64, 0x3C, 0x64, 0x64},
		{0x64, 0x64, 0x64, 0x3C, 0x5A, 0x46, 0x5A, 0x5A},
		{0x5A, 0x5A, 0x5A, 0x46, 0x55, 0x4B, 0x55, 0x55}
	};
	private static final int[][] RIGHT_DOOR = {
		{0x9F, 0x9F, 0x9F, 0x0A, 0x9A, 0x10, 0x9A, 0x9A},
		{0x8C, 0x8C, 0x8C, 0x1C, 0x7C, 0x2C, 0x7C, 0x7C},
		{0x74, 0x74, 0x74, 0x32, 0x67, 0x3F, 0x67, 0x67},
		{0x61, 0x61, 0x61, 0x43, 0x5C, 0x48, 0x5C, 0x5C},
		{0x59, 0x59, 0x59, 0x49, 0x56, 0x4C, 0x56, 0x56}
	};
	private static final int[][] RIGHT_FRONT_WALL = {
		{0x9F, 0x0F, 0x91, 0x0F, 0x91, 0x91, 0x9F, 0x91},
		{0x91, 0x28, 0x78, 0x28, 0x78, 0x78, 0x91, 0x78},
		{0x78, 0x3C, 0x64, 0x3C, 0x64, 0x64, 0x78, 0x64},
		{0x64, 0x46, 0x5A, 0x46, 0x5A, 0x5A, 0x64, 0x5A},
		{0x5A, 0x4B, 0x55, 0x4B, 0x55, 0x55, 0x5A, 0x55}
	};
	private static final int[][] RIGHT_FRONT_DOOR = {
		{0x9F, 0x1E, 0x9F, 0x1E, 0x9F, 0x91},
		{0x91, 0x32, 0x82, 0x32, 0x82, 0x78},
		{0x78, 0x42, 0x6A, 0x42, 0x6A, 0x64},
		{0x64, 0x4A, 0x5E, 0x4A, 0x5E, 0x5A},
		{0x5A, 0x4D, 0x57, 0x4D, 0x57, 0x55}
	};
	// Depth-0 row preserved as-is (including the 0x9D entry that does not match the
	// symmetric pattern — never rendered thanks to the depth > 0 guard).
	private static final int[][] RIGHT_HIDDEN_WALL = {
		{0x9F, 0x0F, 0x00, 0x00, 0x00, 0x91, 0x9D, 0x91},
		{0x91, 0x28, 0x9F, 0x28, 0x9F, 0x78, 0x91, 0x78},
		{0x78, 0x3C, 0x8C, 0x3C, 0x8C, 0x64, 0x78, 0x64},
		{0x64, 0x46, 0x6E, 0x46, 0x6E, 0x5A, 0x64, 0x5A},
		{0x5A, 0x4B, 0x5F, 0x4B, 0x5F, 0x55, 0x5A, 0x55}
	};
	private static final int[][] RIGHT_HIDDEN_DOOR = {
		{0x9F, 0x1E, 0x00, 0x00, 0x00, 0x91},
		{0x91, 0x32, 0x9F, 0x32, 0x9F, 0x78},
		{0x78, 0x42, 0x86, 0x42, 0x86, 0x64},
		{0x64, 0x4A, 0x6A, 0x4A, 0x6A, 0x5A},
		{0x5A, 0x4D, 0x5D, 0x4D, 0x5D, 0x55}
	};

	// Wall directly in front of us at each depth (closed rectangle).
	private static final int[][] FRONT_WALL = {
		{0x0F, 0x0F, 0x91, 0x0F, 0x91, 0x91, 0x0F, 0x91},
		{0x28, 0x28, 0x78, 0x28, 0x78, 0x78, 0x28, 0x78},
		{0x3C, 0x3C, 0x64, 0x3C, 0x64, 0x64, 0x3C, 0x64},
		{0x46, 0x46, 0x5A, 0x46, 0x5A, 0x5A, 0x46, 0x5A},
		{0x4B, 0x4B, 0x55, 0x4B, 0x55, 0x55, 0x4B, 0x55}
	};
	private static final int[][] FRONT_DOOR = {
		{0x1E, 0x91, 0x1E, 0x1E, 0x82, 0x1E, 0x82, 0x91},
		{0x32, 0x78, 0x32, 0x32, 0x6E, 0x32, 0x6E, 0x78},
		{0x42, 0x64, 0x42, 0x42, 0x5E, 0x42, 0x5E, 0x64},
		{0x4A, 0x5A, 0x4A, 0x4A, 0x56, 0x4A, 0x56, 0x5A},
		{0x4D, 0x55, 0x4D, 0x4D, 0x53, 0x4D, 0x53, 0x55}
	};

	public void hideSprites() {
		for (int i=0;i<4;i++) {
			getGame().getVicII().getSprite(i).enabled = false;
		}
	}
	
	@Override
	public void exit() {
	}
}
