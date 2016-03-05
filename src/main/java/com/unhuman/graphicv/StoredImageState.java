package com.unhuman.graphicv;

import java.awt.Color;

public class StoredImageState {
	boolean initialized = false;
	protected enum MODE { SPRITE, CHAR };
	protected String charMap;
	protected Color[] master = new Color[2];
	protected Color[][][] set = new Color[2][4][8];
	protected boolean[][] selectedPixels = new boolean[16][16]; // I do not like this. 
	
	protected void store(GraphiCV graphic, boolean backupColors) {
		initialized = true;
		
		// backup charmap
		StringBuilder allChars = new StringBuilder(256);
		for (int quad = 0; quad < 4; quad++)
			allChars.append(graphic.calculateQuadrantPixels(quad, true));
		charMap = allChars.toString();
		
		// backup colors
		if (backupColors) {
			for (int fgbg = 0; fgbg < 2; fgbg++) {
				String key = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.values()[fgbg]);
				master[fgbg] = graphic.colorButtonsMap.get(key).getColor();
				for (int quad=0; quad<4; quad++) {
					for (int row=0; row<8; row++) {
						key = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, quad, row, JPixelButton.STATES.values()[fgbg]);
						set[fgbg][quad][row] = graphic.colorButtonsMap.get(key).getColor();
					}
				}
			}
		}
		
		// track selected pixels - this is duplicative, but whatever
		for (int x=0; x<16; x++) {
			for (int y=0; y<16; y++) {
				String key = JPixelButton.generatePixelName(x, y);
				selectedPixels[x][y] = graphic.pixelButtonsMap.get(key).getState().equals(JPixelButton.STATES.FOREGROUND);				
			}
		}
	}
	
	public boolean getPixelSelected(int x, int y) {
		return selectedPixels[x][y];
	}
	
	protected void updateDisplay(GraphiCV graphic) {
		if (!initialized)
			return;
		
		// restore colors
		for (int fgbg = 0; fgbg < 2; fgbg++) {
			String key = JPixelButton.generateColorName(JPixelButton.MASTER_COLOR_PREFIX, 0, 0, JPixelButton.STATES.values()[fgbg]);
			graphic.colorButtonsMap.get(key).setColor(master[fgbg]);
			for (int quad=0; quad<4; quad++) {
				for (int row=0; row<8; row++) {
					key = JPixelButton.generateColorName(JPixelButton.COLOR_PREFIX, quad, row, JPixelButton.STATES.values()[fgbg]);
					graphic.colorButtonsMap.get(key).setColor(set[fgbg][quad][row]);
				}
			}
		}
		
		// restore graphic
		graphic.parseGraphicsText(4, 0, charMap);
	}
}
