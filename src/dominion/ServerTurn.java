package dominion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dominion.Game.CardStack;
import dominion.Game.PlayerInfo;
import dominion.RemoteMessage.Action;
import dominion.card.ActionCard;
import dominion.card.Card;
import dominion.card.ComplexDecisionCard;
import dominion.card.Decision;
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.GainDecision;
import dominion.card.InteractingCard;
import dominion.card.TreasureCard;

public class ServerTurn extends Turn {
	private PlayerInfo player;
	private boolean bpComputed = false;

	private boolean[] reacted;
	public ComplexDecisionCard inProgress = null;
	
	public ServerTurn(PlayerInfo p) {
		super();
		player = p;
	}

	@Override public void drawCards(int cards) { player.drawCards(cards); }
	
	public void requestPlay() {
		player.streams.sendMessage(new RemoteMessage(Action.chooseAction, player.playerNum, null, null));
	}

	public void requestDecision(Card c) {
		player.streams.sendMessage(new RemoteMessage(Action.makeDecision, player.playerNum, c, null));
	}

	public void requestBuy(int upperLimit, int numGains) {
		player.streams.sendMessage(new RemoteMessage(Action.chooseBuy, player.playerNum, null, new GainDecision(upperLimit, numGains)));
	}
	
	//Note: it will only actually be computed once, no matter how many times it is called
	public void computeBuyingPower()
	{
		if(bpComputed) return;
		for(Card card : inHand) {
			if(card instanceof TreasureCard) 
				buyingPower += ((TreasureCard)card).getValue();
		}
		bpComputed = true;
	}

	public void startTurn() {
		//TODO: when I add duration, draw cards now
		continueTurn();
	}

	public void continueTurn() {
		if(inProgress != null) {
			return;
			//We've already sent the message requesting the decision, 
			//so hold off on doing more stuff 'til later
		}
		if(numActionsLeft > 0) {
			//prompt to choose an action or decide not to
			if(actionsInHand()) {
				requestPlay();
				return;
			}
			numActionsLeft = 0;
		}
		if(numBuysLeft > 0) {
			//prompt to buy a card or choose not to
			computeBuyingPower();
			requestBuy(buyingPower, numBuysLeft);
		} 
		else {
			player.cleanup();
		}
	}

	public void setInProgress(ComplexDecisionCard c) {
		inProgress = c;
		if(c instanceof InteractingCard && player.playerNum == player.currentPlayer()) {
			//these should all be false to begin with
			this.reacted = new boolean[player.numPlayers()];
			//except me
			reacted[player.playerNum] = true;
		}
		c.startProcessing(this);
	}

	public void doneProcessing() {
		inProgress = null;
		continueTurn();
	}
	public void doneProcessingOutOfTurn() {
		player.doneReacting();
		inProgress = null;
	}

	@Override
	public void playCard(Card c) {
		System.out.println("Attempting to play " + c);
		if(c == null) {
			numActionsLeft = 0;
			//TODO: set a "done with actions" bool?
		} else {
			for(int i = 0; i < inHand.size(); i++) {
				if(inHand.get(i) instanceof ActionCard && 
						inHand.get(i).toString().contains(c.toString())) {
					ActionCard ac = (ActionCard)inHand.get(i);
					playHelper(ac);
					player.sendPlay(ac);
					player.doInteraction(ac);
					break;
				}
			}
		}
		//TODO: send a "it wasn't valid" message?
		continueTurn();
	}

	//this message essentially confirms decision was legal, go ahead and show 
	//changes in the display
	private void sendConfirmDecision(Decision d) {
		player.streams.sendMessage(new RemoteMessage(Action.sendDecision, player.playerNum, inProgress, d));
	}

	private boolean checkCardListInHand(CardListDecision cld) {
		//TODO keep hand sorted all the time - i.e. before you send (so you don't 
		//call sort multiple times, may be a noop on later calls anyway, though)
		Collections.sort(inHand);
		Collections.sort(cld.list);
		//start at the beginning of both lists and try to match each member
		//of cld.list to one in inHand
		int l = 0;
		for(int h = 0; h < inHand.size() && l < cld.list.size(); h++) {
			if(inHand.get(h).compareTo(cld.list.get(l)) > 0) {
				//We got to something in the hand that skips past next one in the list
				return false;
			}
			if(inHand.get(h).equals(cld.list.get(l))) {
				//We found this one, we can move on
				l++;
			}
			//now we move on to the next one inHand (by means of for loop increment)
		}
		if(l < cld.list.size()) //didn't get all the way through
			return false;
		return true;
	}

	//card must verify appropriate number
	public boolean trashCardsFromHand(CardListDecision cld) {
		System.out.println("Trashing these cards from hand " + cld.list);
		if(!checkCardListInHand(cld)) {
			requestDecision(inProgress);
			return false;
		}
		for(Card c : cld.list) this.trashCard(c);
		System.out.println("Got through trashing");
		sendConfirmDecision(cld);
		return true;
	}

	//card must verify appropriate number
	public boolean discardCardsFromHand(CardListDecision cld) {
		System.out.println("Discarding cards from hand " + cld.list);
		if(!checkCardListInHand(cld)) {
			requestDecision(inProgress);
			return false;
		}
		for(Card c : cld.list) this.discardCardFromHand(c);
		System.out.println("Got through discarding");
		sendConfirmDecision(cld);
		return true;
	}

	public void buyCards(List<Card> list) {
		System.out.println("Attempting to buy cards: " + list);
		
		if(list.size() > numBuysLeft) {
			//TODO: else send a "it wasn't valid" message?
			continueTurn();
			return;
		}
			
		CardStack cs;
		List<CardStack> cslist = new ArrayList<CardStack>();
		Collections.sort(list);

		int upperLimit = buyingPower;
		int count = 0;
		for(int i = 0; i < list.size(); i++) {
			count++;
			if(i+1 < list.size() && list.get(i).equals(list.get(i+1))) 
				continue;
			
			if((cs = player.getCardStack(list.get(i))) != null && 
				cs.numLeft >= count && cs.type.getCost() <= upperLimit) {
				upperLimit -= cs.type.getCost();
				for(; count > 0; count--)
				  cslist.add(cs);
			}
			else {
				//TODO: else send a "it wasn't valid" message?
				continueTurn();
				return;
			}
			count = 0;
		}

		for(CardStack s : cslist) {
			player.gainCard(s.type);
			s.numLeft--;
			player.sendGain(s.type);			
		}
		
		numBuysLeft = 0;
		continueTurn();
	}
	

	@Override
	public void revealHand() {
		player.sendHandReveal(new CardListDecision(inHand));
	}

	public Card revealTopCard() {
		Card c =  player.getTopCard();
		player.sendDeckReveal(c);
		return c;
	}

	public void gainCurse() {
		if(player.getCardFromSupply(Card.curse) != null) {
			player.gainCard(Card.curse);
			player.sendGain(Card.curse);
		}
		
	}

	//Note: caller must verify presence of card
	@Override
	public void trashCard(Card c) {
		inHand.remove(c);
		player.trashCard(c);
	}

	@Override
	public void discardCardFromHand(Card c) {
		inHand.remove(c);
		player.discardCard(c);
	}

	public void putOnDeckFromHand(Card c) { 
		if(inHand.remove(c)) {
			player.putCardOnTopOfDeck(c, true); 
		}
	}

	public void putCardOnTopOfDeck(Card c) { player.putCardOnTopOfDeck(c, false); }
	public void discardCard(Card c) { player.discardCard(c); }

	public ServerTurn currentTurn() { return player.currentTurn(); }
	public int currentPlayer() { return player.currentPlayer(); }
	public int numPlayers() { return player.numPlayers(); }
	public int playerNum() { return player.playerNum; }
	public void doneReacting() { 
		player.doneReacting();
	}
	public void playerIsDoneReacting(int playerNum) {
		reacted[playerNum] = true;
		if(isDoneReacting()) {
			doneProcessing();
			continueTurn();
		}
	}
	public boolean isDoneReacting() { 
		for(boolean react : reacted) if(!react) return false;
		return true;
	}
}
