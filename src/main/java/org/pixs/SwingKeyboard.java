package org.pixs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SwingKeyboard implements IKeyboard, KeyListener {

		//   ============================ Constants ==============================

		//	 =========================== Attributes ==============================
		
		private List<IKeyListener> m_listeners = new ArrayList<IKeyListener>();

		//	 =========================== Constructor =============================

		//	 ========================== Access methods ===========================

		//	 ========================= Treatment methods =========================

		
	    // Set to remember currently pressed keys.
	    private final Set<Integer> pressedKeys = new HashSet<>();
		
		@Override
		public void keyPressed(KeyEvent event) {
			int keyCode = event.getKeyCode();
			if (!pressedKeys.contains(keyCode)) {
	            pressedKeys.add(keyCode);
				keyPressed(event.getKeyCode());
			}
		}
		
		public void keyPressed(int key) {
			Byte keyCode = getMappedKey(key);
			if (keyCode != null) {
				for (IKeyListener listener : m_listeners) {
					listener.keyPressed(keyCode.byteValue());
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent event) {
			pressedKeys.remove(event.getKeyCode());
			keyReleased(event.getKeyCode());
		}
		
		public void keyReleased(int key) {
			Byte keyCode = getMappedKey(key);
			if (keyCode != null) {
				for (IKeyListener listener : m_listeners) {
					listener.keyReleased(keyCode.byteValue());
				}
			}
		}
		
		private Byte getMappedKey(int keyCode) {
			switch (keyCode) {
				case KeyEvent.VK_0: return KEY_0;
				case KeyEvent.VK_1: return KEY_1;
				case KeyEvent.VK_2: return KEY_2;
				case KeyEvent.VK_3: return KEY_3;
				case KeyEvent.VK_4: return KEY_4;
				case KeyEvent.VK_5: return KEY_5;
				case KeyEvent.VK_6: return KEY_6;
				case KeyEvent.VK_7: return KEY_7;
				case KeyEvent.VK_8: return KEY_8;
				case KeyEvent.VK_9: return KEY_9;

				case KeyEvent.VK_A: return KEY_A;
				case KeyEvent.VK_B: return KEY_B;
				case KeyEvent.VK_C: return KEY_C;
				case KeyEvent.VK_D: return KEY_D;
				case KeyEvent.VK_E: return KEY_E;
				case KeyEvent.VK_F: return KEY_F;
				case KeyEvent.VK_G: return KEY_G;
				case KeyEvent.VK_H: return KEY_H;
				case KeyEvent.VK_I: return KEY_I;
				case KeyEvent.VK_J: return KEY_J;
				case KeyEvent.VK_K: return KEY_K;
				case KeyEvent.VK_L: return KEY_L;
				case KeyEvent.VK_M: return KEY_M;
				case KeyEvent.VK_N: return KEY_N;
				case KeyEvent.VK_O: return KEY_O;
				case KeyEvent.VK_P: return KEY_P;
				case KeyEvent.VK_Q: return KEY_Q;
				case KeyEvent.VK_R: return KEY_R;
				case KeyEvent.VK_S: return KEY_S;
				case KeyEvent.VK_T: return KEY_T;
				case KeyEvent.VK_U: return KEY_U;
				case KeyEvent.VK_V: return KEY_V;
				case KeyEvent.VK_W: return KEY_W;
				case KeyEvent.VK_X: return KEY_X;
				case KeyEvent.VK_Y: return KEY_Y;
				case KeyEvent.VK_Z: return KEY_Z;

				case KeyEvent.VK_BACK_SPACE: return KEY_DEL;
				case KeyEvent.VK_ENTER: return KEY_RETURN;
				case KeyEvent.VK_SPACE: return KEY_SPACE;
				
				case KeyEvent.VK_F5: return KEY_F5;
				case KeyEvent.VK_F7: return KEY_F7;
				case KeyEvent.VK_COMMA: return KEY_COMMA;
				case KeyEvent.VK_SEMICOLON: return KEY_DOT;
				
				case KeyEvent.VK_UP: return KEY_CRSR_UP;
				case KeyEvent.VK_DOWN: return KEY_CRSR_DOWN;
				case KeyEvent.VK_LEFT: return KEY_CRSR_LEFT;
				case KeyEvent.VK_RIGHT: return KEY_CRSR_RIGHT;
			}
			return null;	
		}

		@Override
		public void keyTyped(KeyEvent arg0) {}

		@Override
		public void addKeyListener(IKeyListener listener) {
			m_listeners.add(listener);
		}
	}



