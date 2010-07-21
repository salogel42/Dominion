package dominion;

import dominion.card.Card;
import dominion.card.Decision.CardListDecision;

public interface DominionGUI {
	public enum SelectionType {trash, discard, undraw};
	public void setupCardSelection(int upperLimit, boolean exact, SelectionType type);
	
	public void trashCardSelection(int playerNum, CardListDecision cld);
	public void trashCard(int playerNum, Card c);
	public void discardCard(int playerNum, Card c);

}
