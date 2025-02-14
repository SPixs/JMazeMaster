package org.pixs.hardware;

public class Sprite {
	
    // Dimensions du sprite en mode normal (24x21 pixels)
    public static final int WIDTH = 24;
    public static final int HEIGHT = 21;
    public static final int TRANSPARENT = -1;

    // Attributs de contrôle du sprite
    public boolean enabled;      // Actif ou non
    public int x, y;             // Position à l'écran (coordonnées en pixels)
    public boolean doubleWidth;
    public boolean doubleHeight;
    public boolean multicolor;   // Si false : sprite monochrome ; sinon, mode multicolore

    // Couleurs
    // En mode monochrome, seul 'color' est utilisé.
    // En mode multicolore, le codage est : 00 = transparent, 01 = multiColor1, 10 = multiColor2, 11 = color.
    public int color;         
    public int multiColor1;
    public int multiColor2;

    // Données du sprite : 63 octets (21 lignes * 3 octets par ligne)
    // Chaque ligne correspond à 24 bits.
    public byte[] spriteData = new byte[64];

    /**
     * Retourne la couleur du pixel dans le sprite à la position locale (relX, relY)
     * (les coordonnées locales sont en mode normal, c’est-à-dire 0..23 et 0..20).
     * Renvoie TRANSPARENT si le pixel est transparent.
     */
    public int getPixelAt(int relX, int relY) {
        if (!multicolor) {
            // Mode monochrome : chaque bit (dans 3 octets par ligne) représente un pixel.
            int byteIndex = relY * 3 + (relX / 8);
            int bitIndex = 7 - (relX % 8);
            boolean bitSet = (spriteData[byteIndex] & (1 << bitIndex)) != 0;
            return bitSet ? color : TRANSPARENT;
        } else {
            // Mode multicolore : la résolution horizontale est divisée par 2
            // On part du principe que relX est en mode « normal » et on divise par 2.
            int mcX = relX / 2; // valeur entre 0 et 11
            // Chaque ligne de 3 octets représente 12 pixels en multicolore (2 bits par pixel)
            int byteIndex = relY * 3 + (mcX / 4);  // 4 pixels par octet
            int shift = 6 - 2 * (mcX % 4);
            int bits = (spriteData[byteIndex] >> shift) & 0x03;
            switch (bits) {
                case 0: return TRANSPARENT;
                case 1: return multiColor1;
                case 2: return color;
                case 3: return multiColor2;
                default: return TRANSPARENT; // cas impossible
            }
        }
    }
}
