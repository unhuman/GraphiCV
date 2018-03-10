package com.unhuman.graphicv;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/*
 * GraphiCV.java requires the following files:
 */

public class GraphiCV extends JPanel implements ActionListener, ItemListener, KeyListener, MouseListener {
	private static final long serialVersionUID = 1L;
	private static JFrame frame = null;
	private static boolean cvMode = false;
	private static boolean asmMode = false;
	private static String[] labelsDistinct = { "Top-Left", "Bottom-Left", "Top-Right", "Bottom-Right" };
	private static String[] labelsCombined = { "Full" };
	private static String[] labelsPreview = { "Sprite 1", "Sprite 2", "Sprite 3", "Sprite 4", "BG", "Combined" };
	protected TreeMap<String, JPixelButton> pixelButtonsMap = new TreeMap<String, JPixelButton>();
	protected TreeMap<String, JPixelButton> colorButtonsMap = new TreeMap<String, JPixelButton>();
	protected TreeMap<String, JTextField> quadrantHexMap = new TreeMap<String, JTextField>();
	protected static final String POPUP_COPY = "Copy To...";
	protected static final String POPUP_SWAP = "Swap With...";
	
	static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	// action buttons
	private JComboBox comboSelectSystem;
	private JComboBox comboDisplayLevel;
	private int lastSelectedDisplayLevel;
	private JCheckBox dualColorEnablerBox;
	private JButton resetPixelsButton;
	private JButton leftButton;
	private JButton rightButton;
	private JButton mirrorButton;
	private JButton flipButton;
	private JButton upButton;
	private JButton downButton;
	private JButton rotateClockButton;
	private JButton rotateCounterButton;
	private JButton invertPixelsButton;
	private JButton invertColorsButton;
	private JPixelButton previewBackgroundColorButton;
	private JPanel previewPanel;
	private JPanel previewPixelHolder[] = new JPanel[6];
	private JLabel[] previewLabels = new JLabel[6];
	private JPanel distinctGraphicsDescriptorsPanel;
	private JPanel combinedGraphicsDescriptorsPanel;
	private JTextField combinedGraphicsTextField;	

	// button related state
	private boolean dualColorsEnabled = false;
	private boolean rotateQuestionAsked = false;

	// matcher for pixels - pixel-quadrant-x-y
	private static final Pattern pixelPattern = Pattern.compile(JPixelButton.PIXEL_PREFIX + "([\\d]*)-([\\d]*)-([\\d]*)");

	// matcher for colors - quadrant-y-state
	private static final Pattern colorPattern = Pattern.compile(".*" + JPixelButton.COLOR_PREFIX + "([\\d]*)-([\\d]*)-(.*)");

	private static final String TI_99_XB = "TI-99 XB";
	private static final String TI_99_ASM = "TI-99 Asm";
	private static final String COLECOVISION = "ColecoVision";

	private static final String SPRITE_1 = "Sprite 1";
	private static final String SPRITE_2 = "Sprite 2";
	private static final String SPRITE_3 = "Sprite 3";
	private static final String SPRITE_4 = "Sprite 4";
	private static final String BACKGROUND = "Background";
	
	private StoredImageState[] storedImageStates = new StoredImageState[5];
	
	public GraphiCV() {
		JPanel graphicsPanel = new JPanel();
		graphicsPanel.setLayout(new BoxLayout(graphicsPanel, BoxLayout.Y_AXIS));

		JPanel systemSelectPanel = new JPanel();
		systemSelectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		systemSelectPanel.add(new JLabel("System: "));
		comboSelectSystem = new JComboBox();
		comboSelectSystem.addItem(TI_99_XB);
		comboSelectSystem.addItem(TI_99_ASM);
		comboSelectSystem.addItem(COLECOVISION);
		comboSelectSystem.addActionListener(this);
		systemSelectPanel.add(comboSelectSystem);
		graphicsPanel.add(systemSelectPanel);		
		
		// Graphics stuff (and Manipulation) lie at the top
		JPanel graphicsAreaPanel = new JPanel();
		graphicsAreaPanel.add(createPixelManager());
		graphicsPanel.add(graphicsAreaPanel);
		// put the text representations below
		graphicsPanel.add(createTextRepresentations());
		
		JPanel screenPanel = new JPanel();
		screenPanel.add(graphicsPanel);
		screenPanel.add(createManipulationButtonsAndPreview());
		
		// put everything on the screen
		add(screenPanel);

		calculateGraphics();
		enableColorsEntry(false, dualColorsEnabled);
	}

	// input is a string of text and number of sets of 16 we are allowing
	// TODO: Switch to regular expressions!
	private String getHexFromInput(String hexText, int validSets) {
		// trim off anything before an equals sign
		int trimCharLoc = hexText.indexOf("=");
		if (trimCharLoc >= 0)
			hexText = hexText.substring(trimCharLoc);

		// trim out anything before {
		trimCharLoc = hexText.indexOf("{");
		if (trimCharLoc >= 0)
			hexText = hexText.substring(trimCharLoc + 1);

		// trim out anything after last }
		trimCharLoc = hexText.lastIndexOf("}");
		if (trimCharLoc >= 0)
			hexText = hexText.substring(0, trimCharLoc);

		// now remove any 0x chars with nothing
		hexText = hexText.replaceAll("0*[xX]", "");

		// uppercase the hex string
		hexText = hexText.toUpperCase();

		// now we'll go through all the chars and get out the 0-9, A-F
		StringBuilder newHex = new StringBuilder(hexText.length());
		for (int i = 0; i < hexText.length(); i++) {
			char ch = hexText.charAt(i);
			if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))
				newHex.append(ch);
		}

		// Ensure we have a batches of 16
		if ((newHex.length() % 16) == 0) {
			// check the number of sets of 16 chars (per character) we've found
			int foundSets = newHex.length() / 16;
			if ((foundSets > 0) && (foundSets <= validSets)) {
				return newHex.toString();
			}
		}

		// we didn't get a match, so return nothing
		return null;
	}

	protected JLabel createSpacer(int horizSpace, int vertSpace) {
		JLabel label = new JLabel();
		Dimension spacerDimension = new Dimension(horizSpace, vertSpace);
		label.setSize(spacerDimension);
		label.setPreferredSize(spacerDimension);
		label.setMinimumSize(spacerDimension);
		label.setMaximumSize(spacerDimension);
		return label;
	}

	protected JLabel createSpacer(int space) {
		return createSpacer(space, space);
	}

	String generateHexName(String type, int quadrant) {
		return type + "-" + quadrant;
	}

	protected JPanel createPixelPanel() {
		return internalCreatePixelPanel(null);
	}
	
	protected JPanel createPreviewPanel(int previewNum) {
		return internalCreatePixelPanel(previewNum);
	}

	private JPanel internalCreatePixelPanel(Integer previewNum) {
		boolean isPreview = (previewNum != null);
		JPanel pixelPanel = new JPanel();
		// TODO: get this to display properly
		// getting rid of Gridlayout gives nice gaps
		if (isPreview)
			pixelPanel.setLayout(new GridLayout(2, 2));
		else {
			GridBagLayout layout = new GridBagLayout();
			pixelPanel.setLayout(layout);
		}

		// preview needs a border
		if (isPreview) {
			previewPixelHolder[previewNum] = pixelPanel;
			previewPixelHolder[previewNum].setBorder(new LineBorder(JPixelButton.WHITE));
		}
		pixelPanel.setBackground(JPixelButton.BLACK);

		// due to the way the GridLayout works (left to right, then down),
		// we do the crazy vert / horiz loops for quadrants
		for (int vert=0; vert<2; vert++) {
			for (int horiz=0; horiz<2; horiz++) { 				
				JPanel pixelHolder = new JPanel();
				pixelHolder.setLayout(new GridLayout(8, 8));
				
				for (int y = 0; y < 8; y++) {
					 
					for (int x = 0; x < 8; x++) {
						String buttonKey;
						int updatedX = x + horiz * 8;
						int updatedY = y + vert * 8;
						if (isPreview) 
							buttonKey = JPixelButton.generatePreviewName(previewNum, updatedX, updatedY);
						else 
							buttonKey = JPixelButton.generatePixelName(updatedX, updatedY);
						
						JPixelButton pixel = new JPixelButton(JPixelButton.STATES.BACKGROUND, 
															JPixelButton.WHITE, buttonKey, previewNum, updatedX, updatedY);
										
						if (!isPreview) {
							pixel.addActionListener(this);
							pixel.addKeyListener(this);
						} else {
							pixel.addMouseListener(this);
							// preview pixels have no border
							pixel.setBorder(null);
						}
						pixelHolder.add(pixel);
						pixelButtonsMap.put(buttonKey, pixel);					
					}
				}
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = horiz*2;
				c.gridy = vert*2;
				pixelPanel.add(pixelHolder, c);
			}
		}

		if (!isPreview) {
			GridBagConstraints cc = new GridBagConstraints();
			cc.gridx = 1;
			cc.gridy = 1;
			Dimension dim = new Dimension(2,2);
			Box.Filler spacer = new Box.Filler(dim, dim, dim);
			pixelPanel.add(spacer, cc);

			return pixelPanel;
		} else {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			previewLabels[previewNum] = new JLabel(labelsPreview[previewNum]);
			previewLabels[previewNum].addMouseListener(this);
			container.add(previewLabels[previewNum]);
			c.gridy = 1;
			container.add(pixelPanel, c);
			return container;
		}
	}

	protected JPanel createInternalColorPanel(String identifier, int items, int startingQuadrant) {
		JPanel colorHolder = new JPanel();
		GridLayout layout = new GridLayout(items, 2);
		colorHolder.setLayout(layout);

		for (int y = 0; y < items; y++) {
			for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
				int quadrant = startingQuadrant + y / 8;
				String buttonKey = JPixelButton.generateColorName(identifier, quadrant, y % 8, state);
				JPixelButton colorButton = new JPixelButton(state,
						(state == JPixelButton.STATES.FOREGROUND) ? JPixelButton.BLACK : JPixelButton.TRANSPARENT,
						buttonKey);
				colorButton.setToolTipText("This sets the " + ((state == JPixelButton.STATES.FOREGROUND) ? "foreground" : "background") + 
												" color for the " + ((identifier.equals(JPixelButton.MASTER_COLOR_PREFIX)) ? " entire image" : " row"));
				colorButton.addActionListener(this);
				colorHolder.add(colorButton);
				colorButtonsMap.put(buttonKey, colorButton);
			}
		}

		return colorHolder;
	}

	protected JPanel createColorPanel(int startingQuadrant) {
		return createInternalColorPanel(JPixelButton.COLOR_PREFIX, 16, startingQuadrant);
	}

	protected JPanel createMasterColorPanel() {
		return createInternalColorPanel(JPixelButton.MASTER_COLOR_PREFIX, 1, 0);
	}

	protected JPanel createPixelManager() {
		JPanel pixelManager = new JPanel();
		pixelManager.setLayout(new GridBagLayout());
		pixelManager.add(createMasterColorPanel());
		pixelManager.add(createSpacer(8));
		pixelManager.add(createColorPanel(0)); // color panel (quadrant 0-1)
		pixelManager.add(createSpacer(8));
		pixelManager.add(createPixelPanel());
		pixelManager.add(createSpacer(8));
		pixelManager.add(createColorPanel(2)); // color panel (quadrant 2-3)

		return pixelManager;
	}

	protected JPanel createGraphicsDescriptors(String type, String[] labels) {
		int numPairs = labels.length;

		// Create and populate the panel.
		Font fixedFont = new Font("monospaced", Font.PLAIN, 12);

		JPanel p = new JPanel(new SpringLayout());
		for (int i = 0; i < numPairs; i++) {
			JLabel l = new JLabel(labels[i] + ":", JLabel.TRAILING);
			p.add(l);
			JTextField textField = new JTextField(12);
			textField.setFont(fixedFont);
			String hexName = generateHexName(type, i);
			textField.setName(hexName);
			textField.addActionListener(this);
			
			if (labels == labelsCombined) {
				combinedGraphicsTextField = textField;
			}
			
			// textField.setEditable(false);
			l.setLabelFor(textField);
			p.add(textField);
			quadrantHexMap.put(hexName, textField);
		}

		// Lay out the panel.
		SpringUtilities.makeCompactGrid(p, numPairs, 2, // rows, cols
				6, 6, // initX, initY
				6, 6); // xPad, yPad
		return p;
	}

	protected JPanel createTextRepresentations() {
		JPanel textManager = new JPanel();
		textManager.setLayout(new BoxLayout(textManager, BoxLayout.Y_AXIS));
		JPanel graphicsLabelPanel = new JPanel();
		graphicsLabelPanel.add(new JLabel("Graphics"));
		textManager.add(graphicsLabelPanel);
		distinctGraphicsDescriptorsPanel = createGraphicsDescriptors("graphics", labelsDistinct);
		textManager.add(distinctGraphicsDescriptorsPanel);
		combinedGraphicsDescriptorsPanel = createGraphicsDescriptors("graphicsCombined", labelsCombined);
		textManager.add(combinedGraphicsDescriptorsPanel);
		textManager.add(new JLabel("Color Maps"));
		textManager.add(createGraphicsDescriptors("colors", labelsDistinct));

		return textManager;
	}

	private ImageIcon getImageIcon(String imagePath) {
		URL resource = getClass().getResource(imagePath);
		if (resource != null) {
			return new ImageIcon(resource);
		} else
			return new ImageIcon(imagePath);
	}
	
	protected JPanel createManipulationButtonsAndPreview() {
		JPanel buttonManager = new JPanel();
		GridBagLayout buttonBag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		buttonManager.setLayout(buttonBag);
		
		c.weightx = 1.0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		
		JPanel graphicSelectPanel = new JPanel();
		graphicSelectPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		graphicSelectPanel.add(new JLabel("Level: "));
		comboDisplayLevel = new JComboBox();
		comboDisplayLevel.addItem(SPRITE_1);
		comboDisplayLevel.addItem(SPRITE_2);
		comboDisplayLevel.addItem(SPRITE_3);
		comboDisplayLevel.addItem(SPRITE_4);
		comboDisplayLevel.addItem(BACKGROUND);
		comboDisplayLevel.setSelectedIndex(comboDisplayLevel.getItemCount()-1);
		lastSelectedDisplayLevel = comboDisplayLevel.getSelectedIndex();
		for (int i=0; i<comboDisplayLevel.getItemCount(); i++) {
			storedImageStates[i] = new StoredImageState();
		}
		comboDisplayLevel.addActionListener(this);
		graphicSelectPanel.add(comboDisplayLevel);
		// checkbox to indicate if right buttons are same as left buttons
		dualColorEnablerBox = new JCheckBox("Enable different colors");
		dualColorEnablerBox.setToolTipText("Selecting this will allow each 8x8 character to have its own colors");
		dualColorEnablerBox.addItemListener(this);
		graphicSelectPanel.add(dualColorEnablerBox);
		buttonBag.setConstraints(graphicSelectPanel, c);
		buttonManager.add(graphicSelectPanel);

		c.gridy++;
		
		// Action buttons
		
		EmptyBorder noBorder = new EmptyBorder(0,0,0,0);
		ImageIcon buttonImage;
		
		// panel for Reset / Inverse 
		JPanel buttonRowManager = new JPanel();
		buttonRowManager.setBorder(noBorder);
		buttonRowManager.setLayout(new FlowLayout(FlowLayout.LEFT));		
		buttonImage = getImageIcon("resource/images/erase.gif");
		resetPixelsButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Reset"));
		resetPixelsButton.setToolTipText("Reset Pixels");
		resetPixelsButton.addActionListener(this);
		buttonRowManager.add(resetPixelsButton);
		buttonImage = getImageIcon("resource/images/invertPixels.gif");
		invertPixelsButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Invert Pixels"));
		invertPixelsButton.setToolTipText("Invert Pixels");
		invertPixelsButton.addActionListener(this);
		buttonRowManager.add(invertPixelsButton);		
		buttonImage = getImageIcon("resource/images/invertColors.gif");
		invertColorsButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Invert Colors"));
		invertColorsButton.setToolTipText("Invert Colors");
		invertColorsButton.addActionListener(this);
		buttonRowManager.add(invertColorsButton);		
		buttonBag.setConstraints(buttonRowManager, c);	
		buttonManager.add(buttonRowManager);
		
		c.gridy++;
		
		// Panel for Mirror / Flip
		buttonRowManager = new JPanel();
		buttonRowManager.setLayout(new FlowLayout(FlowLayout.LEFT));
		buttonRowManager.setBorder(noBorder);
		buttonImage = getImageIcon("resource/images/left.gif");
		leftButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("<-- Push Left"));
		leftButton.setToolTipText("Push Left");
		leftButton.addActionListener(this);
		buttonRowManager.add(leftButton);
		buttonImage = getImageIcon("resource/images/right.gif");
		rightButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Push Right -->"));
		rightButton.setToolTipText("Push Right");
		rightButton.addActionListener(this);
		buttonRowManager.add(rightButton);		
		buttonImage = getImageIcon("resource/images/up.gif");
		upButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Push Up"));
		upButton.setToolTipText("Push Up");
		upButton.addActionListener(this);
		buttonRowManager.add(upButton);		
		buttonImage = getImageIcon("resource/images/down.gif");
		downButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Push Down"));
		downButton.setToolTipText("Push Down");
		downButton.addActionListener(this);
		buttonRowManager.add(downButton);		
		buttonBag.setConstraints(buttonRowManager, c);	
		buttonManager.add(buttonRowManager);
		
		c.gridy++;
		
		// Panel for Rotate Clockwise / Counter-Clockwise
		buttonRowManager = new JPanel();
		buttonRowManager.setLayout(new FlowLayout(FlowLayout.LEFT));
		buttonRowManager.setBorder(noBorder);
		buttonImage = getImageIcon("resource/images/mirror.gif");
		mirrorButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("<-- Mirror -->"));
		mirrorButton.setToolTipText("Mirror");
		mirrorButton.addActionListener(this);
		buttonRowManager.add(mirrorButton);		
		buttonImage = getImageIcon("resource/images/flip.gif");
		flipButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Flip"));
		flipButton.setToolTipText("Flip");
		flipButton.addActionListener(this);
		buttonRowManager.add(flipButton);
		buttonImage = getImageIcon("resource/images/clock.gif");
		rotateClockButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("Rotate -->"));
		rotateClockButton.setToolTipText("Rotate Clockwise");
		rotateClockButton.addActionListener(this);
		buttonRowManager.add(rotateClockButton);
		buttonImage = getImageIcon("resource/images/counter.gif");
		rotateCounterButton = ((buttonImage.getIconHeight()>0) ? new JButton(buttonImage) : new JButton("<-- Rotate"));
		rotateCounterButton.setToolTipText("Rotate Counter-Clockwise");
		rotateCounterButton.addActionListener(this);
		buttonRowManager.add(rotateCounterButton);
		buttonBag.setConstraints(buttonRowManager, c);	
		buttonManager.add(buttonRowManager);

		c.gridy++;
		JLabel previewSpacerLabel = new JLabel("");
		previewSpacerLabel.setPreferredSize(new Dimension(1,100));
		buttonBag.setConstraints(previewSpacerLabel, c);
		buttonManager.add(previewSpacerLabel);		
		
		c.gridy++;
		JLabel previewLabel = new JLabel("Preview BG Color:"); 
		buttonBag.setConstraints(previewLabel, c);
		buttonManager.add(previewLabel);		
		
		// preview color
		c.gridy++;
		previewBackgroundColorButton = new JPixelButton(
				JPixelButton.STATES.FOREGROUND, JPixelButton.CYAN, "previewBackground",
				new Dimension(140, 16));
		previewBackgroundColorButton.addActionListener(this);
		buttonBag.setConstraints(previewBackgroundColorButton, c);
		buttonManager.add(previewBackgroundColorButton);

		// spacer between preview color and pane
		c.gridy++;
		JLabel spacer = createSpacer(4);
		buttonBag.setConstraints(spacer, c);
		buttonManager.add(spacer);
		
		
		// preview pane
		c.gridy++;
		previewPanel = new JPanel();
		previewPanel.setBorder(new LineBorder(JPixelButton.WHITE));
		previewPanel.setBackground(JPixelButton.CYAN);
		buttonBag.setConstraints(previewPanel, c);
		for (int i=0; i<6; i++) {
			JPanel levelPreview = createPreviewPanel(i);
			previewPanel.add(levelPreview);
		}

		buttonManager.add(previewPanel);

		// spacer between pane & sets
		c.gridy++;
		JLabel spacer2 = createSpacer(16);
		buttonBag.setConstraints(spacer2, c);
		buttonManager.add(spacer2);
		
		
		// sets
		c.gridy++;
		JLabel setsLabel = new JLabel("Sets:");
		buttonBag.setConstraints(setsLabel, c);
		buttonManager.add(setsLabel, c);
		
		
		return buttonManager;
	}

	protected void pixelColorChange(JPixelButton colorButton) {
		JPixelButton.STATES colorState = colorButton.getState();
		Color newColor = colorButton.getBackground();

		Matcher colorMatcher = colorPattern.matcher(colorButton.getName());
		if (colorMatcher.matches()) {
			// determine the color to use from the color panel
			int quadrant = Integer.parseInt(colorMatcher.group(1));
			int y = Integer.parseInt(colorMatcher.group(2));
			int endQuadrant = quadrant + (((quadrant >= 2) || (dualColorsEnabled)) ? 0 : 2);
			for (int q = quadrant; q <= endQuadrant; q += 2) {
				for (int x = 0; x < 8; x++) {
					String pixelName = JPixelButton.generatePixelName(q, x, y);
					JPixelButton pixelButton = pixelButtonsMap.get(pixelName);
					if (pixelButton.getState() == colorState) {
						pixelButton.setColor(newColor);
					}
				}
			}
		}
	}

	protected void rowColorChange(JPixelButton colorButton) {
		JPixelButton.STATES colorState = colorButton.getState();
		Color newColor = colorButton.getBackground();

		Matcher colorMatcher = colorPattern.matcher(colorButton.getName());
		if (colorMatcher.matches()) {
			for (int q = 0; q < 4; q++) {
				for (int y = 0; y < 8; y++) {
					String colorPixel = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, colorState);
					JPixelButton color = colorButtonsMap.get(colorPixel);
					color.setBackground(newColor);
					pixelColorChange(color);
				}
			}
		}
	}

	protected void inverseColors() {
		// flip the master colors
		String colorPixel = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.FOREGROUND);
		JPixelButton fg = colorButtonsMap.get(colorPixel);
		colorPixel = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.BACKGROUND);
		JPixelButton bg = colorButtonsMap.get(colorPixel);
		Color oldFgColor = fg.getBackground();
		fg.setColor(bg.getBackground());
		bg.setColor(oldFgColor);

		// flip the quadrant/line-based settings
		for (int q = 0; q < 4; q++) {
			for (int y = 0; y < 8; y++) {
				colorPixel = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, JPixelButton.STATES.FOREGROUND);
				fg = colorButtonsMap.get(colorPixel);
				colorPixel = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, JPixelButton.STATES.BACKGROUND);
				bg = colorButtonsMap.get(colorPixel);

				oldFgColor = fg.getBackground();
				fg.setColor(bg.getBackground());
				bg.setColor(oldFgColor);

				pixelColorChange(fg);
				pixelColorChange(bg);
			}

			calculateQuadrantColors(q);
		}
	}

	protected void inversePixels() {
		for (int y = 0; y < 16; y++) {
			// flip pixels
			for (int x = 0; x < 16; x++) {
				String buttonName = JPixelButton.generatePixelName(x, y);
				JPixelButton pixel = pixelButtonsMap.get(buttonName);
				JPixelButton.STATES newState = pixel.getState().equals(JPixelButton.STATES.FOREGROUND) ? JPixelButton.STATES.BACKGROUND : JPixelButton.STATES.FOREGROUND;
				String colorName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y, newState);
				Color newColor = colorButtonsMap.get(colorName).getBackground();

				pixel.setStateAndColor(newState, newColor);
			}
		}

		calculateGraphics();
	}	
	
	protected void horiz(int pixelDir) {
		int startX = (pixelDir == 1) ? 15 : 0;
		int endX = (pixelDir == 1) ? 0 : 15;
		for (int y=0; y<16; y++) {
			for (int x=startX; x != endX; x-=pixelDir) {
				String pixelKey = JPixelButton.generatePixelName(x, y);
				JPixelButton pixelTo = pixelButtonsMap.get(pixelKey);
				
				pixelKey = JPixelButton.generatePixelName((x-pixelDir), y); 
				JPixelButton pixelFrom = pixelButtonsMap.get(pixelKey);
			
				JPixelButton.STATES pixelState = (pixelFrom != null) ? pixelFrom.getState() : JPixelButton.STATES.BACKGROUND;				
				Color pixelColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y, pixelState)).getColor();
				pixelTo.setStateAndColor(pixelState, pixelColor);
			}
			
			String pixelKey = JPixelButton.generatePixelName(endX, y);
			JPixelButton pixelTo = pixelButtonsMap.get(pixelKey);
			Color pixelColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.BACKGROUND)).getColor();
			pixelTo.setStateAndColor(JPixelButton.STATES.BACKGROUND, pixelColor);
		}
		
		calculateGraphics();
	}

	protected void vert(int pixelDir) {
		int startY = (pixelDir == 1) ? 15 : 0;
		int endY = (pixelDir == 1) ? 0 : 15;
		
		for (int q = 0; q <= 2; q+=2) {
			for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
				for (int y = startY; y != endY; y-=pixelDir) {
					String toPixelName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y / 8 + q, y % 8, state);
					JPixelButton toColor = colorButtonsMap.get(toPixelName);
					String fromPixelName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, (y-pixelDir) / 8 + q, (y-pixelDir) % 8, state);
					JPixelButton fromColor = colorButtonsMap.get(fromPixelName);
	
					toColor.setBackground(fromColor.getBackground());
				}
				String backgroundColorName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, state);
				JPixelButton backgroundColor = colorButtonsMap.get(backgroundColorName);
				String toPixelName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, endY / 8 + q, endY % 8, state);
				JPixelButton toColor = colorButtonsMap.get(toPixelName);				
				toColor.setBackground(backgroundColor.getColor()); 
			}
		}
		
		for (int x=0; x < 16; x++) {
			for (int y=startY; y != endY; y-=pixelDir) {			
				String pixelKey = JPixelButton.generatePixelName(x, y);
				JPixelButton pixelTo = pixelButtonsMap.get(pixelKey);
				
				pixelKey = JPixelButton.generatePixelName(x, y-pixelDir); 
				JPixelButton pixelFrom = pixelButtonsMap.get(pixelKey);
			
				JPixelButton.STATES pixelState = (pixelFrom != null) ? pixelFrom.getState() : JPixelButton.STATES.BACKGROUND;				
				Color pixelColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y, pixelState)).getColor();
				pixelTo.setStateAndColor(pixelState, pixelColor);
			}
			
			String pixelKey = JPixelButton.generatePixelName(x, endY);
			JPixelButton pixelTo = pixelButtonsMap.get(pixelKey);
			Color pixelColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, endY, JPixelButton.STATES.BACKGROUND)).getColor();
			pixelTo.setStateAndColor(JPixelButton.STATES.BACKGROUND, pixelColor);
		}
		
		calculateGraphics();
	}	
	
	protected void mirror() {
		for (int y = 0; y < 16; y++) {
			// switch the color selection pixels
			for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
				String leftSidePixelName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y / 8, y % 8, state);
				JPixelButton leftColor = colorButtonsMap.get(leftSidePixelName);
				String rightSidePixelName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y / 8 + 2, y % 8, state);
				JPixelButton rightColor = colorButtonsMap.get(rightSidePixelName);

				Color oldRightColor = rightColor.getBackground();
				rightColor.setBackground(leftColor.getBackground());
				leftColor.setBackground(oldRightColor);
			}

			// switch the image around too
			for (int x = 0; x < 8; x++) {
				int quadrant = (y / 8) + ((x / 8) * 2);
				String buttonName = JPixelButton.generatePixelName(quadrant, x % 8, y % 8);
				JPixelButton leftPixel = pixelButtonsMap.get(buttonName);
				buttonName = JPixelButton.generatePixelName(quadrant + 2, 7 - x % 8, y % 8);
				JPixelButton rightPixel = pixelButtonsMap.get(buttonName);

				JPixelButton.STATES newLeftState = rightPixel.getState();
				Color newLeftColor = rightPixel.getBackground();

				rightPixel.setStateAndColor(leftPixel.getState(), leftPixel.getBackground());
				leftPixel.setStateAndColor(newLeftState, newLeftColor);
			}
		}

		calculateGraphics();
	}

	protected void flip() {
		for (int y = 0; y < 8; y++) {
			// flip pixels
			for (int x = 0; x < 16; x++) {
				int quadrant = (y / 8) + ((x / 8) * 2);
				String buttonName = JPixelButton.generatePixelName(quadrant, x % 8, y % 8);
				JPixelButton topPixel = pixelButtonsMap.get(buttonName);
				buttonName = JPixelButton.generatePixelName(quadrant + 1, x % 8, 7 - y % 8);
				JPixelButton bottomPixel = pixelButtonsMap.get(buttonName);

				JPixelButton.STATES newTopState = bottomPixel.getState();
				Color newTopColor = bottomPixel.getBackground();

				bottomPixel.setStateAndColor(topPixel.getState(), topPixel.getBackground());
				topPixel.setStateAndColor(newTopState, newTopColor);
			}

			// flip colors too
			for (JPixelButton.STATES stateFlip : JPixelButton.STATES.values()) {
				for (int q = 0; q < 4; q += 2) {
					String colorPixel = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, stateFlip);
					JPixelButton top = colorButtonsMap.get(colorPixel);
					colorPixel = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q + 1, 7 - y, stateFlip);
					JPixelButton bottom = colorButtonsMap.get(colorPixel);

					Color oldTopColor = top.getBackground();
					top.setBackground(bottom.getBackground());
					bottom.setBackground(oldTopColor);
				}
			}
		}

		calculateGraphics();
	}

	protected void calculateGraphics() {
		// recalculate the pixels & colors for the image
		String combinedGraphicText = "";
		for (int q = 0; q < 4; q++) {
			combinedGraphicText += calculateQuadrantPixels(q);
			calculateQuadrantColors(q);
		}
		// fix CV display
		combinedGraphicText = combinedGraphicText.replace("}{", ", ");
		combinedGraphicsTextField.setText(combinedGraphicText);
		combinedGraphicsTextField.setCaretPosition(0);
	}

	protected boolean colorsDiffer() {
		for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
			Color baseColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, 0, 0, state)).getColor();
			for (int q=0; q < 4; q++) {
				for (int y = 0; y < 8; y++) {
					Color checkColor = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, state)).getColor();
					if (checkColor != baseColor)
						return true;
				}
			}
		}
		return false;
	}
	
	protected void swapPixels(JPixelButton pixel1, JPixelButton pixel2) {
		JPixelButton.STATES new1State = pixel2.getState();
		Color new1Color = pixel2.getBackground();

		pixel2.setStateAndColor(pixel1.getState(), pixel1.getBackground());
		pixel1.setStateAndColor(new1State, new1Color);
	}
	
	protected void cornerToCorner() {		
		// copy display into memory buffer
		boolean rotated[][] = new boolean[16][16];
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				String pixelKey = JPixelButton.generatePixelName(x, y);
				JPixelButton pixel = pixelButtonsMap.get(pixelKey);
				boolean selected = (pixel.getState() == JPixelButton.STATES.FOREGROUND);
				rotated[15 - y][15 - x] = selected;
			}
		}

		// determine colors
		String buttonName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.FOREGROUND);
		JPixelButton fgColor = colorButtonsMap.get(buttonName);
		buttonName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.BACKGROUND);
		JPixelButton bgColor = colorButtonsMap.get(buttonName);		
		
		// copy memory buffer back into display
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				String pixelKey = JPixelButton.generatePixelName(x, y);
				JPixelButton pixel = pixelButtonsMap.get(pixelKey);
				boolean selected = rotated[x][y];

				pixel.setStateAndColor((selected) ? JPixelButton.STATES.FOREGROUND : JPixelButton.STATES.BACKGROUND,
						(selected) ? fgColor.getBackground() : bgColor.getBackground());
			}
		}
	}
	
	protected void rotate(boolean clockwise) {
		int n = 0;
		//boolean changeColors = false;
		if (colorsDiffer()) {
			//changeColors = true;
			if (!rotateQuestionAsked) {
				rotateQuestionAsked = true;
				Object[] options = { "Continue", "Cancel" };
				n = JOptionPane.showOptionDialog(frame,
						"Rotating an image will reset the colors.\nThis question will only be asked once.", "Rotate Question",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			}
		}
		
		// only perform the rotate if it's acceptable
		if (n == 0) {
			// If we're editing a sprite, don't reset the colors
			if (comboDisplayLevel.getSelectedItem().equals(BACKGROUND)) {
				// reset the foreground color
				String buttonName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.FOREGROUND);
				JPixelButton colorChange = colorButtonsMap.get(buttonName);
				processButtonColorChange(colorChange);
	
				// reset the background color
				buttonName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.BACKGROUND);
				colorChange = colorButtonsMap.get(buttonName);
				processButtonColorChange(colorChange);
			}
			
			// cheating - rotate clockwise = inverse & then a flip
			// counterclockwise = 3 * clockwise
			for (int rotateCount = 0; rotateCount < ((clockwise) ? 1 : 3); rotateCount++) {
				// perform the rotation
				cornerToCorner();
				flip();
			}
		}
	}

	protected void resetAll() {
		int backupIndex = comboDisplayLevel.getSelectedIndex();
		
		for (int i=0; i<comboDisplayLevel.getItemCount(); i++) {
			comboDisplayLevel.setSelectedIndex(i);
			reset();
		}
		
		comboDisplayLevel.setSelectedIndex(backupIndex);
	}
	
	protected void reset() {
		JPixelButton pixelReset;
		for (int y = 0; y < 16; y++) {
			String colorButton = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, y / 8, y % 8, JPixelButton.STATES.BACKGROUND);
			Color bgColor = colorButtonsMap.get(colorButton).getBackground();
			for (int x = 0; x < 16; x++) {
				String pixelName = JPixelButton.generatePixelName(x, y);
				pixelReset = pixelButtonsMap.get(pixelName);
				pixelReset.setStateAndColor(JPixelButton.STATES.BACKGROUND, bgColor);
			}
		}

		calculateGraphics();
	}

	protected void parseGraphicsText(int maxQuadrants, int startingQuad, String graphicText) {
		// Clean off funny characters
		// TODO : IMPROVE!!!
		graphicText = graphicText.replaceAll("(0x|[{},>\\s])", "");
		int chNum = 0;
		do {
			if (startingQuad > 3)
				return;
			
			for (int y = 0; y < 8; y++) {
				Color fg = colorButtonsMap.get(
						JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, startingQuad, y % 8, JPixelButton.STATES.FOREGROUND)).getBackground();
				Color bg = colorButtonsMap.get(
						JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, startingQuad, y % 8, JPixelButton.STATES.BACKGROUND)).getBackground();
				for (int xBase = 0; xBase < 8; xBase += 4) {
					int nibbleValue = Character.digit(graphicText.charAt(chNum++), 16);
					for (int pixelItem = 0; pixelItem < 4; pixelItem++) {
						int pixelCheckValue = (int) Math.pow(2, 3 - pixelItem);
						JPixelButton.STATES state = (nibbleValue >= pixelCheckValue) ? JPixelButton.STATES.FOREGROUND
								: JPixelButton.STATES.BACKGROUND;
						if (state == JPixelButton.STATES.FOREGROUND)
							nibbleValue -= pixelCheckValue;
						String pixelName = JPixelButton.generatePixelName(startingQuad, xBase + pixelItem, y);
						JPixelButton pixelButton = pixelButtonsMap.get(pixelName);
						pixelButton.setStateAndColor(state, (state == JPixelButton.STATES.FOREGROUND) ? fg : bg);
					}
				}
			}
			calculateQuadrantPixels(startingQuad);
			startingQuad++;
		} while (chNum < graphicText.length());		
	}
	
	protected void handleGraphicsInput(JTextField graphicField) {
		String nameInput = graphicField.getName();
		int startingQuadrant = Integer.parseInt(nameInput.substring(nameInput.indexOf('-') + 1));
		int validPackages = 4 - startingQuadrant;
		String validHex = getHexFromInput(graphicField.getText(), validPackages);

		if (validHex != null) {
			parseGraphicsText(validPackages, startingQuadrant, validHex);
			calculateGraphics();
		}
		graphicField.setBackground((validHex != null) ? Color.WHITE : Color.YELLOW);
	}

	protected void handleColorsInput(JTextField colorField) {
		String nameInput = colorField.getName();
		int startingQuadrant = Integer.parseInt(nameInput.substring(nameInput.indexOf('-') + 1));
		int validPackages = 4 - startingQuadrant;
		String validHex = getHexFromInput(colorField.getText(), validPackages);
		if (validHex != null) {			
			// determine if our colors are going over to the other side so
			// we can enforce different side colors to be turned on
			int charCount = validHex.length()/16;
			int lastQuadrant = startingQuadrant + charCount - 1; 
			if (lastQuadrant >= 2) {
				dualColorEnablerBox.setSelected(true);
			}

			int chNum = 0;
			for (int q = startingQuadrant; q<=lastQuadrant; q++) {
				for (int y = 0; y < 8; y++) {
					for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
						JPixelButton colorChange = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, state));
						int nibbleValue = Character.digit(validHex.charAt(chNum++), 16);
						colorChange.setBackground(JPixelButton.colors[nibbleValue]);
						pixelColorChange(colorChange);
						
						// Place colors on both sides if not enabled boths sides
						if (!dualColorEnablerBox.isSelected()) {
							colorChange = colorButtonsMap.get(JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q+2, y, state));
							colorChange.setBackground(JPixelButton.colors[nibbleValue]);
							pixelColorChange(colorChange);
						}
					}
				}
				calculateQuadrantColors(q);
				if (!dualColorEnablerBox.isSelected())
					calculateQuadrantColors(q+2);
			}
		}
		colorField.setBackground((validHex != null) ? Color.WHITE : Color.YELLOW);
	}

	protected void enableColorsEntry(boolean allElseRight, boolean enable) {
		// alter the colors based on enabled
		int start = (allElseRight) ? 0 : 2;
		for (int q = start; q < 4; q++) {
			for (int y = 0; y < 8; y++) {
				for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
					String buttonName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q, y, state);
					JPixelButton colorButton = colorButtonsMap.get(buttonName);
					colorButton.setVisible(enable);

					// copy colors from left to right
					if (!enable && !allElseRight) {
						String copyButtonName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, q - 2, y, state);
						JPixelButton copyColorButton = colorButtonsMap.get(copyButtonName);
						Color copyColor = copyColorButton.getBackground();
						colorButton.setBackground(copyColor);

						// update the colors of the image
						pixelColorChange(colorButton);
					}
				}
			}

			// if the colors got swapped,
			// we need to update what's displayed
			if (!enable)
				calculateQuadrantColors(q);

			// enable textfield based on extra colors enabled
			quadrantHexMap.get(generateHexName("colors", q)).setEnabled(enable);
		}
	}

	// check boxes listener - dual colors
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source == dualColorEnablerBox) {
			dualColorsEnabled = e.getStateChange() != ItemEvent.DESELECTED;
			enableColorsEntry(false, dualColorsEnabled);
		}
	}

	protected void processButtonColorChange(JPixelButton button) {
		// These actions differ how they pass their colors down
		if (button.isColorButton()) {
			// if we are not supporting both colors sides
			// the right color needs to match the left color changed
			if (!dualColorsEnabled) {
				Matcher colorMatcher = colorPattern.matcher(button.getName());
				if (colorMatcher.matches()) {
					Color colorChosen = button.getBackground();
					// determine the color to use from the color panel
					int quadrant = Integer.parseInt(colorMatcher.group(1));
					int y = Integer.parseInt(colorMatcher.group(2));
					String mode = colorMatcher.group(3);
					String name = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, quadrant + 2, y, mode);
					JPixelButton otherButton = colorButtonsMap.get(name);
					otherButton.setBackground(colorChosen);
				}
			}
			// now, we need to modify all the pixelButtons in the row
			// altered
			pixelColorChange(button);
		} else {
			// Change the colors for all the rows, which will change
			// all the pixels
			rowColorChange(button);
		}

		// Calculate all the colors for the quadrants
		for (int q = 0; q < 4; q++) {
			calculateQuadrantColors(q);
		}
	}

	protected void performColorSelection(JPixelButton button, int minimumColor) {
		// obtain the color from the item selected as a default
		Color colorChosen = button.getBackground();
		com.unhuman.graphicv.ColorChooserDialog colorChooserDialog = new com.unhuman.graphicv.ColorChooserDialog(frame, colorChosen, minimumColor);
		int x = button.getLocationOnScreen().x;
		int y = button.getLocationOnScreen().y;
		// ensure the color chooser is shown nicely, relative to parent
		if ((x + button.getSize().width + colorChooserDialog.getWidth()) > (getLocationOnScreen().x + getWidth()))
			x = button.getLocationOnScreen().x - colorChooserDialog.getWidth();
		else 
			x += button.getSize().width;
		colorChooserDialog.setBounds(x, y, colorChooserDialog.getWidth(), colorChooserDialog.getHeight());
		colorChooserDialog.setVisible(true);
		// now set our color to the item selected
		colorChosen = colorChooserDialog.getChosenColor();
		button.setBackground(colorChosen);

		processButtonColorChange(button);
		redrawPreview(null);
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		Object source = e.getSource();
		if (source instanceof JPixelButton) {
			int xAdd = 0;
			int yAdd = 0;
			int keyCode = e.getKeyCode();
			switch (keyCode) {
			case KeyEvent.VK_LEFT:
				xAdd = -1;
				break;
			case KeyEvent.VK_RIGHT:
				xAdd = 1;
				break;
			case KeyEvent.VK_UP:
				yAdd = -1;
				break;
			case KeyEvent.VK_DOWN:
				yAdd = 1;
				break;
			default:
				return;
			}
			JPixelButton button = (JPixelButton) e.getSource();
			String name = button.getName();
			if (name.startsWith(JPixelButton.PIXEL_PREFIX)) {
				// determine the button name
				Matcher pixelMatcher = pixelPattern.matcher(name);
				// determine the color to use from the color panel
				if (!pixelMatcher.matches())
					return;

				int quadrant = Integer.parseInt(pixelMatcher.group(1));
				int x = Integer.parseInt(pixelMatcher.group(2));
				int y = Integer.parseInt(pixelMatcher.group(3));

				// move things around
				x += xAdd;
				if ((x == 8) || (x < 0)) {
					quadrant = (quadrant + 2) % 4;
					x = (x < 0) ? 7 : 0;
				}
				y += yAdd;
				if ((y == 8) || (y < 0)) {
					int startQuad = (quadrant / 2) * 2;
					quadrant = (quadrant + 1) % 2 + startQuad;
					y = (y < 0) ? 7 : 0;
				}

				// generate the new button name
				button = pixelButtonsMap.get(JPixelButton.generatePixelName(quadrant, x, y));
			} else if (name.startsWith(JPixelButton.COLOR_PREFIX)) {

			} else if (name.startsWith(JPixelButton.MASTER_COLOR_PREFIX)) {
	
			}

			// set focus to whatever we have found (or leave alone)
			button.grabFocus();
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	public Color getColorForPixel(JPixelButton pixelButton, JPixelButton.STATES state) {
		Color retColor = null;
		Matcher pixelMatcher = pixelPattern.matcher(pixelButton.getName());
		if (pixelMatcher.matches()) {
			// determine the color to use from the color panel
			int quadrant = Integer.parseInt(pixelMatcher.group(1));
			int y = Integer.parseInt(pixelMatcher.group(3));

			retColor = colorButtonsMap.get(
				JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, quadrant, y, state)).getBackground();
		}
		return retColor;
	}
	
	protected void redrawPreview(Color previewBgColor) {
		if (previewBgColor != null) {
			previewPanel.setBackground(previewBgColor);
			Color xorColor = new Color(255 - previewBgColor.getRed(), 255 - previewBgColor.getGreen(), 255 - previewBgColor.getBlue());
			LineBorder xorBorder = new LineBorder(xorColor);
			for (int i=0; i<6; i++) {
				previewPixelHolder[i].setBorder(xorBorder);
				previewLabels[i].setForeground(xorColor);
				previewLabels[i].getParent().setBackground(previewBgColor);
			}
			previewPanel.setBorder(xorBorder);
		}
		
		int backupDisplayLevel = comboDisplayLevel.getSelectedIndex();
		// loop through all the pixels and redraw them in the preview
		for (int previewPanel=0; previewPanel<5; previewPanel++) {
			changeDisplayLevel(previewPanel);
			for (int quadrant=0; quadrant<4; quadrant++) {
				for (int y = 0; y < 8; y++) {
					for (int x = 0; x < 8; x++) {
						colorPreviewButton(pixelButtonsMap.get(JPixelButton.generatePixelName(quadrant, x, y)), previewPanel);
					}
				}
			}
		}
		changeDisplayLevel(backupDisplayLevel);
	}

	protected void updateCombinedPreview() {
		for (int x=0; x<16; x++) {
			for (int y=0; y<16; y++) {
				updateCombinedPreview(x, y);
			}
		}
	}
	
	protected void updateCombinedPreview(int x, int y) {
		// goes through and finds out what color to draw pixel as
		Color useColor = previewBackgroundColorButton.getColor();
		int backgroundIndex = comboDisplayLevel.getItemCount()-1;
		for (int i = backgroundIndex; i>=0; i--) {
			
			// getting color / status a bit different if we are working on the layer
			JPixelButton pixel = null;
			boolean selected;
			if (i==comboDisplayLevel.getSelectedIndex()) {
				String pixelKey = JPixelButton.generatePixelName(x, y);		
				pixel = pixelButtonsMap.get(pixelKey);
				selected = pixel.getState().equals(JPixelButton.STATES.FOREGROUND);
			} else {
				// not current display layer - get from stored layer
				String pixelKey = JPixelButton.generatePreviewName(i, x, y);		
				pixel = pixelButtonsMap.get(pixelKey);
				selected = storedImageStates[i].getPixelSelected(x, y);
				
				// drawing sprites - change selected in case sprite color is transparent.  Unfortunate.
				if (i != backgroundIndex) {
					Color spriteColor = storedImageStates[i].master[0];
					if (spriteColor == JPixelButton.TRANSPARENT)
						selected = false;
				}
			}
			
			if (i == backgroundIndex) {
				// draw all pixels but transparent from background
				if (pixel.getColor() != JPixelButton.TRANSPARENT) {
					useColor = pixel.getColor();
				}
			} else {
				// draw sprites over top only if selected pixel (and not transparent)
				if (selected && (pixel.getColor() != JPixelButton.TRANSPARENT)) {
					useColor = pixel.getColor();
				}
			}
		}
		
		String combinedPixelKey = JPixelButton.generatePreviewName(5, x, y);
		JPixelButton combinedPixel = pixelButtonsMap.get(combinedPixelKey);
		combinedPixel.setColor(useColor);
	}
	
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source instanceof JPixelButton) {
			JPixelButton button = (JPixelButton) e.getSource();
			if (button == previewBackgroundColorButton){
				performColorSelection(button, 1);
				// redraw the preview stuff
				Color newBgColor = button.getBackground();
				redrawPreview(newBgColor);				
			} else if (button.isPixelButton()) {
				//	this check makes it so single pixels work by spacebar and mouseclick
				if (!button.isCapturing()) {
					// determine quadrant & pixels
					Color foregroundColor = getColorForPixel(button, JPixelButton.STATES.FOREGROUND);
					Color backgroundColor = getColorForPixel(button, JPixelButton.STATES.BACKGROUND);
						
					button.flipState(foregroundColor, backgroundColor);
				}
				JPixelButton.clearCapturingMode();
				
				calculateGraphics();
			} else if (button.isPreviewButton()) {
				// TODO: user clicked on preview - should do same thing as changing display level combo box
				// TODO: extract the display level out of the name
				String name = button.getName();
				Pattern levelPattern = Pattern.compile("^PREVIEW_PREFIX" + "(\\d+)");
				Matcher levelMatcher = levelPattern.matcher(name);
				int level = Integer.parseInt(levelMatcher.group(1));
				if (level < 6) {
					comboDisplayLevel.setSelectedIndex(level);
					ActionEvent spoofedEvent = new ActionEvent(comboDisplayLevel, 0, "click");
					// spoof event
					actionPerformed(spoofedEvent);
				}
			} else if (button.isColorButton() || button.isMasterColorButton()) {
				performColorSelection(button, 0);
			}
		} else if (source instanceof JTextField) {
			JTextField textField = (JTextField) e.getSource();
			if (textField.getName().startsWith("graphics")) {
				handleGraphicsInput(textField);
			} else if (textField.getName().startsWith("colors")) {
				handleColorsInput(textField);
			}
		} else if (source == comboSelectSystem) {
			cvMode = comboSelectSystem.getSelectedItem().equals(COLECOVISION);
			asmMode = comboSelectSystem.getSelectedItem().equals(TI_99_ASM);
			calculateGraphics();
		} else if (source == comboDisplayLevel) {
			changeDisplayLevel(comboDisplayLevel.getSelectedIndex());
		} else if (source == resetPixelsButton) {
			// ask permission to perform the reset
			Object[] options = { "Reset Layer", "Reset All", "Cancel" };
			int optionSelected = JOptionPane.showOptionDialog(frame,
					"Resetting the image will lose all your pixel data.\nTo reset the colors, use the master colors.", "Reset Question",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			if (optionSelected == 0) {
				reset();
			} else if (optionSelected == 1) {
				resetAll();
			}
		} else if (source == leftButton) {
			horiz(-1);			
		} else if (source == rightButton) {
			horiz(1);			
		} else if (source == upButton) {
			vert(-1);			
		} else if (source == downButton) {
			vert(1);			
		} else if (source == mirrorButton) {
			mirror();
		} else if (source == flipButton) {
			flip();
		} else if (source == rotateClockButton) {
			rotate(true);
		} else if (source == rotateCounterButton) {
			rotate(false);
		} else if (source == invertColorsButton) {
			inverseColors();
		} else if (source == invertPixelsButton) {
			inversePixels();
		} else if (source instanceof JMenuItem) {
			JMenuItem menuItem = (JMenuItem)source;
			String[] menuSourceInfo = menuItem.getName().split(":", 2);
			String menuAction = menuSourceInfo[0];
			int menuStartItem = Integer.parseInt(menuSourceInfo[1]);
			
			// backup the selected index and switch to the item initiating the request
			int backupIndex = comboDisplayLevel.getSelectedIndex();
			comboDisplayLevel.setSelectedIndex(menuStartItem);
			
			String selectedText = menuItem.getText();
			for (int i=0; i < comboDisplayLevel.getItemCount(); i++) {
				if (selectedText.equals(comboDisplayLevel.getItemAt(i))) {
					if (menuAction.equals(POPUP_COPY)) {
						storedImageStates[i].store(this, false);
						comboDisplayLevel.setSelectedIndex(i);
					} else if (menuAction.equals(POPUP_SWAP)) {
						StoredImageState holder = storedImageStates[i];
						storedImageStates[i] = storedImageStates[menuStartItem];
						storedImageStates[menuStartItem] = holder;
						lastSelectedDisplayLevel = i;
						comboDisplayLevel.setSelectedIndex(i);
						comboDisplayLevel.setSelectedIndex(menuStartItem);
					}
				}
			}
			comboDisplayLevel.setSelectedIndex(backupIndex);
		}	
	}

	private void changeDisplayLevel(int switchTo) {
		// ensure hex string updated
		calculateGraphics();
		// save off current image
		storedImageStates[lastSelectedDisplayLevel].store(this, true);
					
		boolean enableExtraColorManipulation = comboDisplayLevel.getSelectedItem().equals(BACKGROUND);
		dualColorEnablerBox.setEnabled(enableExtraColorManipulation);
		invertColorsButton.setEnabled(enableExtraColorManipulation);
		
		String backgroundColorName = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.BACKGROUND);
		JPixelButton backgroundColor = colorButtonsMap.get(backgroundColorName);
		backgroundColor.setVisible(enableExtraColorManipulation);
		enableColorsEntry(true, enableExtraColorManipulation);
		
		if (enableExtraColorManipulation) {
			// update right colors if enabled
			enableColorsEntry(false, dualColorEnablerBox.isSelected());
		} else {
			// set global background to be transparent for sprites
			backgroundColor.setColor(JPixelButton.TRANSPARENT);
			// propagate color change to all other colors
			processButtonColorChange(backgroundColor);
		}
		
		// switch last selected to current one
		lastSelectedDisplayLevel = switchTo;
		
		// reset / restore image from place
		reset();

		storedImageStates[lastSelectedDisplayLevel].updateDisplay(this);
		// ensure hex string updated
		calculateGraphics();
	}
	
	// Output hex in several modes... cvMode = colecovision C format
	// else TI-99/4A (Extended) Basic format
	public String calculateQuadrantPixels(int quadrant) {
		return calculateQuadrantPixels(quadrant, false);
	}
			
	public String calculateQuadrantPixels(int quadrant, boolean forceCharsOnly) {
		StringBuilder pixelValue = new StringBuilder(16);
		if (!forceCharsOnly && cvMode) {
			pixelValue.append("{");
		}

		String pixelName = null;
		for (int y = 0; y < 8; y++) {
			int value = 0;
			for (int x = 0; x < 8; x++) {
				pixelName = JPixelButton.generatePixelName(quadrant, x, y);
				value *= 2;
				if (pixelButtonsMap.get(pixelName).isForegroundState())
					value += 1;
			}

			if (!forceCharsOnly && cvMode) {
				if (y > 0)
					pixelValue.append(", ");
				pixelValue.append("0x");
			}

			if (!forceCharsOnly && asmMode) {
				if (y % 2 == 0) {
					if (y > 0)
						pixelValue.append(",");
					pixelValue.append(">");
				}
			}
			
			String hexedItem = Integer.toHexString(value);
			if (hexedItem.length() == 1)
				pixelValue.append("0");
			pixelValue.append(hexedItem.toUpperCase());
		}

		if (!forceCharsOnly && cvMode) {
			pixelValue.append("}");
		}

		JTextField quadTextUpdate = quadrantHexMap.get(generateHexName("graphics", quadrant));
		quadTextUpdate.setText(pixelValue.toString());
		quadTextUpdate.setBackground(Color.WHITE);
		
		return pixelValue.toString();
	}

	// Output hex in several modes... cvMode = colecovision C format
	// else TI-99/4A (Extended) Basic format (which should never happen for
	// colors
	protected String calculateQuadrantColors(int quadrant) {
		StringBuilder colorValue = new StringBuilder(16);
		if (cvMode) {
			colorValue.append("{");
		}

		String colorName = null;
		for (int y = 0; y < 8; y++) {
			int value = 0;
			for (JPixelButton.STATES state : JPixelButton.STATES.values()) {
				colorName = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, quadrant, y, state);
				value *= 16;
				value += colorButtonsMap.get(colorName).getColorIndex();
			}
			if (cvMode) {
				if (y > 0)
					colorValue.append(", ");
				colorValue.append("0x");
			}
			String hexedItem = Integer.toHexString(value).toUpperCase();
			if (hexedItem.length() == 1)
				colorValue.append("0");
			colorValue.append(hexedItem);
		}

		if (cvMode) {
			colorValue.append("}");
		}

		JTextField quadTextUpdate = quadrantHexMap.get(generateHexName("colors", quadrant));
		quadTextUpdate.setText(colorValue.toString());
		quadTextUpdate.setBackground(Color.WHITE);
		
		return colorValue.toString();
	}

	public void colorPreviewButton(JPixelButton pixelToMatch) {
		colorPreviewButton(pixelToMatch, comboDisplayLevel.getSelectedIndex());
	}
	
	// I really don't like this, but it's quick & dirty - and it works too
	// Determine the matching preview pixel for the pixel provided and set it's color
	// Besides, what program doesn't have a comment like this somewhere?
	// Check out the source for MSDOS 6....  At least I don't use bad words 
	public void colorPreviewButton(JPixelButton pixelToMatch, int previewPanel) {
		String matchName = pixelToMatch.getName().replace(JPixelButton.PIXEL_PREFIX, JPixelButton.PREVIEW_PREFIX + previewPanel + "-");
		JPixelButton previewPixel = pixelButtonsMap.get(matchName);
		if (previewPixel != null) {
			Color newColor = pixelToMatch.getColor();
			// set the background of the pixel appropriately if transparent
			if (newColor == JPixelButton.TRANSPARENT)
				newColor = previewBackgroundColorButton.getBackground();
			previewPixel.setStateAndColor(pixelToMatch.getState(), newColor);
		}
	}
	
	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI() {

		// Create and set up the window.
		frame = new JFrame("GraphiCV by Howard Uman / unHUman.com");

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		GraphiCV newContentPane = new GraphiCV();
		JPixelButton.setGraphiCV(newContentPane);
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// Initialize all the graphics backups
		for (int i=0; i<newContentPane.comboDisplayLevel.getItemCount(); i++) {
			// simulate event to build up backup memory
			newContentPane.comboDisplayLevel.setSelectedIndex(i);
			ActionEvent e = new ActionEvent(newContentPane.comboDisplayLevel, 0, "nothing");
			newContentPane.actionPerformed(e);
		}
		newContentPane.comboDisplayLevel.setSelectedIndex(0);

		// simulate color change
		newContentPane.redrawPreview(JPixelButton.CYAN);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
}

	public static void main(String[] args) {
		// Make colors render properly on mac (requires setOpaque)
		// see: http://stackoverflow.com/questions/1065691/how-to-set-the-background-color-of-a-jbutton-on-the-mac-os
		try {
			UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public void mouseClicked(MouseEvent e) {
		Integer selectedPreview = null;
		
		Object source = e.getSource();
		if (source instanceof JPixelButton) {
			JPixelButton pixelCheck = (JPixelButton)source;
			Integer previewNum = pixelCheck.getPreviewNum();
			if ((previewNum != null) && (previewNum < 5)) {
				selectedPreview = previewNum;
			}
		} else if (source instanceof JLabel) {
			String text = ((JLabel)source).getText();
			for (int i=0; i<labelsPreview.length; i++) {
				if ((i < comboDisplayLevel.getItemCount()) && labelsPreview[i].equals(text)) {
					selectedPreview = i;
					break;
				}
			}
		}
		
		if (selectedPreview != null) {
			if (e.getButton() == MouseEvent.BUTTON1)
				comboDisplayLevel.setSelectedIndex(selectedPreview);
			else if (e.getButton() == MouseEvent.BUTTON3) {
				// TODO - show right click menu
				JPopupMenu popup = new JPopupMenu("RightClickPreviews");
				
				String[] actionArray = {POPUP_COPY, POPUP_SWAP};
				for (String action: Arrays.asList(actionArray)) {
					if (!action.equals(actionArray[0]))
						popup.addSeparator();
					JMenuItem title = new JMenuItem(action);
					title.setEnabled(false);
					popup.add(title);
					popup.addSeparator();
					for (int i=0; i < comboDisplayLevel.getItemCount(); i++) {
						String itemManipulate = (String)comboDisplayLevel.getItemAt(i);
						JMenuItem menuItem = new JMenuItem(itemManipulate);
						menuItem.setName(action + ":" + selectedPreview);
						menuItem.addActionListener(this);
						if (i == selectedPreview)
							menuItem.setEnabled(false);
						popup.add(menuItem);
					}
				}
				popup.show(e.getComponent(), 0, 0);									
			}
		}
	}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {}

	public void mouseReleased(MouseEvent arg0) {}
}