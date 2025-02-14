package org.pixs.mazemaster.states;

import org.pixs.hardware.VicIIDisplay.ColorMode;
import org.pixs.mazemaster.Character;
import org.pixs.mazemaster.Game;

public class InitState extends GameState {

	public InitState(Game game) {
		super(game);
	}

	@Override
	public void enter() {
		
		getGame().initCharsetInRAM();
		getGame().getVicII().setMode(ColorMode.STANDARD_BITMAP);
		
		// Init triggers
		for (int i=0;i<5;i++) {
			byte[] triggers = new byte[256];
			int startAddress = 0xAB90 + 0x0200*i;
			for (int j=0;j<0x70;j++) {
				triggers[j] = getMem(startAddress+j);
			}
			getGame().setTriggers(i, triggers);
		}
		
		///////////
//		Character character = getGame().getCharacter(0);
//		character.setName("JOHN DOE");
//		character.setStrengh(18);
//		character.setIntellect(18);
//		character.setConstitution(128);
//		character.setDexterityt(18);
//		character.setXP(0);
//		character.setGold(65535);
//		character.setClassType(1);
//		
//		character = getGame().getCharacter(1);
//		character.setName("DR STRANGE");
//		character.setStrengh(18);
//		character.setIntellect(18);
//		character.setConstitution(255);
//		character.setDexterityt(18);
//		character.setXP(0);
//		character.setGold(65535);
//		character.setClassType(2);
		////////////
		
		setState(new MainMenuState(getGame()));
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub
		
	}

}
