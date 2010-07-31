package dominion;

import dominion.card.Card;
import dominion.card.Decision.CardListDecision;
import dominion.card.SelectionCard;

public interface DominionGUI {
	public enum SelectionType {trash, discard, undraw};
	public void setupCardSelection(int upperLimit, boolean exact, SelectionType type, SelectionCard c);
	
	public void trashCardSelection(int playerNum, CardListDecision cld);
	public void trashCard(int playerNum, Card c);
	public void discardCard(int playerNum, Card c);

}
