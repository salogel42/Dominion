package dominion;

import dominion.card.Decision.CardListDecision;

public interface OooyGUI {
	public void setupCardSelection(int upperLimit, boolean exact);
	
	public void trashCardSelection(int playerNum, CardListDecision cld);

}
