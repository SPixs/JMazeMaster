package org.pixs.mazemaster;

//Trigger.java
public class Trigger {
 private int x;
 private int y;
 private TriggerType type;
private int index;
 

public enum TriggerType {
     UP_STAIRS,
     DOWN_STAIRS,
     CLUE,
     HOLE,
     NORMAL
 }
 
 public Trigger(int index, int x, int y, TriggerType type) {
	 this.index = index;
     this.x = x;
     this.y = y;
     this.type = type;
 }
 
 public int getIndex() {
	return index;
}
 public int getX() { return x; }
 public int getY() { return y; }
 public TriggerType getType() { return type; }
 
 public int getMonsterID(MazeLevel level) {
	 return (getIndex() & 0x0f) + 6 * level.getLevelNumber();
 }
 
 /**
  * Retourne une description en langage naturel du trigger.
 * @param level 
  */
 public String getDescription(MazeLevel level) {
     switch (type) {
         case UP_STAIRS: return "Escalier montant (Up Stairs) à (" + x + ", " + y + ")";
         case DOWN_STAIRS: return "Escalier descendant (Down Stairs) à (" + x + ", " + y + ")";
         case CLUE: return "Indice (Clue) à (" + x + ", " + y + ")";
         case HOLE: return "Trou dans le sol (Hole) à (" + x + ", " + y + ")";
         case NORMAL: default: return "Trigger monstre à (" + x + ", " + y + ") : " + (level == null ? "???" : MonsterData.getInstance().getMonsterType(getMonsterID(level)).getName());
     }
 }
 
 @Override
 public String toString() {
     return getDescription(null);
 }
}
