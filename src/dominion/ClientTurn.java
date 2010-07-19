package dominion;

import dominion.card.ActionCard;
import dominion.card.Card;


public class ClientTurn extends Turn {

	@Override
	public void drawCards(int cards) { 
		//Do nothing, server handles this
	}

	@Override
	public void revealHand() {	
		//Again, do nothing, server handles this
	}

	@Override
	public boolean actionsInHand() { 
		//obviously wrong but doesn't matter for client side
		//TODO may need this to be right for some?  not currently
		return true;
	} 
	
	@Override
	public void playCard(Card c) {
		//TODO maybe check that it is there and an ActionCard before calling?
		//Server should've already checked, but could've been an issue sending
		this.playHelper((ActionCard) c);
	}
}
