package org.pixs.mazemaster;

import java.io.IOException;

import org.pixs.SwingJoystick;
import org.pixs.SwingKeyboard;
import org.pixs.hardware.CIA1;
import org.pixs.hardware.VicIIDisplay;
import org.pixs.hardware.VicIIRenderer;

public class Main {

	public static void main(String[] args) throws IOException {
		VicIIRenderer renderer = new VicIIRenderer();
		VicIIDisplay vicII = renderer.getDisplay();
		
		SwingJoystick joystick = new SwingJoystick();
		SwingKeyboard keyboard = new SwingKeyboard();
		CIA1 cia1 = new CIA1(joystick, keyboard);
		
		renderer.displayInFrame("Maze Master", joystick, keyboard);

		renderer.startRefresh();
		
		Game game = new Game(vicII, cia1);
		keyboard.addKeyListener(game); 
		game.start();
	}
}
