package dominion;

import java.util.ArrayList;
import java.util.List;

import dominion.card.ActionCard;
import dominion.card.Card;

public abstract class Turn {
	
	//general info needed for every turn
	protected int numBuysLeft;
	protected int numActionsLeft;
	protected int buyingPower;
	public List<Card> inPlay = new ArrayList<Card>();
	public List<Card> inHand = new ArrayList<Card>();

	//Default starting values
	//TODO take in list of duration cards to activate at beginning of turn
	//	   or out of turn (Lighthouse)
	public Turn() {
		numBuysLeft = 1;
		numActionsLeft = 1;
		buyingPower = 0;
	}

	public void addBuys(int buys) { numBuysLeft+=buys; }
	public void addActions(int actions) { numActionsLeft+=actions; }
	public void addBuyingPower(int bp) { buyingPower+=bp; }
	
	void addCardToHand(Card cm) { inHand.add(cm); }
	
	public boolean actionsInHand() {
		for(Card card : inHand) {
			if(card instanceof ActionCard)
				return true;
		}
		return false;
	}
		
	//Functionality common to Client and Server implementations of playCard
	protected void playHelper(ActionCard ac) {
		inHand.remove(ac);
		//if these swap order, must change Conspirator implementation
		inPlay.add(ac);
		ac.playCard(this);
		numActionsLeft--;
	}

	public boolean containsCard(Card c) {
		for(Card hc : inHand)
			if(hc.equals(c))
				return true;
		return false;
	}

	//Things that work differently for Client and Server but are needed by both
	public abstract void drawCards(int cards);
	public abstract void playCard(Card c);
	public abstract void revealHand();
	public abstract void trashCardFromHand(Card c);
	public abstract void discardCardFromHand(Card c);

}
