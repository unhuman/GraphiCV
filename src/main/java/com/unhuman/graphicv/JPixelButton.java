package com.unhuman.graphicv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;

/**
 *
 * @author Howie
 */
public class JPixelButton extends JButton implements FocusListener {
	private static final long serialVersionUID = 1L;
	private static GraphiCV graphiCV = null;

	private static final int UNKNOWN_PIXEL = -1;
	private static int pixelSize = 16;
	private static Dimension pixelDimension = new Dimension(pixelSize, pixelSize);
	private static int previewSize = 4;
	private static Dimension previewDimension = new Dimension(previewSize, previewSize);

	public static void setGraphiCV(GraphiCV owner) {
		graphiCV = owner;
	}

	// We use this suck out what transparent would really be - doesn't like null
	private static JButton transparentColorSuckerButton = new JButton();
	static { transparentColorSuckerButton.setBackground(null); }
	protected static Color TRANSPARENT = transparentColorSuckerButton.getBackground();
	public static Color colors[] = {
			TRANSPARENT, // transparent is a special case - java makes it look nice :)
			new Color(0, 0, 0), new Color(71, 183, 59), new Color(124, 207, 111), new Color(93, 78, 255),
			new Color(128, 114, 255), new Color(182, 98, 71), new Color(93, 200, 237), new Color(215, 107, 72),
			new Color(251, 143, 108), new Color(195, 205, 65), new Color(211, 218, 118), new Color(62, 159, 47),
			new Color(182, 100, 199), new Color(204, 204, 204), new Color(255, 255, 255) };

	public static Color BLACK = colors[1];
	public static Color CYAN = colors[7];
	public static Color WHITE = colors[15];	
	

	enum STATES { FOREGROUND, BACKGROUND }

	enum PIXELTYPES { PIXEL, PREVIEW, COLOR, MASTERCOLOR }

	public static final String PIXEL_PREFIX = "pixel-";
	public static final String PREVIEW_PREFIX = "preview-";
	public static final String COLOR_PREFIX = "color-";
	public static final String MASTER_COLOR_PREFIX = "mastercolor-";

	private Integer previewNum = null;
	private int pixelX;
	private int pixelY;
	private STATES state = STATES.BACKGROUND;
	private PIXELTYPES pixelType = null;
	private static EtchedBorder insideBorder = new EtchedBorder(EtchedBorder.LOWERED, Color.GRAY, Color.CYAN);
	private static LineBorder outsideBorder = new LineBorder(Color.GRAY);
	private static CompoundBorder selectedBorder = new CompoundBorder(insideBorder, outsideBorder);
	private Border unselectedBorder = null;

	private static STATES capturingMode = null;

	public JPixelButton(JPixelButton.STATES state, Color startColor, String name, Dimension dimension) {
		this(state, startColor, name, UNKNOWN_PIXEL, UNKNOWN_PIXEL);
		setDimensions(dimension);
	}	
	
	public JPixelButton(JPixelButton.STATES state, Color startColor, String name) {
		this(state, startColor, name, UNKNOWN_PIXEL, UNKNOWN_PIXEL);
	}

	public JPixelButton(JPixelButton.STATES state, Color startColor, String name, Integer previewNum, int x, int y) {
		this(state, startColor, name, x, y);
		this.previewNum = previewNum;
	}
	
	public JPixelButton(JPixelButton.STATES state, Color startColor, String name, int x, int y) {
		this.pixelX = x;
		this.pixelY = y;
		
		Dimension useDimension = pixelDimension;

		if (name.startsWith(PIXEL_PREFIX)) {
			pixelType = PIXELTYPES.PIXEL;
			// pixels we track colors
			addMouseListener(new MouseEventListener());
		} else if (name.startsWith(PREVIEW_PREFIX)) {
			this.setEnabled(false);
			pixelType = PIXELTYPES.PREVIEW;
			useDimension = previewDimension;
		} else if (name.startsWith(COLOR_PREFIX)) {
			pixelType = PIXELTYPES.COLOR; 
		} else {
			pixelType = PIXELTYPES.MASTERCOLOR;
		}
		
		setName(name);
		// see: http://stackoverflow.com/questions/1065691/how-to-set-the-background-color-of-a-jbutton-on-the-mac-os
		setOpaque(true);

		setStateAndColor(state, startColor);
		if (!name.startsWith(PREVIEW_PREFIX)) {
			addFocusListener(this);
			unselectedBorder = getBorder();			
		}
		setDimensions(useDimension);
	}
	
	private void setDimensions(Dimension dimension) {
		setSize(dimension);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);		
	}

	
	public static String generatePixelName(int x, int y) {
		int quadrant = (y / 8) + ((x / 8) * 2);
		return generatePixelName(quadrant, x%8, y%8);
	}
	public static String generatePixelName(int quadrant, int x, int y) {
		return JPixelButton.PIXEL_PREFIX + quadrant + "-" + x + "-" + y;
	}

	public static String generatePreviewName(int index, int x, int y) {
		int quadrant = (y / 8) + ((x / 8) * 2);
		return generatePreviewName(index, quadrant, x%8, y%8);
	}	
	public static String generatePreviewName(int index, int quadrant, int x, int y) {
		return JPixelButton.PREVIEW_PREFIX + index + "-" + quadrant + "-" + x + "-" + y;
	}

	public static String generateColorName(String identifier, int y, JPixelButton.STATES state) {
		int quadrant = (y / 8);
		return generateColorName(identifier, quadrant, y%8, state);
	}
	public static String generateColorName(String identifier, int quadrant, int y, JPixelButton.STATES state) {
		return identifier + quadrant + "-" + y % 8 + "-" + state;
	}

	public static String generateColorName(String identifier, int y, String state) {
		int quadrant = (y / 8);
		return generateColorName(identifier, quadrant, y%8, state);
	}
	public static String generateColorName(String identifier, int quadrant, int y, String state) {
		return identifier + quadrant + "-" + y % 8 + "-" + state;
	}
	
	public boolean isPixelButton() {
		return (pixelType == PIXELTYPES.PIXEL);
	}
	public boolean isPreviewButton() {
		return (pixelType == PIXELTYPES.PREVIEW);
	}
	public boolean isColorButton() {
		return (pixelType == PIXELTYPES.COLOR);
	}
	public boolean isMasterColorButton() {
		return (pixelType == PIXELTYPES.MASTERCOLOR);
	}
	
	public void flipState(Color foregroundColor, Color backgroundColor) {
		state = (state == STATES.FOREGROUND) ? STATES.BACKGROUND : STATES.FOREGROUND;
		setColor((state == STATES.FOREGROUND) ? foregroundColor : backgroundColor);
	}

	public void setStateAndColor(STATES state, Color color) {
		this.state = state;
		setColor(color);

		if ((graphiCV != null) && (pixelX != UNKNOWN_PIXEL) && (pixelY != UNKNOWN_PIXEL))
			graphiCV.updateCombinedPreview(pixelX, pixelY);
	}

	public void setColor(Color newColor) {
		setBackground(newColor);
	}

	@Override
	public void setBackground(Color newColor) {
		super.setBackground(newColor);
		if (isPixelButton() && (graphiCV != null))
			graphiCV.colorPreviewButton(this);
	}

	public Color getColor() {
		return getBackground();
	}

	public int getColorIndex() {
		for (int i = 0; i < colors.length; i++) {
			if (getBackground() == colors[i])
				return i;
		}

		// we didn't find a color - assume it's transparent
		return 0;
	}

	public STATES getState() {
		return state;
	}

	public boolean isForegroundState() {
		return (state == STATES.FOREGROUND);
	}

	public void focusLost(FocusEvent e) {
		setBorder(unselectedBorder);
	}

	public void focusGained(FocusEvent e) {
		setBorder(selectedBorder);
	}
	
	protected void processMouseEvent(MouseEvent e) {
		if (getName().startsWith(PIXEL_PREFIX) && (capturingMode != null)) {
			setStateAndColor(capturingMode, graphiCV.getColorForPixel(this, capturingMode));
			grabFocus();
		}
		super.processMouseEvent(e);
	}
	
	public static void clearCapturingMode() {
		capturingMode = null;
	}
	public boolean isCapturing() {
		return (capturingMode != null);
	}
	
	class MouseEventListener implements MouseListener {
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {
			capturingMode = (getState() == STATES.FOREGROUND) ? STATES.BACKGROUND : STATES.FOREGROUND;
			Color newColor = graphiCV.getColorForPixel((JPixelButton)e.getComponent(), capturingMode);
			setStateAndColor(capturingMode, newColor);
		}
		public void mouseReleased(MouseEvent e) {
			if (capturingMode != null) {
				clearCapturingMode();
				graphiCV.calculateGraphics();
			}
		}
	}

	public int getPixelX() {
		return pixelX;
	}

	public int getPixelY() {
		return pixelY;
	}
	
	public Integer getPreviewNum() {
		return previewNum;
	}
}
