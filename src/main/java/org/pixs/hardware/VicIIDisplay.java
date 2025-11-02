package org.pixs.hardware;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class VicIIDisplay {

	private BufferedImage m_image;

	private byte m_borderColor = 14;
	private byte m_backgroundColor = 6;

	private byte[] m_charset; // 256 characters encoded in 256 * 8 bytes
	private byte[] m_screenRam = new byte[1000]; // 1000 characters codes
	private byte[] m_colorRam = new byte[1000]; // 1000 colors (only lower nibble is significant)

	private byte[] m_fliColorRam = new byte[8192]; // 

	private byte[] m_bitmapRam = new byte[8192]; // 1000 characters codes

	private Sprite[] m_sprites = new Sprite[8];
	
	public static enum ColorMode {
		STANDARD_TEXT,
		STANDARD_BITMAP,
		HIRES_FLI
	}
	
	private ColorMode m_mode = ColorMode.STANDARD_TEXT;
	
	public ColorMode getMode() {
		return m_mode;
	}

	public void setMode(ColorMode mode) {
		m_mode = mode;
	}

	public VicIIDisplay() {
		
		setCharset(getKernalCharset(0));
		
		Arrays.fill(m_screenRam, (byte)32);
		Arrays.fill(m_colorRam, (byte)14);
		
		for (int i=0;i<8;i++) {
			m_sprites[i] = new Sprite();
		}
		
		// Init palette (loaded in VICE vpl format)
		m_palettes = new Color[16];
		
		// Apply a gamma of 1.5 to pallet
		float ginv = 1 / 1.5f;
		
//		try (InputStream is = getClass().getClassLoader().getResourceAsStream("org/pixs/vicII/ccs64.vpl")) {
//		try (InputStream is = getClass().getClassLoader().getResourceAsStream("org/pixs/vicII/vice.vpl")) {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("org/pixs/hardware/pepto-pal.vpl")) {
	        try (InputStreamReader isr = new InputStreamReader(is);
	             BufferedReader reader = new BufferedReader(isr)) {
	            List<String> colors = reader.lines().filter(s -> (!s.trim().startsWith("#") && !s.isEmpty())).collect(Collectors.toList());
	            for (int i=0;i<16;i++) {
					Integer[] colorsAsInt = Arrays.stream(colors.get(i).split(" ")).map(s -> Integer.parseInt(s, 16)).toArray(Integer[]::new);
					int red =  (int) Math.round(255 * Math.pow(colorsAsInt[0] / 255.0, ginv)); 
					int green =  (int) Math.round(255 * Math.pow(colorsAsInt[1] / 255.0, ginv)); 
					int blue =  (int) Math.round(255 * Math.pow(colorsAsInt[2] / 255.0, ginv)); 

					m_palettes[i] = new Color(red, green, blue);
	            }
	        }
			catch (Exception ex) { ex.printStackTrace(); }
	    }
		catch (Exception ex) { ex.printStackTrace(); }
		
		
		byte[] redComponents = new byte[m_palettes.length];
		byte[] greenComponents = new byte[m_palettes.length];
		byte[] blueComponents = new byte[m_palettes.length];
		for (int i=0;i<m_palettes.length;i++) {
			Color color = m_palettes[i];
			redComponents[i] = (byte)(color.getRed() & 0x0FF);
			greenComponents[i] = (byte)(color.getGreen() & 0x0FF);
			blueComponents[i] = (byte)(color.getBlue() & 0x0FF);
		}
		
        IndexColorModel colorModel = new IndexColorModel(4, 16, redComponents, greenComponents, blueComponents);
        
        m_buffer = new MyDataBuffer();
        
        SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_BYTE, 384, 272, new int[] { 0xff });
		WritableRaster raster = new MyRaster(sm, m_buffer, new Point(0, 0));
        
        m_image = new BufferedImage(colorModel, raster, false, null);
	}
	
	private class MyDataBuffer extends DataBuffer {

		public MyDataBuffer() {
			super(DataBuffer.TYPE_BYTE, 0);
		}
		
		@Override
		public int getElem(int bank, int i) {
		    int y = i / 384;
		    int x = i % 384;
		    int bgPixel;

		    // Determine background pixel (border or main display)
		    if (x < 4 * 8 || x >= 44 * 8 || y < 4 * 8 + 3 || y >= 272 - 4 * 8 - 5) {
		        bgPixel = m_borderColor;
		    } else {
		        if (m_mode == ColorMode.STANDARD_TEXT) {
		            int col = (x - 4 * 8) / 8;
		            int row = (y - (4 * 8 + 3)) / 8;
		            int charCode = m_screenRam[row * 40 + col] & 0xff;
		            int color = m_colorRam[row * 40 + col] & 0xff;
		            int pixelX = (x - 4 * 8) % 8;
		            int pixelY = (y - (4 * 8 + 3)) % 8;
		            byte tmp = m_charset[charCode * 8 + pixelY];
		            boolean bitSet = (tmp & (1 << (7 - pixelX))) != 0;
		            // Note: using m_bgColors array here for background management
		            bgPixel = bitSet ? color : m_bgColors[y - (4 * 8 + 3)];
		        } else if (m_mode == ColorMode.STANDARD_BITMAP) {
		            int viewportX = (x - 4 * 8);
		            int viewportY = (y - (4 * 8 + 3));
		            int col = viewportX / 8;
		            int row = viewportY / 8;
		            int bitmapByteIndex = 320 * row + 8 * col + (viewportY % 8);
		            byte tmp = m_bitmapRam[bitmapByteIndex];
		            boolean bitSet = (tmp & (1 << (7 - (viewportX % 8)))) != 0;
		            int color = m_screenRam[row * 40 + col] & 0xff;
		            bgPixel = bitSet ? ((color >> 4) & 0x0f) : (color & 0x0f);
		        } else if (m_mode == ColorMode.HIRES_FLI) {
		            int viewportX = (x - 4 * 8);
		            int viewportY = (y - (4 * 8 + 3));
		            int col = viewportX / 8;
		            int row = viewportY / 8;
		            int bitmapByteIndex = 320 * row + 8 * col + (viewportY % 8);
		            byte tmp = m_bitmapRam[bitmapByteIndex];
		            boolean bitSet = (tmp & (1 << (7 - (viewportX % 8)))) != 0;
		            int color = m_fliColorRam[bitmapByteIndex] & 0xff;
		            bgPixel = bitSet ? ((color >> 4) & 0x0f) : (color & 0x0f);
		        } else {
		            bgPixel = 0;
		        }
		    }
		    
		    // --- Sprite clipping on external border ---
		    // If current pixel is in the border area,
		    // don't overlay sprite (default C64 behavior)
		    if (x < 4 * 8 || x >= 44 * 8 || y < 4 * 8 + 3 || y >= 272 - 4 * 8 - 5) {
		        return bgPixel;
		    }

		    // Sprite management: iterate through all sprites
		    for (int s = 0; s < m_sprites.length; s++) {
		        Sprite sprite = m_sprites[s];
		        if (sprite == null || !sprite.enabled)
		            continue;
		        // Displayed sprite size depends on doubleWidth/doubleHeight flags
		        int spriteWidth = sprite.doubleWidth ? Sprite.WIDTH * 2 : Sprite.WIDTH;
		        int spriteHeight = sprite.doubleHeight ? Sprite.HEIGHT * 2 : Sprite.HEIGHT;
		        // If pixel (x,y) is in the sprite area...
		        int sx = sprite.x + 8;
		        int sy = sprite.y - 15;
		        if (x >= sx && x < sx + spriteWidth && y >= sy && y < sy + spriteHeight) {
		            // Relative coordinates within the sprite.
		            int relX = x - sx;
		            int relY = y - sy;
		            // If sprite is double-sized, compensate:
		            if (sprite.doubleWidth)
		                relX /= 2;
		            if (sprite.doubleHeight)
		                relY /= 2;
		            // Get pixel from sprite (according to its display mode)
		            int spritePixel = sprite.getPixelAt(relX, relY);
		            if (spritePixel != Sprite.TRANSPARENT) {
		                // Sprite pixel has priority (lowest index first)
		                return spritePixel;
		            }
		        }
		    }
		    return bgPixel;
		}

		@Override
		public void setElem(int bank, int i, int val) {
			throw new UnsupportedOperationException();
		}
	}
	
	public byte[] getKernalCharset(int index) {
		try {
			byte[] data = getClass().getClassLoader().getResourceAsStream("org/pixs/hardware/chargen").readAllBytes();
		    byte[] font = new byte[256*8];
		    System.arraycopy(data, index * 8 * 256, font, 0, font.length);
		    return font;
		}
		catch (Exception ex) { ex.printStackTrace(); }
		return null;
	}
	
	public void setCharset(byte[] charset) {
		m_charset = charset;
	}
	
	Random random = new Random();
	
	public void setRandomCharDefinition(int charIndex, byte[] oldChar) {
		for (int i=charIndex*8;i<charIndex*8+8;i++) {
			oldChar[i-charIndex*8] = m_charset[i];
//			if (random.nextDouble() < 0.1)
				m_charset[i] = (byte) random.nextInt();
		}
	}

	public void getCharDefinition(int charIndex, byte[] charDefinition) {
		for (int i=charIndex*8;i<charIndex*8+8;i++) {
			charDefinition[i-charIndex*8] = m_charset[i];
		}
	}

	public void setCharDefinition(int charIndex, byte[] charDefinition) {
		for (int i=charIndex*8;i<charIndex*8+8;i++) {
			m_charset[i] = charDefinition[i-charIndex*8];
		}
	}

	public class MyRaster extends WritableRaster {

		protected MyRaster(SampleModel sampleModel, DataBuffer buffer, Point origin) {
			super(sampleModel, buffer, origin);
		}
	}
	
	public BufferedImage getImage() {
		return m_image;
	}
	
	BufferedImage viewPortImage = new BufferedImage(320, 200, BufferedImage.TYPE_3BYTE_BGR);

	private MyDataBuffer m_buffer;
	
	public BufferedImage getViewportImage() {
		Graphics2D graphics = (Graphics2D) viewPortImage.getGraphics();
		graphics.drawImage(getImage(), -32, -35, null);
		return viewPortImage;
	}
	
	public void drawViewPort(Graphics2D g2d, int x, int y, int width, int height) {
		g2d.drawImage(getImage(), 0, 0, width, height, 32+x, 35+y, 32+x+width, 35+y+height, null);
	}


	BufferedImage charImage = new BufferedImage(8, 8, BufferedImage.TYPE_3BYTE_BGR);

	private byte[] m_bgColors = new byte[200];

	private Color[] m_palettes;
	
	public BufferedImage getViewportImage8x8(int x, int y) {
		Graphics2D graphics = (Graphics2D) charImage.getGraphics();
//		graphics.setClip(0, 0, 8, 8);
//		graphics.drawImage(getImage(), -32-(x*8), -35-(y*8), 8, 8, null);
		graphics.drawImage(getImage(), 0, 0, 8, 8, 32+x*8, 35+y*8, 32+x*8+x, 35+y*8+8, null);
		return charImage;
	}

	public void setCharAt(int column, int row, int charCode) {
		m_screenRam[column+row*40] = (byte) charCode;
//		m_buffer.invalidate(column+row*40);
	}

	public void setColorAt(int column, int row, int color) {
		m_colorRam[column+row*40] = (byte) (color & 0x0F);
//		m_buffer.invalidate(column+row*40);
	}

	public void setCharAt(int index, int charcode) {
		if (index < 1000) {
			m_screenRam[index] = (byte)charcode;
//			m_buffer.invalidate(index);
		}
	}

	public void setColorAt(int index, int j) {
		if (index < 1000) {
			m_colorRam[index] = (byte)(j & 0x0F);
//			m_buffer.invalidate(index);
		}
	}

	public void setBorderColor(int i) {
		m_borderColor = (byte) i;
	}

	public void setBackgroundColor(int i) {
		m_backgroundColor = (byte) i;
	}

	public byte getCharAt(int column, int row) {
		return m_screenRam[column+row*40];
	}

	public byte getCharAt(int i) {
		return m_screenRam[i];
	}

	public int getColorAt(int column, int row) {
		return m_colorRam[column+row*40];
	}
	
	public byte getFLIColorAt(int index) {
		return m_fliColorRam[index];
	}

	public void setFLIColorAt(int index, byte color) {
		m_fliColorRam[index] = color;
	}
	
	public byte getBackgroundColor() {
		return m_backgroundColor;
	}

	public void getBackgroundColors(byte[] colors) {
		System.arraycopy(m_bgColors, 0, colors, 0, 200);
	}
	
	public void setBackgroundColors(byte[] colors) {
		System.arraycopy(colors, 0, m_bgColors, 0, 200);
	}

	public void setBGColor(int index, byte color) {
		m_bgColors[index] = color;
	}

	public byte getBitmapValue(int randomBitmapIndex) {
		return m_bitmapRam[randomBitmapIndex];
	}

	public void setBitmapValue(int randomBitmapIndex, byte randomBitmapValue) {
		m_bitmapRam[randomBitmapIndex] = randomBitmapValue;
	}

	public Color getBorderColor() {
		return m_palettes[m_borderColor];
	}

	/**
	 * @return 8000 bytes bitmap content
	 */
	public byte[] getBitmapContent() {
		return m_bitmapRam;
	}
	
	/**
	 * @return 1000 bytes video matrix (screen ram)
	 */
	public byte[] getVideoMatrix() {
		return m_screenRam;
	}
	

	/**
	 * Permet de définir un sprite pour le numéro index (0 à 7)
	 */
	public void setSprite(int index, Sprite sprite) {
	    if (index >= 0 && index < m_sprites.length) {
	        m_sprites[index] = sprite;
	    }
	}

	public Sprite getSprite(int index) {
		 return m_sprites[index];
	}
 }
