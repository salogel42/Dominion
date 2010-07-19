package dominion.card;

import dominion.Turn;

public interface ActionCard extends Card {
	public void playCard(Turn turn);
}
