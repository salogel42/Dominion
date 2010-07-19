package dominion;

import dominion.card.Card;
import dominion.card.Decision.CardListDecision;

public interface DominionGUI {
	public void setupCardSelection(int upperLimit, boolean exact);
	
	public void trashCardSelection(int playerNum, CardListDecision cld);
	public void trashCard(int playerNum, Card c);

}
