package dominion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dominion.Game.CardStack;
import dominion.Game.PlayerInfo;
import dominion.RemoteMessage.Action;
import dominion.card.ActionCard;
import dominion.card.Card;
import dominion.card.Decision;
import dominion.card.InteractingCard;
import dominion.card.TreasureCard;
import dominion.card.Decision.CardListDecision;

public class ServerTurn extends Turn {
	private PlayerInfo player;
	private boolean bpComputed = false;

	public ServerTurn(PlayerInfo p) {
		super();
		player = p;
	}

	@Override public void drawCards(int cards) { player.drawCards(cards); }
	
	
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

	public void takeTurn() {
		while(numActionsLeft > 0 && actionsInHand()) {
			//prompt to choose an action or decide not to
			Card play = player.getPlay();
			playCard(play);
		}
		computeBuyingPower();
		List<Card> buys;
		do {
			//prompt to buy a card or choose not to
			buys = player.getBuys(buyingPower, numBuysLeft);
		} while(!buyCards(buys));
		player.cleanup();
	}

	@Override
	public boolean playCard(Card c) {
		System.out.println("Attempting to play " + c);
		if(c == null) {
			numActionsLeft = 0;
			return true;
		} 
		for(int i = 0; i < inHand.size(); i++) {
			if(inHand.get(i) instanceof ActionCard && 
					inHand.get(i).toString().contains(c.toString())) {
				ActionCard ac = (ActionCard)inHand.get(i);
				player.sendPlay(ac);
				playHelper(ac);
				return true;
			}
		}
		return false;
		//TODO: send a "it wasn't valid" message?
	}

	//this message essentially confirms decision was legal, go ahead and show 
	//changes in the display
	private void sendConfirmDecision(Decision d, Card inProgress) {
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
	public boolean trashCardsFromHand(CardListDecision cld, Card inProgress) {
		System.out.println("Trashing these cards from hand " + cld.list);
		if(!checkCardListInHand(cld)) {
			return false;
		}
		for(Card c : cld.list) this.trashCardFromHand(c);
		System.out.println("Got through trashing");
		sendConfirmDecision(cld, inProgress);
		return true;
	}

	//card must verify appropriate number
	public boolean discardCardsFromHand(CardListDecision cld, Card inProgress) {
		System.out.println("Discarding cards from hand " + cld.list);
		if(!checkCardListInHand(cld)) {
			return false;
		}
		for(Card c : cld.list) this.discardCardFromHand(c);
		System.out.println("Got through discarding");
		sendConfirmDecision(cld, inProgress);
		return true;
	}

	public boolean buyCards(List<Card> list) {
		System.out.println("Attempting to buy cards: " + list);
		
		if(list.size() > numBuysLeft) {
			//TODO: else send a "it wasn't valid" message?
			return false;
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
				return false;
			}
			count = 0;
		}

		for(CardStack s : cslist) {
			player.gainCard(s.type);
			s.numLeft--;
			player.sendGain(s.type);			
		}
		
		numBuysLeft = 0;
		return true;
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

	// could also be called getTopCard, the looking is done on client side,
	// but i'm intending this to be used when you need to "look at" a card
	// note that this does actually remove it from the deck.
	public Card lookAtTopCard() {
		return player.getTopCard();
	}

	// note: this is NOT from the supply, if you want to 
	// add a card to the hand from the supply, use gainCardToHand
	public void putCardInHand(Card c) {
		inHand.add(c);
		player.sendPutInHand(c);
	}

	public boolean gainCardToHand(Card c) {
		if(player.getCardFromSupply(c) == null) return false;
		inHand.add(c);
		return true;
	}

	public boolean gainCard(Card c) {
		if(player.getCardFromSupply(c) == null) return false;
		player.gainCard(c);
		player.sendGain(c);
		return true;
	}
	
	public void gainCurse() {
		gainCard(Card.curse);
	}

	//Note: caller must verify presence of card
	@Override
	public void trashCardFromHand(Card c) {
		inHand.remove(c);
		player.trashCard(c);
	}

	//Note: caller must verify presence of card
	@Override
	public void trashCardFromPlay(Card c) {
		inPlay.remove(c);
		player.trashCard(c);
	}

	@Override
	public void discardCardFromHand(Card c) {
		inHand.remove(c);
		player.discardCard(c);
	}

	public void discardDeck() {
		player.discardDeck();
	}

	public boolean putOnDeckFromHand(Card c) { 
		if(inHand.remove(c)) {
			player.putCardOnTopOfDeck(c, true); 
			return true;
		}
		return false;
	}

	public void putCardOnTopOfDeck(Card c) { player.putCardOnTopOfDeck(c, false); }
	public void discardCard(Card c) { player.discardCard(c); }
	public void discardCardPublically(Card c) { player.discardCardPublically(c); }

	public ServerTurn currentTurn() { return player.currentTurn(); }
	public ServerTurn getTurn(int playerNum) { return player.getTurn(playerNum); }
	public int currentPlayer() { return player.currentPlayer(); }
	public int numPlayers() { return player.numPlayers(); }
	public int playerNum() { return player.playerNum; }
	
	public Decision getDecision(Card cardToMakeDecisionFor, Decision decision) { 
		return player.getDecision(cardToMakeDecisionFor, decision); 
	}

	public void sendDecisionToPlayer(Card c, Decision d) {
		player.streams.sendMessage(new RemoteMessage(Action.sendDecision, player.playerNum, c, d));
	}

	public List<Decision> doInteraction(InteractingCard c) {
		return player.doInteraction(c);
	}
}
