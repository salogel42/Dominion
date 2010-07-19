package dominion.card;

import dominion.ServerTurn;

public interface InteractingCard extends ActionCard {
	//This is called by all of the OTHER players when someone plays the card
	public void reactToCard(ServerTurn turn);
}
