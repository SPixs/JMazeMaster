package org.pixs.mazemaster;

public class MonsterType {
    private int monsterID;
    private String name;
    private int attackBonus;
    private int initialHP;
    private int armorRating;
    private byte[] spriteDescriptor; // 4 octets

    public MonsterType(int monsterID, String name, int attackBonus, int initialHP, int armorRating, byte[] spriteDescriptor) {
        this.monsterID = monsterID;
        this.name = name;
        this.attackBonus = attackBonus;
        this.initialHP = initialHP;
        this.armorRating = armorRating;
        this.spriteDescriptor = spriteDescriptor;
    }

    public int getMonsterID() { return monsterID; }
    public String getName() { return name; }
    public int getAttackBonus() { return attackBonus; }
    public int getInitialHP() { return initialHP; }
    public int getArmorRating() { return armorRating; }
    public byte[] getSpriteDescriptor() { return spriteDescriptor; }

    @Override
    public String toString() {
        return "MonsterType{" +
                "ID=" + monsterID +
                ", name='" + name + '\'' +
                ", attackBonus=" + attackBonus +
                ", initialHP=" + initialHP +
                ", armorRating=" + armorRating +
                ", spriteDescriptor=" + bytesToHex(spriteDescriptor) +
                '}';
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
