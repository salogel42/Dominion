package dominion.card;

import dominion.DominionGUI;


public interface DecisionCard extends ActionCard {

	public void createAndSendDecisionObject(DominionGUI gui);
	
	//TODO: should this ever be necessary?  a lot of these happen through other server messages
	public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision);
}
