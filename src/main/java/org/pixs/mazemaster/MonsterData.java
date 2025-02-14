package org.pixs.mazemaster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MonsterData {
	
    private static MonsterData instance = null;
    private byte[] data;

    // Adresses dans le fichier .bin (64K)
    private static final int TOTAL_MONSTERS = 40; // IDs de 0 à 39

    // Table d'offset pour le nom
    private static final int NAME_TABLE_LOW_START = 0xA448;   // pour monsterID 0..26
    private static final int NAME_TABLE_LOW_COUNT = 27;
    private static final int NAME_TABLE_HIGH_START = 0xA463;  // pour monsterID 27..39

    // La zone texte contenant les noms commence à 0xA58D.
    private static final int NAME_TEXT_BASE = 0xA80D;
    private static final int NAME_TEXT_BASE_HIGH = 0xA90B;

    // Table pour les bonus d'attaque : 40 octets, à partir de 0xA470
    private static final int ATTACK_BONUS_START = 0xA470;
    // Table pour HP initiaux : 40 octets, à partir de 0xA498
    private static final int INITIAL_HP_START = 0xA498;
    // Table pour Armor Rating : 40 octets, à partir de 0xA4C0
    private static final int ARMOR_RATING_START = 0xA4C0;
    // Descripteur de sprite : 4 octets par monstre, à partir de 0xBE60
    private static final int SPRITE_DESCRIPTOR_START = 0xBE60;

    private MonsterData(byte[] data) {
        this.data = data;
    }

    /**
     * Initialise MonsterData avec le fichier .bin complet.
     * @param binFilePath chemin vers le fichier .bin de 64K.
     * @throws IOException si le fichier ne peut être lu.
     */
    public static void initialize() throws IOException {
        if(instance == null) {
        	byte[] data = MazeMasterViewer.class.getClassLoader().getResourceAsStream("org/pixs/mazemaster/maze_master.bin").readAllBytes();
			if (data.length != 65536) {
				throw new IllegalArgumentException("Le fichier doit contenir exactement 64K octets.");
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
     * Renvoie un MonsterType correspondant à l'ID passé.
     * Cet ID est normalisé dans la plage [0, TOTAL_MONSTERS-1].
     */
    public MonsterType getMonsterType(int monsterID) {
    	
        // Normalisation de l'ID
        monsterID = monsterID % TOTAL_MONSTERS;
        
        // Lecture du bonus d'attaque
        int attackBonus = data[ATTACK_BONUS_START + monsterID] & 0xFF;
        // Lecture des HP initiaux
        int initialHP = data[INITIAL_HP_START + monsterID] & 0xFF;
        // Lecture de l'armor rating
        int armorRating = data[ARMOR_RATING_START + monsterID] & 0xFF;
        
        // Lecture du descripteur de sprite (4 octets)
        byte[] spriteDescriptor = new byte[4];
        int spriteOffset = SPRITE_DESCRIPTOR_START + monsterID * 4;
        System.arraycopy(data, spriteOffset, spriteDescriptor, 0, 4);
        
        // Récupération de l'offset du nom depuis la bonne table
        int nameOffset;
        int nameAbsoluteOffset;
        if (monsterID < NAME_TABLE_LOW_COUNT) {
            nameOffset = data[NAME_TABLE_LOW_START + monsterID] & 0xFF;
            nameAbsoluteOffset = NAME_TEXT_BASE + nameOffset;
        } else {
            nameOffset = data[NAME_TABLE_HIGH_START + (monsterID - NAME_TABLE_LOW_COUNT)] & 0xFF;
            nameAbsoluteOffset = NAME_TEXT_BASE_HIGH + nameOffset;
        }
        // Calcul de l'offset absolu du nom
        String name = readStringUntilFF(nameAbsoluteOffset);
        
        return new MonsterType(monsterID, name, attackBonus, initialHP, armorRating, spriteDescriptor);
    }

    /**
     * Lit une chaîne à partir de l'offset donné jusqu'au caractère terminant (0xFF).
     * Pour simplifier, nous interprétons chaque octet comme un caractère ASCII.
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
