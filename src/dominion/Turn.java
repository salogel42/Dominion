package dominion;

import java.util.ArrayList;
import java.util.List;

import dominion.card.ActionCard;
import dominion.card.Card;

public abstract class Turn {
	protected int numBuysLeft;
	protected int numActionsLeft;
	protected int buyingPower;
	public List<Card> inPlay = new ArrayList<Card>();
	public List<Card> inHand = new ArrayList<Card>();
	
	public Turn() {
		numBuysLeft = 1;
		numActionsLeft = 1;
		buyingPower = 0;
	}

	public void addBuys(int buys) { numBuysLeft+=buys; }
	public void addActions(int actions) { numActionsLeft+=actions; }
	public void addBuyingPower(int bp) { buyingPower+=bp; }
	public abstract void drawCards(int cards);
	
	void addCardToHand(Card cm) {
		inHand.add(cm);
	}
	
	public abstract boolean actionsInHand();
	
	public abstract void revealHand();
	
	abstract public void playCard(Card c);
	
	protected void playHelper(ActionCard ac) {
		inHand.remove(ac);
		//TODO: swap order of next two lines?
		//if so, must change Conspirator implementation
		inPlay.add(ac);
		ac.playCard(this);
		numActionsLeft--;
	}


}
