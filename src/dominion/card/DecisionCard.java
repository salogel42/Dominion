package dominion.card;

import dominion.OooyGUI;


public interface DecisionCard extends ActionCard {

	public void createAndSendDecisionObject(OooyGUI gui);
	public void carryOutDecision(OooyGUI gui, int playerNum, Decision decision);
}
