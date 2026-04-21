package org.pixs.hardware;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.pixs.IJoystick;
import org.pixs.IJoystickButtonListener;
import org.pixs.IKeyListener;
import org.pixs.IKeyboard;
import org.pixs.JoystickButton;

/**
 * Aggregated input state: which joystick buttons and PETSCII keys are
 * currently held. Registers itself as a listener on a joystick and a
 * keyboard; the game loop polls the two sets on each tick. Historically
 * named "CIA1" because it filled the same role as the C64's CIA1 chip,
 * but there's no hardware emulation here.
 */
public class InputState implements IJoystickButtonListener, IKeyListener {

	private final Set<JoystickButton> m_pressedButton = new CopyOnWriteArraySet<>();
	private final Set<Byte> m_pressedKey = new CopyOnWriteArraySet<>();

	public InputState(IJoystick joystick, IKeyboard keyboard) {
		joystick.addButtonListener(this);
		keyboard.addKeyListener(this);
	}

	@Override
	public void buttonPressed(JoystickButton button) {
		m_pressedButton.add(button);
	}

	@Override
	public void buttonReleased(JoystickButton button) {
		m_pressedButton.remove(button);
	}

	public Set<JoystickButton> getPressedButton() {
		return m_pressedButton;
	}

	public Set<Byte> getPressedKey() {
		return m_pressedKey;
	}

	@Override
	public void keyPressed(byte keyCode) {
		m_pressedKey.add(keyCode);
	}

	@Override
	public void keyReleased(byte keyCode) {
		m_pressedKey.remove(keyCode);
	}
}

