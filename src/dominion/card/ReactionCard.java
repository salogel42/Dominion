package dominion.card;

import dominion.Turn;

public interface ReactionCard extends ActionCard {
	//The boolean return value is true if you should react to an attack
	public boolean reaction(Turn turn);
}
