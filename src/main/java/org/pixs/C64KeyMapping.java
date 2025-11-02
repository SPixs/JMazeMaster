package org.pixs;

public final class C64KeyMapping {
    /**
     * Table de conversion de scan code (0x00 to 0x3F) vers PETSCII.
     * Les indices correspondent aux constantes suivantes :
     *   0x00 : KEY_DEL          
     *   0x01 : KEY_RETURN       
     *   0x02 : KEY_CRSR_RIGHT   
     *   0x03 : KEY_F7           
     *   0x04 : KEY_F1           
     *   0x05 : KEY_F3           
     *   0x06 : KEY_F5           
     *   0x07 : KEY_CRSR_DOWN    
     *   0x08 : KEY_3            
     *   0x09 : KEY_W            
     *   0x0A : KEY_A            
     *   0x0B : KEY_4            
     *   0x0C : KEY_Z            
     *   0x0D : KEY_S            
     *   0x0E : KEY_E            
     *   0x0F : KEY_LSHIFT       
     *   0x10 : KEY_5            
     *   0x11 : KEY_R            
     *   0x12 : KEY_D            
     *   0x13 : KEY_6            
     *   0x14 : KEY_C            
     *   0x15 : KEY_F            
     *   0x16 : KEY_T            
     *   0x17 : KEY_X            
     *   0x18 : KEY_7            
     *   0x19 : KEY_Y            
     *   0x1A : KEY_G            
     *   0x1B : KEY_8            
     *   0x1C : KEY_B            
     *   0x1D : KEY_H            
     *   0x1E : KEY_U            
     *   0x1F : KEY_V            
     *   0x20 : KEY_9            
     *   0x21 : KEY_I            
     *   0x22 : KEY_J            
     *   0x23 : KEY_0            
     *   0x24 : KEY_M            
     *   0x25 : KEY_K            
     *   0x26 : KEY_O            
     *   0x27 : KEY_N            
     *   0x28 : KEY_PLUS         
     *   0x29 : KEY_P            
     *   0x2A : KEY_L            
     *   0x2B : KEY_MINUS        
     *   0x2C : KEY_DOT          
     *   0x2D : KEY_COLON        
     *   0x2E : KEY_AT           
     *   0x2F : KEY_COMMA        
     *   0x30 : KEY_POUND        
     *   0x31 : KEY_ASTERISK     
     *   0x32 : KEY_SEMICOLON    
     *   0x33 : KEY_HOME         
     *   0x34 : KEY_RSHIFT       
     *   0x35 : KEY_EQUALS       
     *   0x36 : KEY_ARROW_UP     
     *   0x37 : KEY_SLASH        
     *   0x38 : KEY_1            
     *   0x39 : KEY_ARROW_LEFT   
     *   0x3A : KEY_CTRL         
     *   0x3B : KEY_2            
     *   0x3C : KEY_SPACE        
     *   0x3D : KEY_COMMODORE    
     *   0x3E : KEY_Q            
     *   0x3F : KEY_RUNSTOP      
     *
     * Les valeurs PETSCII utilisées ici sont choisies à titre d’exemple.
     * Pour les touches alphanumériques, on utilise les codes correspondant
     * aux majuscules (par défaut sur C64 en mode texte).
     *
     * Pour les touches spéciales, plusieurs choix sont possibles.
     */
    public final static byte[] SCAN_TO_PETSCII = {
        /* 0x00 KEY_DEL          */ (byte)0x14,  // PETSCII DELETE (par exemple)
        /* 0x01 KEY_RETURN       */ (byte)0x0D,  // Carriage Return
        /* 0x02 KEY_CRSR_RIGHT   */ (byte)0x1D,  // Flèche droite
        /* 0x03 KEY_F7           */ (byte)0x93,  // Choix arbitraire pour F7
        /* 0x04 KEY_F1           */ (byte)0x91,  // Choix arbitraire pour F1
        /* 0x05 KEY_F3           */ (byte)0x92,  // Choix arbitraire pour F3
        /* 0x06 KEY_F5           */ (byte)0x94,  // Choix arbitraire pour F5
        /* 0x07 KEY_CRSR_DOWN    */ (byte)0x91,  // Flèche bas (souvent 0x91 en PETSCII)
        /* 0x08 KEY_3            */ (byte)'3',   // 0x33
        /* 0x09 KEY_W            */ (byte)'W',   // 0x57
        /* 0x0A KEY_A            */ (byte)'A',   // 0x41
        /* 0x0B KEY_4            */ (byte)'4',   // 0x34
        /* 0x0C KEY_Z            */ (byte)'Z',   // 0x5A
        /* 0x0D KEY_S            */ (byte)'S',   // 0x53
        /* 0x0E KEY_E            */ (byte)'E',   // 0x45
        /* 0x0F KEY_LSHIFT       */ (byte)0x00,  // Pas de PETSCII « caractère » pour Shift
        /* 0x10 KEY_5            */ (byte)'5',   // 0x35
        /* 0x11 KEY_R            */ (byte)'R',   // 0x52
        /* 0x12 KEY_D            */ (byte)'D',   // 0x44
        /* 0x13 KEY_6            */ (byte)'6',   // 0x36
        /* 0x14 KEY_C            */ (byte)'C',   // 0x43
        /* 0x15 KEY_F            */ (byte)'F',   // 0x46
        /* 0x16 KEY_T            */ (byte)'T',   // 0x54
        /* 0x17 KEY_X            */ (byte)'X',   // 0x58
        /* 0x18 KEY_7            */ (byte)'7',   // 0x37
        /* 0x19 KEY_Y            */ (byte)'Y',   // 0x59
        /* 0x1A KEY_G            */ (byte)'G',   // 0x47
        /* 0x1B KEY_8            */ (byte)'8',   // 0x38
        /* 0x1C KEY_B            */ (byte)'B',   // 0x42
        /* 0x1D KEY_H            */ (byte)'H',   // 0x48
        /* 0x1E KEY_U            */ (byte)'U',   // 0x55
        /* 0x1F KEY_V            */ (byte)'V',   // 0x56
        /* 0x20 KEY_9            */ (byte)'9',   // 0x39
        /* 0x21 KEY_I            */ (byte)'I',   // 0x49
        /* 0x22 KEY_J            */ (byte)'J',   // 0x4A
        /* 0x23 KEY_0            */ (byte)'0',   // 0x30
        /* 0x24 KEY_M            */ (byte)'M',   // 0x4D
        /* 0x25 KEY_K            */ (byte)'K',   // 0x4B
        /* 0x26 KEY_O            */ (byte)'O',   // 0x4F
        /* 0x27 KEY_N            */ (byte)'N',   // 0x4E
        /* 0x28 KEY_PLUS         */ (byte)'+',   // 0x2B
        /* 0x29 KEY_P            */ (byte)'P',   // 0x50
        /* 0x2A KEY_L            */ (byte)'L',   // 0x4C
        /* 0x2B KEY_MINUS        */ (byte)'-',   // 0x2D
        /* 0x2C KEY_DOT          */ (byte)'.',   // 0x2E
        /* 0x2D KEY_COLON        */ (byte)':',   // 0x3A
        /* 0x2E KEY_AT           */ (byte)'@',   // 0x40
        /* 0x2F KEY_COMMA        */ (byte)',',   // 0x2C
        /* 0x30 KEY_POUND        */ (byte)'#',   // Ici, on utilise '#' (0x23) pour la touche « pound »
        /* 0x31 KEY_ASTERISK     */ (byte)'*',   // 0x2A
        /* 0x32 KEY_SEMICOLON    */ (byte)';',   // 0x3B
        /* 0x33 KEY_HOME         */ (byte)0x93,  // Choix arbitraire pour HOME
        /* 0x34 KEY_RSHIFT       */ (byte)0x00,  // Pas de PETSCII pour Shift
        /* 0x35 KEY_EQUALS       */ (byte)'=',   // 0x3D
        /* 0x36 KEY_ARROW_UP     */ (byte)0x11,  // Flèche haut (souvent 0x11 en PETSCII)
        /* 0x37 KEY_SLASH        */ (byte)'/',   // 0x2F
        /* 0x38 KEY_1            */ (byte)'1',   // 0x31
        /* 0x39 KEY_ARROW_LEFT   */ (byte)0x1C,  // Flèche gauche (souvent 0x1C)
        /* 0x3A KEY_CTRL         */ (byte)0x00,  // Pas de PETSCII pour CTRL
        /* 0x3B KEY_2            */ (byte)'2',   // 0x32
        /* 0x3C KEY_SPACE        */ (byte)' ',   // Espace (0x20)
        /* 0x3D KEY_COMMODORE    */ (byte)0x8D,  // Choix arbitraire pour la touche Commodore
        /* 0x3E KEY_Q            */ (byte)'Q',   // 0x51
        /* 0x3F KEY_RUNSTOP      */ (byte)0x8E   // Choix arbitraire pour RUN/STOP
    };

	public static Byte scanToPETSCII(byte keyCode) {
		if ((keyCode & 0x0FF) < 0x40) {
			return SCAN_TO_PETSCII[keyCode];
		}
		switch (keyCode) {
			case IKeyboard.KEY_CRSR_UP: return (byte) 0xD1;
			case IKeyboard.KEY_CRSR_LEFT: return (byte) 0x9D; 
			default:
				throw new IllegalStateException();
		}
	}
}
