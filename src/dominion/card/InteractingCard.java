package dominion.card;

import dominion.ServerTurn;

public interface InteractingCard extends ActionCard {
	//This is called on all of the OTHER players when someone plays the card
	public Decision reactToCard(ServerTurn turn);
}
