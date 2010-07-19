package dominion;

import dominion.card.ActionCard;
import dominion.card.Card;


public class ClientTurn extends Turn {

	//Do nothing, server handles this
	@Override
	public void drawCards(int cards) { }

	//Again, do nothing, server handles this
	@Override
	public void revealHand() {	}

	@Override
	public boolean actionsInHand() { 
		// TODO obviously wrong but doesn't matter for client side
		return true;
	} 
	
	@Override
	public void playCard(Card c) {
		//TODO maybe check that it is there and an ActionCard before calling?
		this.playHelper((ActionCard) c);
	}
}
