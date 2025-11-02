package org.pixs.mazemaster;

import java.util.Arrays;

public class Character {
	
	private String m_name = "";
	private int m_strength;
	private int m_intellect;
	private int m_dexterity;
	private int m_constitution;
	private int m_classType;
	private int m_gold;
	private int m_xp;
	
	private byte[] m_items = new byte[4];
	private int m_condition;
	private int m_mazeXp;
	private int m_spellPoints;
	private int m_armorRating;
	private int m_numberOfStrikes;
	private int m_spellNumber;

	public byte[] getNameAsBytes() {
		byte[] name = new byte[16];
		Arrays.fill(name, (byte)0x24);
		for (int i=0;i<16;i++) {
			byte value = 36;
			if (i < m_name.length()) {
				char c = m_name.toUpperCase().charAt(i);
				if (c >= '0' && c <= '9') {
					value = (byte) (c - '0');
				}
				else if (c >= 'A' && c <= 'Z') {
					value = (byte) (10 + c - 'A');
				}
				else if (c == ' ') {
					value = (byte) (0x24);
				}
			}
			name[i] = value;
		}
		return name;
	}

	public void setNameFromByte(byte[] name) {
		m_name = "";
		for (byte c : name) {
			if (c < 10) m_name += (char)('0'+c);
			else if (c == 0x24) {
				m_name += ' ';
			}
			else {
				m_name += (char)('A'+(c-10));
			}
		}
	}
	
	public void reset() {
		m_name = "";
		resetAttributes();
		clearIndicators();
	}

	public void resetAttributes() {
		m_strength = 0;
		m_intellect = 0;
		m_dexterity = 0;
		m_constitution = 0;
		for (int i=0;i<4;i++) {
			setItem(i, (byte)0);
		}
		m_gold = 0;
		m_xp = 0;
		m_condition = 0;
		m_mazeXp = 0;
		m_spellPoints = 0;
		m_armorRating = 0;
	}

	public void setStrength(int strength) {
		m_strength = strength;
	}

	public void setIntellect(int intellect) {
		m_intellect = intellect;
	}

	public void setDexterity(int dexterity) {
		m_dexterity = dexterity;
	}

	public void setConstitution(int constitution) {
		m_constitution = constitution;
	}

	/**
	 * @param i 1 for warrior, 2 for wizard
	 */
	public void setClassType(int i) {
		m_classType = i;
	}

	public int getClassType() {
		return m_classType;
	}

	public void setGold(int gold) {
		m_gold = gold;
	}

	public void setXP(int xp) {
		m_xp = xp;
	}

	public void clearIndicators() {
		Arrays.fill(m_items, (byte)0);
	}

	public int getIntellect() {
		return m_intellect;
	}

	public int getDexterity() {
		return m_dexterity;
	}

	public int getConstitution() {
		return m_constitution;
	}

	public int getGold() {
		return m_gold;
	}

	public int getXp() {
		return m_xp;
	}

	public int getStrength() {
		return m_strength;
	}

	public byte getItemCode(int i) {
		return m_items[i];
	}

	public void setItem(int index, byte code) {
		m_items[index] = code;
	}

	/**
	 * $800-$80F : character 1 name
	 * $810 : strength
	 * $811 : intellect
	 * $812 : dexterity
	 * $813 : constitution
	 * $814 : class (1=war, 2=wiz)
	 * $815-$816 : gold
	 * $817-$818 : experience
	 * $819 : first item (weapon)
	 * $81A : second item (armor)
	 * $81B : third item (shield)
	 * $81C : fourth item (magical item)
	 * $81D : condition (current HP)
	 * $81E$-$81F : tmp experience (when in maze)
	 * $820 : armor rating
	 * $821 : spell points
	 * $822 : spell number cast during fight (0=weapon)
	 * $824 : number of strikes for warrior (tmp experience / 8192)
	 */
	public byte[] getRawBytes() {
		byte[] raw = new byte[0x25];
		System.arraycopy(getNameAsBytes(), 0, raw, 0, 0x0f);
		raw[0x10] = (byte)m_strength;
		raw[0x11] = (byte)m_intellect;
		raw[0x12] = (byte)m_dexterity;
		raw[0x13] = (byte)m_constitution;
		raw[0x14] = (byte) m_classType;
		raw[0x15] = (byte)(getGold() & 0x00FF);
		raw[0x16] = (byte)((getGold() >> 8) & 0x00FF);
		raw[0x17] = (byte)(getXp() & 0x00FF);
		raw[0x18] = (byte)((getXp() >> 8) & 0x00FF);
		raw[0x19] = getItemCode(0);
		raw[0x1A] = getItemCode(1);
		raw[0x1B] = getItemCode(2);
		raw[0x1C] = getItemCode(3);
		
		// TODO other values
		
		return raw;
	}
	
	public void setRawBytes(byte[] raw) {
		m_strength = raw[0x10] & 0x0ff;
		m_intellect = raw[0x11] & 0x0ff;
		m_dexterity = raw[0x12] & 0x0ff;
		m_constitution = raw[0x13] & 0x0ff;
		m_classType = raw[0x14];
		m_gold = ((raw[0x15] & 0x00FF) | raw[0x16] << 8) & 0x0FFFF;
		m_xp = ((raw[0x17] & 0x00FF) | raw[0x18] << 8) & 0x0FFFF;
		setItem(0, raw[0x19]);
		setItem(1, raw[0x1A]);
		setItem(2, raw[0x1B]);
		setItem(3, raw[0x1C]);
	}

	public void setCondition(int condition) {
		m_condition = condition;
	}

	public int getCondition() {
		return m_condition;
	}

	public void setMazeXp(int xp) {
		m_mazeXp = xp;
	}
	
	public int getMazeXp() {
		return m_mazeXp;
	}

	public void setSpellPoints(int points) {
		m_spellPoints = points;
	}

	public int getSpellPoints() {
		return m_spellPoints;
	}

	public void setArmorRating(int armorRating) {
		m_armorRating = armorRating;
	}
	
	public int getArmorRating() {
		return m_armorRating;
	}

	public void setName(String name) {
		m_name = name;
	}

	public boolean isValid() {
		return getNameAsBytes()[0] != 0x24;
	}

	public void setNumberOfStrikes(int i) {
		m_numberOfStrikes = i;
	}
	
	public int getNumberOfStrikes() {
		return m_numberOfStrikes;
	}

	public void setSpellNumber(int i) {
		m_spellNumber = i;
	}
	
	public int getSpellNumber() {
		return m_spellNumber;
	}
}
