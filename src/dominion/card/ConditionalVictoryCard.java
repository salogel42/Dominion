package dominion.card;

import java.util.Stack;

public interface ConditionalVictoryCard extends VictoryCard {
	public int getVictoryPoints(Stack<Card> deck);
}
