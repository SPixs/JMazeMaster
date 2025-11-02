package org.pixs.mazemaster;

//MazeLevel.java
import java.util.List;

public class MazeLevel {

	public static final int GRID_SIZE = 20;

	private int levelNumber;
	private int[][] walls; // 20x20 grid, each cell is a byte (0..255)
	private List<Trigger> triggers; // list of 56 triggers (pairs)
	private int wanderingThreshold; // seuil pour l'apparition des wandering monsters (0-255)

	public MazeLevel(int levelNumber, int[][] walls, List<Trigger> triggers, int wanderingThreshold) {
		this.levelNumber = levelNumber;
		this.walls = walls;
		this.triggers = triggers;
		this.wanderingThreshold = wanderingThreshold;
	}

	public int getLevelNumber() {
		return levelNumber;
	}

	public int[][] getWalls() {
		return walls;
	}

	public List<Trigger> getTriggers() {
		return triggers;
	}

	public int getWanderingThreshold() {
		return wanderingThreshold;
	}

	/**
	 * Retourne le type de mur pour la case (row, col) dans la direction donnée.
	 * Chaque cellule (un octet) se structure ainsi : - Bits 0–1 : mur au NORTH -
	 * Bits 2–3 : mur au SOUTH - Bits 4–5 : mur à l'EAST - Bits 6–7 : mur à l'OUEST
	 */
	public WallType getWallType(int row, int col, Orientation orientation) {
		if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) {
			throw new IndexOutOfBoundsException("La case (" + row + ", " + col + ") est hors limites.");
		}
		int cell = walls[row][col];
		int shift;
		switch (orientation) {
		case NORTH:
			shift = 0;
			break;
		case SOUTH:
			shift = 2;
			break;
		case EAST:
			shift = 4;
			break;
		case WEST:
			shift = 6;
			break;
		default:
			shift = 0;
			break;
		}
		int value = (cell >> shift) & 0x03;
		return WallType.fromValue(value);
	}

	/**
	 * Retourne une description textuelle de la géométrie d’une case.
	 */
	public String getCellDescription(int row, int col) {
		StringBuilder sb = new StringBuilder();
		sb.append("Case (").append(row).append(", ").append(col).append(") : ");
		for (Orientation o : Orientation.values()) {
			sb.append(o.name()).append("=").append(getWallType(row, col, o).name()).append(" ");
		}
		return sb.toString().trim();
	}
	
	public MonsterType generateMonsterForTrigger(Trigger trig) {
	    // Vérifier que trig.getType() est NORMAL (c'est-à-dire non spécial).
	    if (trig.getType() != Trigger.TriggerType.NORMAL) {
	        return null; // ou lever une exception, ou gérer différemment.
	    }
	    // Simulate reading a random value, for example:
	    int randVal = (int)(Math.random() * 16); // 0 à 15
	    // Adjustment based on level: for example, add 6 * (level)
	    int levelNumber = this.levelNumber; // level 0 to 4
	    int monsterID = randVal + (6 * levelNumber);
	    
	    // Now, from monsterID, we can retrieve
	    // monster characteristics by consulting tables
	    // stored in ROM (in binary file area, for example).
	    // In our solution, these tables could be extracted into a class
	    // utility (for example, MonsterData) that provides a method getMonsterType(monsterID)
	    
	    MonsterType monster = MonsterData.getInstance().getMonsterType(monsterID);
	    return monster;
	}
}
