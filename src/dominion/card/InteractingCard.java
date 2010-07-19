package dominion.card;

import dominion.Turn;

public interface InteractingCard extends ActionCard {
	public void reactToCard(Turn turn);
}
