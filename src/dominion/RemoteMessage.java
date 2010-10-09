package dominion;

import java.io.Serializable;

import dominion.card.Card;
import dominion.card.Decision;
 
public class RemoteMessage implements Serializable {
	/**
	 * TODO: should this be something special?
	 */
	private static final long serialVersionUID = 4847579924414757687L;
	
	public static enum Action {stack, addCardToHand, chooseAction, playCard, chooseBuy, 
		buyCards, gainCard, endTurn, cardsWereShuffled, revealFromHand, revealFromDeck,
		endScore, makeDecision, sendDecision, putOnDeck, putOnDeckFromHand, putInHand,
		discardCard}
	public final Action action;
	public final int playerNum;
	public final Card card;
	public final Decision decisionObject;
	
	public RemoteMessage(Action action, int playerNum, Card card, Decision d) {
		this.action = action;
		this.playerNum = playerNum;
		this.card = card;
		this.decisionObject = d;
	}
	
	@Override public String toString()
	{
		return playerNum + " " + action + " " + card + " " + decisionObject;
	}
}
