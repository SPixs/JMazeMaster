package org.pixs.mazemaster;

// Enum pour le type de mur d'une face
public enum WallType {
	NONE, WALL, DOOR, HIDDEN;

	public static WallType fromValue(int value) {
		switch (value) {
		case 0:
			return NONE;
		case 1:
			return WALL;
		case 2:
			return DOOR;
		case 3:
			return HIDDEN;
		default:
			return NONE;
		}
	}
}