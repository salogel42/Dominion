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
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.GainDecision;
import dominion.card.TreasureCard;

public class ServerTurn extends Turn {
	private PlayerInfo player;

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
	
	public void computeBuyingPower()
	{
		for(Card card : inHand) {
			if(card instanceof TreasureCard) 
				buyingPower += ((TreasureCard)card).getValue();
		}
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
			computeBuyingPower();
			numActionsLeft = 0;
		}
		if(numBuysLeft > 0) {
			//prompt to buy a card or choose not to
			requestBuy(buyingPower, numBuysLeft);
		} 
		else {
			player.cleanup();
		}
	}

	public void setInProgress(ComplexDecisionCard c) {
		inProgress = c;
		c.startProcessing(this);
	}

	public void doneProcessing() {
		inProgress = null;
		if(numActionsLeft == 0)
			computeBuyingPower();
		continueTurn();
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
		if(numActionsLeft == 0 && inProgress == null) {
			computeBuyingPower();
		}
		//TODO: send a "it wasn't valid" message?
		continueTurn();
	}

	//card must verify appropriate number
	public void trashCardsFromHand(CardListDecision cld) {
		System.out.println("Trashing cards from hand " + cld.list);
		for(Card c : cld.list)
			//TODO bah, need to check that the right number of them are around
			if(!inHand.contains(c)) {
				System.out.println("Oh no, tried to trash something not in hand!");
				return; //TODO send "bad decision" message
			}
		for(Card c : cld.list) {
			this.trashCard(c);
		}
		//TODO problem!!! May or may not have already computed buying power
		System.out.println("Got through trashing");
		player.streams.sendMessage(new RemoteMessage(Action.sendDecision, player.playerNum, inProgress, cld));
//		player.trashCard(c);
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
		//TODO: send server a message to send your hand to everyone else
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

}
