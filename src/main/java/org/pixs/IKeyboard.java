package org.pixs;

public interface IKeyboard {

	public final static byte KEY_DEL          = (byte)0x00;
	public final static byte KEY_RETURN       = (byte)0x01;
	public final static byte KEY_CRSR_RIGHT   = (byte)0x02;
	public final static byte KEY_F7           = (byte)0x03;
	public final static byte KEY_F1           = (byte)0x04;
	public final static byte KEY_F3           = (byte)0x05;
	public final static byte KEY_F5           = (byte)0x06;
	public final static byte KEY_CRSR_DOWN    = (byte)0x07;
	public final static byte KEY_3            = (byte)0x08;
	public final static byte KEY_W            = (byte)0x09;
	public final static byte KEY_A            = (byte)0x0a;
	public final static byte KEY_4            = (byte)0x0b;
	public final static byte KEY_Z            = (byte)0x0c;
	public final static byte KEY_S            = (byte)0x0d;
	public final static byte KEY_E            = (byte)0x0e;
	public final static byte KEY_LSHIFT       = (byte)0x0f;
	public final static byte KEY_5            = (byte)0x10;
	public final static byte KEY_R            = (byte)0x11;
	public final static byte KEY_D            = (byte)0x12;
	public final static byte KEY_6            = (byte)0x13;
	public final static byte KEY_C            = (byte)0x14;
	public final static byte KEY_F            = (byte)0x15;
	public final static byte KEY_T            = (byte)0x16;
	public final static byte KEY_X            = (byte)0x17;
	public final static byte KEY_7            = (byte)0x18;
	public final static byte KEY_Y            = (byte)0x19;
	public final static byte KEY_G            = (byte)0x1a;
	public final static byte KEY_8            = (byte)0x1b;
	public final static byte KEY_B            = (byte)0x1c;
	public final static byte KEY_H            = (byte)0x1d;
	public final static byte KEY_U            = (byte)0x1e;
	public final static byte KEY_V            = (byte)0x1f;
	public final static byte KEY_9            = (byte)0x20;
	public final static byte KEY_I            = (byte)0x21;
	public final static byte KEY_J            = (byte)0x22;
	public final static byte KEY_0            = (byte)0x23;
	public final static byte KEY_M            = (byte)0x24;
	public final static byte KEY_K            = (byte)0x25;
	public final static byte KEY_O            = (byte)0x26;
	public final static byte KEY_N            = (byte)0x27;
	public final static byte KEY_PLUS         = (byte)0x28;
	public final static byte KEY_P            = (byte)0x29;
	public final static byte KEY_L            = (byte)0x2a;
	public final static byte KEY_MINUS        = (byte)0x2b;
	public final static byte KEY_DOT          = (byte)0x2c;
	public final static byte KEY_COLON        = (byte)0x2d;
	public final static byte KEY_AT           = (byte)0x2e;
	public final static byte KEY_COMMA        = (byte)0x2f;
	public final static byte KEY_POUND        = (byte)0x30;
	public final static byte KEY_ASTERISK     = (byte)0x31;
	public final static byte KEY_SEMICOLON    = (byte)0x32;
	public final static byte KEY_HOME         = (byte)0x33;
	public final static byte KEY_RSHIFT       = (byte)0x34;
	public final static byte KEY_EQUALS       = (byte)0x35;
	public final static byte KEY_ARROW_UP     = (byte)0x36;
	public final static byte KEY_SLASH        = (byte)0x37;
	public final static byte KEY_1            = (byte)0x38;
	public final static byte KEY_ARROW_LEFT   = (byte)0x39;
	public final static byte KEY_CTRL         = (byte)0x3a;
	public final static byte KEY_2            = (byte)0x3b;
	public final static byte KEY_SPACE        = (byte)0x3c;
	public final static byte KEY_COMMODORE    = (byte)0x3d;
	public final static byte KEY_Q            = (byte)0x3e;
	public final static byte KEY_RUNSTOP      = (byte)0x3f;
	
	// Extensions
	public final static byte KEY_CRSR_UP   	  = (byte)0xF0;
	public final static byte KEY_CRSR_LEFT    = (byte)0xF1;

	
	void addKeyListener(IKeyListener listener);

}

