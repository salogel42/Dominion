package dominion;

import dominion.card.ActionCard;
import dominion.card.Card;


public class ClientTurn extends Turn {

	private final DominionGUI gui;
	private final int playerNum;
	
	public ClientTurn(DominionGUI gui, int playerNum) {
		this.gui = gui;
		this.playerNum = playerNum;
	}
	@Override
	public void drawCards(int cards) { 
		//Do nothing, server handles this
	}

	@Override
	public void revealHand() {	
		//Again, do nothing, server handles this
	}
	
	@Override
	public void playCard(Card c) {
		//TODO maybe check that it is there and an ActionCard before calling?
		//Server should've already checked, but could've been an issue sending
		this.playHelper((ActionCard) c);
	}

	@Override
	public void trashCardFromHand(Card c) {
		//The GUI removes from inHand on your behalf, and does the display stuff
		gui.trashCard(playerNum, c);		
	}
	@Override
	public void discardCardFromHand(Card c) {
		gui.discardCard(playerNum, c);		
	}

}
