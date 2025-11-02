package org.pixs.mazemaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MonsterData {
	
    private static MonsterData instance = null;
    private byte[] data;

    // Addresses in the file .bin (64K)
    private static final int TOTAL_MONSTERS = 40; // IDs from 0 to 39

    // Table for name
    private static final int NAME_TABLE_LOW_START = 0xA448;   // for monsterID 0..26
    private static final int NAME_TABLE_LOW_COUNT = 27;
    private static final int NAME_TABLE_HIGH_START = 0xA463;  // for monsterID 27..39

    // The text area containing names starts at 0xA58D.
    private static final int NAME_TEXT_BASE = 0xA80D;
    private static final int NAME_TEXT_BASE_HIGH = 0xA90B;

    // Table for attack bonuses : 40 bytes, starting from 0xA470
    private static final int ATTACK_BONUS_START = 0xA470;
    // Table for initial HP : 40 bytes, starting from 0xA498
    private static final int INITIAL_HP_START = 0xA498;
    // Table for Armor Rating : 40 bytes, starting from 0xA4C0
    private static final int ARMOR_RATING_START = 0xA4C0;
    // Sprite descriptor : 4 bytes per monster, starting from 0xBE60
    private static final int SPRITE_DESCRIPTOR_START = 0xBE60;

    private MonsterData(byte[] data) {
        this.data = data;
    }

    /**
     * Initialize MonsterData with the .bin complet.
     * @param binFilePath path to the file .bin de 64K.
     * @throws IOException if the file cannot be read.
     */
    public static void initialize() throws IOException {
        if(instance == null) {
        	byte[] data = MazeMasterViewer.class.getClassLoader().getResourceAsStream("org/pixs/mazemaster/maze_master.bin").readAllBytes();
			if (data.length != 65536) {
				throw new IllegalArgumentException("File must contain exactly 64K bytes.");
			}
            instance = new MonsterData(data);
        }
    }
    
    public static void main(String[] args) throws IOException {
		MonsterData.initialize();
		for (int i=0;i<TOTAL_MONSTERS;i++) {
			System.out.println(instance.getMonsterType(i));
		}
	}

    public static MonsterData getInstance() {
        if(instance == null) {
        	try {
				initialize();
			} 
        	catch (IOException e) {
				e.printStackTrace();
			}
        }
        return instance;
    }

    /**
     * Returns a MonsterType corresponding à l'ID passé.
     * This ID is normalized to the range [0, TOTAL_MONSTERS-1].
     */
    public MonsterType getMonsterType(int monsterID) {
    	
        // Normalize ID
        monsterID = monsterID % TOTAL_MONSTERS;
        
        // Read attack bonus
        int attackBonus = data[ATTACK_BONUS_START + monsterID] & 0xFF;
        // Reading initial HP
        int initialHP = data[INITIAL_HP_START + monsterID] & 0xFF;
        // Read armor rating
        int armorRating = data[ARMOR_RATING_START + monsterID] & 0xFF;
        
        // Read sprite descriptor (4 bytes)
        byte[] spriteDescriptor = new byte[4];
        int spriteOffset = SPRITE_DESCRIPTOR_START + monsterID * 4;
        System.arraycopy(data, spriteOffset, spriteDescriptor, 0, 4);
        
        // Retrieve name offset from appropriate table
        int nameOffset;
        int nameAbsoluteOffset;
        if (monsterID < NAME_TABLE_LOW_COUNT) {
            nameOffset = data[NAME_TABLE_LOW_START + monsterID] & 0xFF;
            nameAbsoluteOffset = NAME_TEXT_BASE + nameOffset;
        } else {
            nameOffset = data[NAME_TABLE_HIGH_START + (monsterID - NAME_TABLE_LOW_COUNT)] & 0xFF;
            nameAbsoluteOffset = NAME_TEXT_BASE_HIGH + nameOffset;
        }
        // Calculate absolute name offset
        String name = readStringUntilFF(nameAbsoluteOffset);
        
        return new MonsterType(monsterID, name, attackBonus, initialHP, armorRating, spriteDescriptor);
    }

    /**
     * Reads a string from the given offset until the terminating character (0xFF).
     * For simplicity, we interpret each byte as an ASCII character.
     */
    private String readStringUntilFF(int offset) {
    	
    	char[] mapping = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ ,.?)-:|".toCharArray();
    	
        StringBuilder sb = new StringBuilder();
        while (offset < data.length) {
            int b = data[offset] & 0xFF;
            if (b == 0xFF) {
                break;
            }
            sb.append(mapping[b]);
            offset++;
        }
        return sb.toString();
    }
}
