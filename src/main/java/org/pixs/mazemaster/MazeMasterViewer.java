package org.pixs.mazemaster;

//MazeMasterViewer.java
import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MazeMasterViewer extends JFrame {

	private static final long serialVersionUID = 6832987329177287180L;

	private MazeLevel[] levels;

	public MazeMasterViewer(MazeLevel[] levels) {
		this.levels = levels;
		setTitle("Maze Master - Viewer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Utilisation d'un JTabbedPane pour afficher chaque niveau dans un onglet
		JTabbedPane tabbedPane = new JTabbedPane();

		for (MazeLevel level : levels) {
			MazeLevelPanel levelPanel = new MazeLevelPanel(level);

			// Zone texte pour afficher la liste des triggers interprétés
			JTextArea triggerArea = new JTextArea();
			triggerArea.setRows(10);
			triggerArea.setEditable(false);
			StringBuilder sb = new StringBuilder();
			sb.append("Triggers du niveau ").append(level.getLevelNumber()).append(":\n");
			for (Trigger trig : level.getTriggers()) {
				sb.append(trig.getDescription(level)).append("\n");
			}
			triggerArea.setText(sb.toString());

			// Combinaison du panneau graphique et de la zone texte dans un panel global
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(levelPanel, BorderLayout.CENTER);
			panel.add(new JScrollPane(triggerArea), BorderLayout.SOUTH);

			tabbedPane.addTab("Niveau " + level.getLevelNumber(), panel);
		}

		add(tabbedPane);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public static void main(String[] args) {
		// Chemin vers le fichier binaire 64K contenant Maze Master
		String binFilePath = "maze_master.bin";
		MazeLevel[] levels;
		try {
			byte[] data = MazeMasterViewer.class.getClassLoader().getResourceAsStream("org/pixs/mazemaster/maze_master.bin").readAllBytes();
			if (data.length != 65536) {
				throw new IllegalArgumentException("File must contain exactly 64K bytes.");
			}

			levels = MazeLevelParser.parseLevels(data);
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		SwingUtilities.invokeLater(() -> new MazeMasterViewer(levels));
	}
}
