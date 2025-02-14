package org.pixs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class SwingJoystick implements IJoystick, KeyListener {

		//   ============================ Constants ==============================

		//	 =========================== Attributes ==============================
		
		private List<IJoystickButtonListener> m_listeners = new ArrayList<IJoystickButtonListener>();

		//	 =========================== Constructor =============================

		//	 ========================== Access methods ===========================

		//	 ========================= Treatment methods =========================

		@Override
		public void keyPressed(KeyEvent event) {
			keyPressed(event.getKeyCode());
		}
		
		public void keyPressed(int key) {
			JoystickButton button = getMappedButton(key);
			if (button != null) {
				for (IJoystickButtonListener listener : m_listeners) {
					listener.buttonPressed(button);
				}
			}
		}

		private JoystickButton getMappedButton(int keyCode) {
			switch (keyCode) {
				case KeyEvent.VK_UP: return JoystickButton.UP;
				case KeyEvent.VK_DOWN: return JoystickButton.DOWN;
				case KeyEvent.VK_LEFT: return JoystickButton.LEFT;
				case KeyEvent.VK_RIGHT: return JoystickButton.RIGHT;
				case KeyEvent.VK_SPACE: return JoystickButton.FIRE;
			}
			return null;
		}

		@Override
		public void keyReleased(KeyEvent event) {
			keyReleased(event.getKeyCode());
		}
		
		public void keyReleased(int key) {
			JoystickButton button = getMappedButton(key);
			if (button != null) {
				for (IJoystickButtonListener listener : m_listeners) {
					listener.buttonReleased(button);
				}
			}
		}

		@Override
		public void keyTyped(KeyEvent arg0) {}

		@Override
		public void addButtonListener(IJoystickButtonListener listener) {
			m_listeners.add(listener);
		}
	}



