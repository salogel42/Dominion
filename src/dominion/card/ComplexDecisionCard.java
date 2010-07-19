package dominion.card;

import dominion.ServerTurn;

public interface ComplexDecisionCard extends DecisionCard {
	public void startProcessing(ServerTurn turn); //does this always just request a decision?
	public void continueProcessing(ServerTurn turn, Decision decision); //is this necessary?

}
