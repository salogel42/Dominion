package dominion.card;

import dominion.ClientTurn;
import dominion.DominionGUI;


public interface DecisionCard extends ActionCard {

	public void createAndSendDecisionObject(DominionGUI gui);
	
	// Most cards will not need this, but some unconventional ones will
	public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn);
}
