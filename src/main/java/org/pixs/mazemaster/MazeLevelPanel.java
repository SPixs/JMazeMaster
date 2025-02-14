package org.pixs.mazemaster;

import javax.swing.*;

import org.pixs.mazemaster.Trigger.TriggerType;

import java.awt.*;
import java.util.List;

public class MazeLevelPanel extends JPanel {
    private MazeLevel level;
    private static final int CELL_SIZE = 20; // taille d'une case en pixels

    public MazeLevelPanel(MazeLevel level) {
        this.level = level;
        setPreferredSize(new Dimension(MazeLevel.GRID_SIZE * CELL_SIZE, MazeLevel.GRID_SIZE * CELL_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Utilisation d'un Graphics2D pour un rendu amélioré.
        Graphics2D g2d = (Graphics2D) g;
        // Activation d'un antialiasing optionnel pour les traits (facultatif)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	      g2d.setColor(Color.WHITE);
	      g2d.fillRect(0, 0, getWidth(), getHeight());

        // Parcours de la grille 20x20.
        // Notez que la case (0,0) est en bas à gauche,
        // donc la ligne 0 se dessine en bas.
        for (int row = 0; row < MazeLevel.GRID_SIZE; row++) {
            // Calculez la position Y : la ligne 0 correspond au bas de l'écran.
            int y = (MazeLevel.GRID_SIZE - row - 1) * CELL_SIZE;
            for (int col = 0; col < MazeLevel.GRID_SIZE; col++) {
                int x = col * CELL_SIZE;
                // Fond de la case en blanc.

                // Pour la cellule, on récupère le type de mur de chaque côté.
                WallType north = level.getWallType(row, col, Orientation.NORTH);
                WallType south = level.getWallType(row, col, Orientation.SOUTH);
                WallType east  = level.getWallType(row, col, Orientation.EAST);
                WallType west  = level.getWallType(row, col, Orientation.WEST);

                // Pour chaque côté, on calcule les coordonnées du segment correspondant.
                // Puis, s'il y a un mur (autre que NONE), on trace la ligne.
                // S'il s'agit d'une porte, on ajoute les marquages.
                // Les coordonnées sont tracées en fonction de l'orientation.
                // --- Côté NORTH ---
                if (north != WallType.NONE) {
                    int x1 = x;
                    int y1 = y;
                    int x2 = x + CELL_SIZE;
                    int y2 = y;
                    g2d.setColor(getColorForWall(north));
                    g2d.drawLine(x1, y1, x2, y2);
                    if (north == WallType.DOOR) {
                        boolean oneWay = isOneWay(row, col, Orientation.NORTH);
                        drawDoorMark(g2d, x1, y1, x2, y2, Orientation.NORTH, oneWay);
                    }
                }
                // --- Côté SOUTH ---
                if (south != WallType.NONE) {
                    int x1 = x;
                    int y1 = y + CELL_SIZE;
                    int x2 = x + CELL_SIZE;
                    int y2 = y + CELL_SIZE;
                    g2d.setColor(getColorForWall(south));
                    g2d.drawLine(x1, y1, x2, y2);
                    if (south == WallType.DOOR) {
                        boolean oneWay = isOneWay(row, col, Orientation.SOUTH);
                        drawDoorMark(g2d, x1, y1, x2, y2, Orientation.SOUTH, oneWay);
                    }
                }
                // --- Côté EAST ---
                if (east != WallType.NONE) {
                    int x1 = x + CELL_SIZE;
                    int y1 = y;
                    int x2 = x + CELL_SIZE;
                    int y2 = y + CELL_SIZE;
                    g2d.setColor(getColorForWall(east));
                    g2d.drawLine(x1, y1, x2, y2);
                    if (east == WallType.DOOR) {
                        boolean oneWay = isOneWay(row, col, Orientation.EAST);
                        drawDoorMark(g2d, x1, y1, x2, y2, Orientation.EAST, oneWay);
                    }
                }
                // --- Côté WEST ---
                if (west != WallType.NONE) {
                    int x1 = x;
                    int y1 = y;
                    int x2 = x;
                    int y2 = y + CELL_SIZE;
                    g2d.setColor(getColorForWall(west));
                    g2d.drawLine(x1, y1, x2, y2);
                    if (west == WallType.DOOR) {
                        boolean oneWay = isOneWay(row, col, Orientation.WEST);
                        drawDoorMark(g2d, x1, y1, x2, y2, Orientation.WEST, oneWay);
                    }
                }
            }
        }

        // Dessin des triggers.
        List<Trigger> triggers = level.getTriggers();
        for (Trigger trig : triggers) {
            int tx = trig.getX() * CELL_SIZE;
            // Transformation de la coordonnée Y pour le système (0,0) en bas à gauche.
            int ty = (MazeLevel.GRID_SIZE - trig.getY() - 1) * CELL_SIZE;
            Color trigColor;
            switch (trig.getType()) {
                case UP_STAIRS: trigColor = Color.GREEN; break;
                case DOWN_STAIRS: trigColor = Color.RED; break;
                case CLUE: trigColor = Color.MAGENTA; break;
                case HOLE: trigColor = Color.DARK_GRAY; break;
                case NORMAL: default: trigColor = Color.BLUE.darker(); break;
            }
            g2d.setColor(trigColor);
            if (trig.getType() != TriggerType.NORMAL) {
	            int r = 10; // diamètre du cercle
	            int cx = tx + (CELL_SIZE - r) / 2;
	            int cy = ty + (CELL_SIZE - r) / 2;
	            g2d.fillOval(cx, cy, r, r);
            }
            else {
                g2d.setFont(new JLabel().getFont().deriveFont(11.0f));
                g2d.drawString(String.valueOf(trig.getMonsterID(level)), tx+6, ty+14);
            }
        }
    }

    /**
     * Retourne la couleur associée à un type de mur.
     */
    private Color getColorForWall(WallType type) {
        switch (type) {
            case WALL:
            case DOOR: return Color.BLACK;
            case HIDDEN: return Color.GREEN;
            case NONE: default: return Color.WHITE;
        }
    }

    /**
     * Détermine si le passage (pour une porte) est en sens unique pour la cellule
     * et l'orientation donnée. Ici, pour l'exemple, nous retournons true
     * si (row + col) est pair, et false sinon. Vous pourrez adapter cette logique
     * selon les données réelles du jeu.
     */
    private boolean isOneWay(int row, int col, Orientation orientation) {
    	
    	WallType wallType = level.getWallType(row, col, orientation);
    	if (wallType != WallType.NONE) {
        	switch (orientation) {
    			case NORTH:
    				int nextRow = (row + 1) % 20;
    				WallType nextWallType = level.getWallType(nextRow, col, Orientation.SOUTH);
    				return nextWallType != WallType.DOOR && nextWallType != WallType.HIDDEN;
    			case SOUTH:
    				nextRow = (row - 1 + 20) % 20;
    				nextWallType = level.getWallType(nextRow, col, Orientation.NORTH);
    				return nextWallType != WallType.DOOR && nextWallType != WallType.HIDDEN;
    			case EAST:
    				int nextCol = (col + 1) % 20;
    				nextWallType = level.getWallType(row, nextCol, Orientation.WEST);
    				return nextWallType != WallType.DOOR && nextWallType != WallType.HIDDEN;
    			case WEST:
    				nextCol = (col - 1 + 20) % 20;
    				nextWallType = level.getWallType(row, nextCol, Orientation.EAST);
    				return nextWallType != WallType.DOOR && nextWallType != WallType.HIDDEN;
    			default:
    				throw new IllegalStateException();
    		}
        	    		
    	}
    	else {
    		return false;
    	}
    }

    /**
     * Dessine les marqueurs spécifiques pour une porte sur le segment donné.
     * Ce segment est défini par les points (x1, y1) et (x2, y2). L'orientation de la
     * porte est fournie pour déterminer la position et l'orientation des ticks et de la flèche.
     *
     * @param g      Graphics2D
     * @param x1, y1 point de départ du segment
     * @param x2, y2 point d'arrivée du segment
     * @param orientation Orientation de la face (NORTH, SOUTH, EAST, WEST)
     * @param oneWay true si le passage est en sens unique (alors une flèche est dessinée)
     */
    private void drawDoorMark(Graphics2D g, int x1, int y1, int x2, int y2,
                                Orientation orientation, boolean oneWay) {
        // Calcul du point médian du segment
        int mx = (x1 + x2) / 2;
        int my = (y1 + y2) / 2;
        int tickLen = 3;
        int tickGap = 3;
        int arrowSize = 8;

        // Dessin des ticks perpendiculaires
        if (orientation == Orientation.NORTH || orientation == Orientation.SOUTH) {
            // Porte horizontale : ticks verticaux
            int tickX1 = mx - tickGap;
            int tickX2 = mx + tickGap;
            // On dessine des ticks centrés verticalement sur la ligne
            g.drawLine(tickX1, y1 + (orientation == Orientation.NORTH ? tickLen : -tickLen), tickX1, y1);
            g.drawLine(tickX2, y1 + (orientation == Orientation.NORTH ? tickLen : -tickLen), tickX2, y1);
            if (oneWay) {
                // Flèche indiquant la direction : pour NORTH, flèche vers le haut; pour SOUTH, vers le bas.
                Polygon arrow = new Polygon();
                if (orientation == Orientation.NORTH) {
                	arrow.addPoint(mx, y1 + arrowSize);
                    arrow.addPoint(mx, y1 - arrowSize);
                    arrow.addPoint(mx - arrowSize/2, y1-4);
                    arrow.addPoint(mx, y1 - arrowSize);
                    arrow.addPoint(mx + arrowSize/2, y1-4);
                    arrow.addPoint(mx, y1 - arrowSize);
                } else { // SOUTH
                	arrow.addPoint(mx, y1 - arrowSize);
                    arrow.addPoint(mx, y1 + arrowSize);
                    arrow.addPoint(mx - arrowSize/2, y1+4);
                    arrow.addPoint(mx, y1 + arrowSize);
                    arrow.addPoint(mx + arrowSize/2, y1+4);
                    arrow.addPoint(mx, y1 + arrowSize);
                }
                g.setColor(Color.BLACK);
                g.drawPolygon(arrow);
            }
        } else if (orientation == Orientation.EAST || orientation == Orientation.WEST) {
            // Porte verticale : ticks horizontaux
            int tickY1 = my - tickGap;
            int tickY2 = my + tickGap;
            g.drawLine(x1 + (orientation == Orientation.WEST ? tickLen : -tickLen), tickY1, x1, tickY1);
            g.drawLine(x1 + (orientation == Orientation.WEST ? tickLen : -tickLen), tickY2, x1, tickY2);
            if (oneWay) {
                Polygon arrow = new Polygon();
                if (orientation == Orientation.WEST) {
                    arrow.addPoint(x1 + arrowSize, my);
                    arrow.addPoint(x1 - arrowSize, my);
                    arrow.addPoint(x1-4, my - arrowSize/2);
                    arrow.addPoint(x1 - arrowSize, my);
                    arrow.addPoint(x1-4, my + arrowSize/2);
                    arrow.addPoint(x1 - arrowSize, my);
                } else { // EAST
                    arrow.addPoint(x1 - arrowSize, my);
                    arrow.addPoint(x1 + arrowSize, my);
                    arrow.addPoint(x1+4, my - arrowSize/2);
                    arrow.addPoint(x1 + arrowSize, my);
                    arrow.addPoint(x1+4, my + arrowSize/2);
                    arrow.addPoint(x1 + arrowSize, my);
                }
                g.setColor(Color.BLACK);
                g.drawPolygon(arrow);
            }
        }
    }
}
