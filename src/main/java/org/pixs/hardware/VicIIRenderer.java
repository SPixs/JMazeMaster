package org.pixs.hardware;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.pixs.SwingKeyboard;

public class VicIIRenderer extends JPanel {
	
	private static final long serialVersionUID = 3233567388831703923L;

	private VicIIDisplay m_vicIIDisplay;

	private Timer m_timer;

	public VicIIRenderer() {
		m_vicIIDisplay = new VicIIDisplay();
		setPreferredSize(new Dimension(384*2, 272*2));
		m_timer = new Timer(20, e-> { repaint(); } );
	}
	
	protected void paintComponent(Graphics g) {
		g.drawImage(m_vicIIDisplay.getImage(), 0, 0, getWidth(), getHeight(), null);
	}

	public void startRefresh() {
		m_timer.start(); 
	}

	public VicIIDisplay getDisplay() {
		return m_vicIIDisplay;
	}
	
	public JFrame displayInFrame(String title, KeyListener joystick, SwingKeyboard keyboard) {
        JFrame f = new JFrame(title);
        try {
			f.setIconImage(ImageIO.read(getClass().getClassLoader().getResourceAsStream("org/pixs/c64_icon.png")));
		} 
        catch (IOException e) {}
        f.getContentPane().add(this);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.pack();
        
        if (joystick != null) {
			f.addKeyListener(joystick);
			f.addKeyListener(keyboard);
			f.setFocusable(true);
			f.setFocusTraversalKeysEnabled(false);
			f.requestFocus();
        }
			
        f.setVisible(true);
        return f;
	}
}
