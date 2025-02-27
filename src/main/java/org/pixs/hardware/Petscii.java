package org.pixs.hardware;

public class Petscii {
	/**
	 * ISO-8859-1 to Petscii conversion table.
	 */
	private static final int[] ISO8859_1_TO_PETSCII = { 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, // 0x00
			0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f, // 0x08
			0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, // 0x10
			0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, // 0x18
			0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, // 0x20 !"#$%&'
			0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, // 0x28 ()*+,-./
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 0x30 01234567
			0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, // 0x38 89:;<=>?
			0x00, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, // 0x40 @ABCDEFG
			0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, // 0x48 HIJKLMNO
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, // 0x50 PQRSTUVW
			0x58, 0x59, 0x5a, 0x1b, 0xbf, 0x1d, 0x1e, 0x64, // 0x58 XYZ[\]^_
			0x27, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, // 0x60 `abcdefg
			0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, // 0x68 hijklmno
			0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, // 0x70 pqrstuvw
			0x18, 0x19, 0x1a, 0x1b, 0x5d, 0x1d, 0x1f, 0x20, // 0x78 xyz{|}~
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x80
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x88
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x90
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x98
			0x20, 0x21, 0x03, 0x1c, 0xbf, 0x59, 0x5d, 0xbf, // 0xa0 ¡¢£¤¥¦§
			0x22, 0x43, 0x01, 0x3c, 0xbf, 0x2d, 0x52, 0x63, // 0xa8 ¨©ª«¬­®¯
			0x0f, 0xbf, 0x32, 0x33, 0x27, 0x15, 0xbf, 0xbf, // 0xb0 °±²³´µ¶·
			0x2c, 0x31, 0x0f, 0x3e, 0xbf, 0xbf, 0xbf, 0x3f, // 0xb8 ¸¹º»¼½¾¿
			0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x43, // 0xc0 À�?ÂÃÄÅÆÇ
			0x45, 0x45, 0x45, 0x45, 0x49, 0x49, 0x49, 0x49, // 0xc8 ÈÉÊËÌ�?Î�?
			0xbf, 0x4e, 0x4f, 0x4f, 0x4f, 0x4f, 0x4f, 0x18, // 0xd0 �?ÑÒÓÔÕÖ×
			0x4f, 0x55, 0x55, 0x55, 0x55, 0x59, 0xbf, 0xbf, // 0xd8 ØÙÚÛÜ�?Þß
			0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x03, // 0xe0 àáâãäåæç
			0x05, 0x05, 0x05, 0x05, 0x09, 0x09, 0x09, 0x09, // 0xe8 èéêëìíîï
			0xbf, 0x0e, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0xbf, // 0xf0 ðñòóôõö÷
			0x0f, 0x15, 0x15, 0x15, 0x15, 0x19, 0xbf, 0x19 // 0xf8 øùúûüýþÿ };
	};

	/**
	 * Petscii to ISO-8859-1 conversion table.<BR>
	 *
	 * CHR$ conversion table (0x01 = no output)
	 */
	private static final int PETSCII_TO_ISO8859_1[] = { 0x0, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1,
			0x0d, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x20, 0x21,
			0x1, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33,
			0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45,
			0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57,
			0x58, 0x59, 0x5a, 0x5b, 0x24, 0x5d, 0x20, 0x20,
			/* alternative: CHR$(92=0x5c) => ISO Latin-1(0xa3) */
			0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, 0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x5c, 0x23,
			0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c,
			/* 0x80-0xFF */
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1,
			0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x1, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, 0x23,
			0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f, 0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f,
			0x5c, 0x5c, 0x2f, 0x2f, 0x23, 0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, 0x7c, 0x5c, 0x5c, 0x2f, 0x5c,
			0x5c, 0x2f, 0x2f, 0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, 0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26,
			0x5c, 0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, 0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, 0x2f,
			0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, 0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x23 };

	/**
	 * Convert ISO-8859-1 to PETSCII characters.
	 *
	 * @param c ISO-8859-1 character
	 * @return PETSCII byte
	 */
	public static byte iso88591ToPetscii(char c) {
		return (byte) ISO8859_1_TO_PETSCII[c & 0xff];
	}

	/**
	 * Convert PETSCII to ISO-8859-1 characters.
	 *
	 * @param c PETSCII byte
	 * @return ISO-8859-1 character (unmapped characters are converted to space)
	 */
	public static char petsciiToIso88591(byte c) {
		byte b = (byte) PETSCII_TO_ISO8859_1[c & 0xff];
		return b != 1 ? (char) (b & 0xff) : ' ';
	}

	/**
	 * Converts PETSCII to ISO-8859-1 characters.
	 *
	 * @param petscii PETSCII bytes. (zero byte terminates the string)
	 *
	 * @return ISO-8859-1 characters.
	 */
	public static final String petsciiToIso88591(final byte[] petscii) {
		StringBuilder result = new StringBuilder();
		for (byte element : petscii) {
			if (element == 0) {
				break;
			}
			result.append(petsciiToIso88591(element));
		}
		return result.toString();
	}

}
