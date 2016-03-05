/*
 * ColorChooserDialog.java
 *
 * Created on March 31, 2007, 3:46 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ColorChooserDialog extends JDialog implements ActionListener,
		PropertyChangeListener {

	/*
	 * public static String colorNames = { "Transparent", "Black", "Medium
	 * Green", "Light Green", "Dark Blue", "Light Blue", "Dark Red", "Cyan",
	 * "Medium Red", "Dark Red", "Dark Yellow", "Light Yellow", "Dark Green",
	 * "Magenta", "Gray", "White" };
	 */

	private static final long serialVersionUID = 1L;

	private Color chosenColor = JPixelButton.colors[0];

	private static Dimension defaultColorDimension = new Dimension(160, 16);

	private static Dimension pixelDimension = new Dimension(16, 16);

	/**
	 * Returns null if the typed string was invalid; otherwise, returns the
	 * string as the user entered it.
	 */
	public Color getChosenColor() {
		return chosenColor;
	}

	public static Color getColor(int i) {
		return JPixelButton.colors[i];
	}

	/** Creates the reusable dialog. */
	public ColorChooserDialog(Frame aFrame, Color defaultColor, int minimumColor) {
		super(aFrame, true);

		chosenColor = defaultColor;
		setTitle("Choose a color");

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel defaultPanel = new JPanel();
		final JPixelButton defaultColorButton = new JPixelButton(
				JPixelButton.STATES.FOREGROUND, defaultColor, "default",
				defaultColorDimension);
		defaultColorButton.addActionListener(this);
		defaultPanel.add(new JLabel("Current Color: "));
		defaultPanel.add(defaultColorButton);
		panel.add(defaultPanel);

		JPanel availableCaptionPanel = new JPanel();
		availableCaptionPanel.add(new JLabel("Available Colors:"));
		panel.add(availableCaptionPanel);

		JPanel availablePanel = new JPanel();
		for (int i = minimumColor; i < JPixelButton.colors.length; i++) {
			JPixelButton colorButton = new JPixelButton(
					JPixelButton.STATES.FOREGROUND, JPixelButton.colors[i],
					Integer.toString(i), pixelDimension);
			colorButton.addActionListener(this);
			availablePanel.add(colorButton);
		}
		panel.add(availablePanel);
		
		// Make this dialog display it.
		setContentPane(panel);
		
		//	ensure the dialog is sized properly
		pack();

		// Handle window closing correctly.
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				setVisible(false);
			}
		});

		// Ensure the existing color gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				defaultColorButton.requestFocusInWindow();
			}
		});
	}

	/** This method handles events for the color buttons field. */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JPixelButton) {
			JPixelButton button = (JPixelButton) e.getSource();
			chosenColor = button.getBackground();
			clearAndHide();
		}
	}

	/** This method reacts to state changes in the option pane. */
	public void propertyChange(PropertyChangeEvent e) {
		clearAndHide();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
	}
	
	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		setVisible(false);
	}
}
