package dominion.card;

import dominion.DominionGUI;


public interface DecisionCard extends ActionCard {

	public void createAndSendDecisionObject(DominionGUI gui);
	public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision);
}
