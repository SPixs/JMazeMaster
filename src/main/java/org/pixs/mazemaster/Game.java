package org.pixs.mazemaster;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.pixs.C64KeyMapping;
import org.pixs.IKeyListener;
import org.pixs.hardware.InputState;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.mazemaster.states.GameState;
import org.pixs.mazemaster.states.InitState;

public class Game implements IKeyListener {

	private VicIIDisplay m_vicII;
	private InputState m_inputState;

	private GameState m_currentState = null;

	private byte[] m_memory;
	private Rom m_rom;
	private MazeMap m_mazeMap;
	private byte[] m_charset;

	private Party m_party;

	private byte[][] m_triggers = new byte[5][];

	public Game(VicIIDisplay vicII, InputState inputState) {
		m_vicII = vicII;
		m_inputState = inputState;
	}

	// ================================ Access methods  =========================================

	public VicIIDisplay getVicII() {
		return m_vicII;
	}

	public InputState getInputState() {
		return m_inputState;
	}

	public void setState(GameState state) {
		if (m_currentState != null) {
			m_currentState.exit();
		}
		m_currentState = state;
		m_currentState.enter();
	}

	public byte[] getCharset() {
		return m_charset;
	}
	
	public void setTriggers(int level, byte[] triggers) {
		m_triggers[level] = triggers;
	}
	
	public byte[] getTriggers(int level) {
		return m_triggers[level];
	}

	// ===================================== Methods ============================================
	
	public void start() throws IOException {
		
		// Create the raw image of C64 memory with the original game loaded
		m_memory = getClass().getClassLoader().getResourceAsStream("org/pixs/mazemaster/maze_master.bin").readAllBytes();
		m_rom = new Rom(m_memory);
		m_mazeMap = new MazeMap(m_rom);
		
		m_party = new Party();

		setState(new InitState(this));
	}

	public void initCharsetInRAM() {
		m_charset = new byte[800]; 
		
		// Copy $D180-$D27F -> $4000-$40FF
		// Recopie la ROM char standard des caracteres ayant les screen code 48 ("0") -> 79 (upper left corner)
		byte[] kernalCharset = m_vicII.getKernalCharset(0);
		for (int i=0;i<256;i++) {
			m_charset[i] = kernalCharset[i+0x180];
		}
		
		// Recopie la ROM char des caracteres ayant les screen code 1 ("A") -> 79 ("<-")
		// a la suite des caracteres "0"..."9"
		// Copy $D008-$D0FF -> $4050-$4147
		for (int i=8;i<256;i++) {
			m_charset[0x48+i] = kernalCharset[i];
		}
		
		// Recopie depuis les data du programme @$BD20 les caracteres restants
		// (' ','.',',','?',...) et le logo MAZE MASTER sur 2 chars de large
		// Copy $BD20-$BF1F -> $4120-$431F (512 bytes = 64 chars) Note : Seul $BD20-$BE5F semble contenir des chars dans la rom (320 bytes = 40 chars)
		for (int i=0;i<512;i++) {
			m_charset[0x120+i] = m_memory[0xBD20+i];
		}
	}

	public byte getMem(int adress) {
		return m_memory[adress];
	}

	public Rom getRom() {
		return m_rom;
	}

	public MazeMap getMazeMap() {
		return m_mazeMap;
	}

	public Party getParty() {
		return m_party;
	}

	/** Convenience shortcut; equivalent to {@code getParty().at(i)}. */
	public Character getCharacter(int i) {
		return m_party.at(i);
	}
	
	private ArrayBlockingQueue<Byte> pressedKeyWithPETSCIIBuffer = new ArrayBlockingQueue<Byte>(10);

	@Override
	public void keyPressed(byte keyCode) {
		byte petscii = C64KeyMapping.scanToPETSCII(keyCode);
		if (petscii == 0) return;
		// If buffer is at capacity (10 entries), drop the oldest.
		// offer() returns false on full instead of throwing like add().
		while (!pressedKeyWithPETSCIIBuffer.offer(petscii)) {
			pressedKeyWithPETSCIIBuffer.poll();
		}
	}

	@Override
	public void keyReleased(byte keyCode) {}
	
	public byte getIN() {
		if (pressedKeyWithPETSCIIBuffer.isEmpty()) return 0;
		return pressedKeyWithPETSCIIBuffer.poll();
	}

	public void deleteCharacter(int index) {
		m_party.deleteAt(index);
	}
}
