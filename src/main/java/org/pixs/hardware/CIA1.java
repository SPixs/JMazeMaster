package org.pixs.hardware;

import java.util.HashSet;
import java.util.Set;

import org.pixs.IJoystick;
import org.pixs.IJoystickButtonListener;
import org.pixs.IKeyListener;
import org.pixs.IKeyboard;
import org.pixs.JoystickButton;
import org.pixs.SwingKeyboard;

public class CIA1 implements IJoystickButtonListener, IKeyListener {

	private Set<JoystickButton> m_pressedButton = new HashSet<JoystickButton>();
	private Set<Byte> m_pressedKey = new HashSet<Byte>();

	public CIA1(IJoystick joystick, IKeyboard keyboard) {
		joystick.addButtonListener(this);
		keyboard.addKeyListener(this);
	}
	
	@Override
	public synchronized void buttonPressed(JoystickButton button) {
		m_pressedButton.add(button);
	}

	@Override
	public synchronized void buttonReleased(JoystickButton button) {
		m_pressedButton.remove(button);
	}

	public Set<JoystickButton> getPressedButton() {
		return m_pressedButton;
	}

	public synchronized Set<Byte> getPressedKey() {
		return m_pressedKey;
	}
	
	@Override
	public synchronized void keyPressed(byte keyCode) {
		m_pressedKey.add(keyCode);
	}

	@Override
	public synchronized void keyReleased(byte keyCode) {
		m_pressedKey.remove(keyCode);
	}
}

