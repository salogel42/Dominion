package dominion.card;

import dominion.ClientTurn;
import dominion.DominionGUI;


public interface DecisionCard extends ActionCard {

	// Most cards will not have to send a decision along, but some cards need multiple decisions
	// so they need a way to distinguish one from another
	public void createAndSendDecisionObject(DominionGUI gui, Decision decision);
	
	// Most cards will not need this, but some unconventional ones will
	public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn);
}
