package org.pixs.mazemaster;

//MazeLevelParser.java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MazeLevelParser {
	
 // Addresses (offsets) in file for wall map and triggers
 private static final int[] WALLS_ADDRESSES = {0xAA00, 0xAC00, 0xAE00, 0xB000, 0xB200};
 private static final int WALLS_SIZE = 400; // 20x20
 
 private static final int[] TRIGGERS_ADDRESSES = {0xAB90, 0xAD90, 0xAF90, 0xB190, 0xB390};
 private static final int TRIGGERS_SIZE = 112; // 56 paires d’octets
 
 // Table for wandering monsters : from 0xA3E6 to 0xA3EA (1 byte per level)
 private static final int WANDERING_THRESHOLD_START = 0xA3E6;
 
 public static MazeLevel[] parseLevels(byte[] data) throws IOException {

     MazeLevel[] levels = new MazeLevel[5];
     for (int levelNum = 0; levelNum < 5; levelNum++) {
         // Read wall map
         int wallsOffset = WALLS_ADDRESSES[levelNum];
         int[][] walls = new int[MazeLevel.GRID_SIZE][MazeLevel.GRID_SIZE];
         for (int i = 0; i < WALLS_SIZE; i++) {
             int row = i / MazeLevel.GRID_SIZE;
             int col = i % MazeLevel.GRID_SIZE;
             walls[row][col] = data[wallsOffset + i] & 0xFF;
         }
         
         // Reading triggers (112 bytes = 56 paires)
         int triggersOffset = TRIGGERS_ADDRESSES[levelNum];
         List<Trigger> triggers = new ArrayList<>();
         for (int i = 0; i < TRIGGERS_SIZE; i += 2) {
             int trigY = data[triggersOffset + i] & 0xFF;
             int trigX = data[triggersOffset + i + 1] & 0xFF;
             Trigger.TriggerType type;
             // Les 4 premières paires sont spéciales
             if (i < 8) {
                 switch(i / 2) {
                     case 0: type = Trigger.TriggerType.UP_STAIRS; break;
                     case 1: type = Trigger.TriggerType.DOWN_STAIRS; break;
                     case 2: type = Trigger.TriggerType.CLUE; break;
                     case 3: type = Trigger.TriggerType.HOLE; break;
                     default: type = Trigger.TriggerType.NORMAL; break;
                 }
             } else {
                 type = Trigger.TriggerType.NORMAL;
             }
             triggers.add(new Trigger(i/2, trigX, trigY, type));
         }
         
         // Reading seuil pour wandering monsters depuis la table en ROM
         int wanderingThreshold = data[WANDERING_THRESHOLD_START + levelNum] & 0xFF;
         
         levels[levelNum] = new MazeLevel(levelNum, walls, triggers, wanderingThreshold);
     }
     return levels;
 }
}
