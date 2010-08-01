package dominion.card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import dominion.DominionGUI;
import dominion.DominionGUI.SelectionType;
import dominion.ServerTurn;
import dominion.Turn;
import dominion.card.Decision.CardListDecision;

public interface Card extends Serializable, Comparable<Card> {
	public static final TreasureCard[] treasureCards = {new Copper(), new Silver(), new Gold()};
	public static final VictoryCard[] victoryCards = {new Estate(), new Duchy(), new Province()};
	public static final Card curse = new Curse();

	public static final Card[] mustUse = { 
	};

	public static final Card[] baseRandomizerDeck = {
		new Chapel(), new Cellar(), new Moat(), 
		new Village(), new Woodcutter(), 
		new Bureaucrat(), new Feast(), new Moneylender(), new Smithy(),
		new CouncilRoom(), new Festival(), new Laboratory(), new Market(),
		new Witch()
	};
	public static final Card[] intrigueRandomizerDeck = {
		new Courtyard(), new GreatHall(), new ShantyTown(), new Conspirator(), new SeaHag(), new Tribute(), new Harem()
	};
	public static final Card[] seasideRandomizerDeck= {
		new Bazaar()
	};
//	public static final Card[] alchemyRandomizerDeck= {
//	};
	
	public static final Card[] startingHand = new Card[10];

	public int getCost();

	@SuppressWarnings("serial")
	public abstract static class DefaultCard implements Card {
		@Override public String toString() { return this.getClass().getSimpleName(); }
		@Override public boolean equals(Object other) { return (other.getClass() == this.getClass()); }
		@Override
		public int compareTo(Card other) {
			if(getCost() == other.getCost()) {
				return toString().compareTo(other.toString());
			}
			return getCost() - other.getCost();
		}
	}

	@SuppressWarnings("serial")
	public abstract static class VictorySelectionCard extends DefaultCard implements SelectionCard {
		@Override public boolean isSelectable(Card c) { return c instanceof VictoryCard; }
	}

	@SuppressWarnings("serial")
	public abstract static class TreasureSelectionCard extends DefaultCard implements SelectionCard {
		@Override public boolean isSelectable(Card c) { return c instanceof TreasureCard; }
	}

	public static class Copper extends DefaultCard implements TreasureCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 0; }
		@Override public int getValue() { return 1; }
	}
	public static class Silver extends DefaultCard implements TreasureCard{
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }
		@Override public int getValue() { return 2; }
	}
	public static class Gold extends DefaultCard implements TreasureCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 6; }
		@Override public int getValue() { return 3; }
	}

	public static class Estate extends DefaultCard implements VictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 2; }
		@Override public int getVictoryPoints() { return 1; }
	}

	public static class Duchy extends DefaultCard implements VictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }
		@Override public int getVictoryPoints() { return 3; }
	}
	public static class Province extends DefaultCard implements VictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 8; }
		@Override public int getVictoryPoints() { return 6; }
	}

	//TODO maybe should implement its own "CurseCard" type?
	public static class Curse extends DefaultCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 0; }
		public int getVictoryPoints() { return -1; }
	}

	//Base set 
	
	//twos
	public class Chapel extends DefaultCard implements ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public void playCard(Turn turn) { 
			if(turn instanceof ServerTurn) {
				((ServerTurn) turn).setInProgress(this);
			}
			//on the client side, just wait for the decision request
		}
		@Override public int getCost() { return 2; }
		@Override
		public void startProcessing(ServerTurn turn) {
				turn.requestDecision(this);
		}
		@Override
		public void createAndSendDecisionObject(DominionGUI gui) {
			//sets the GUI in motion
			gui.setupCardSelection(4, false, SelectionType.trash, null);
		}
		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			System.out.println("processing chapel");
			if(((CardListDecision)decision).list.size() <= 4) {
				if(turn.trashCardsFromHand((CardListDecision)decision))
					turn.doneProcessing();
			}
		}
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) {
			gui.trashCardSelection(playerNum, (CardListDecision)decision);
		}
	}	

	public class Cellar extends DefaultCard implements ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public void playCard(Turn turn) { 
			turn.addActions(1);
			if(turn instanceof ServerTurn) {
				((ServerTurn) turn).setInProgress(this);
			}
			//on the client side, just wait for the decision request
		}
		@Override public int getCost() { return 2; }
		@Override public void startProcessing(ServerTurn turn) { turn.requestDecision(this); }
		@Override
		public void createAndSendDecisionObject(DominionGUI gui) {
			//sets the GUI in motion
			gui.setupCardSelection(-1, false, SelectionType.discard, null);
		}
		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			System.out.println("processing Cellar");
			CardListDecision cld = (CardListDecision)decision;
			int numCards = cld.list.size();
			if(turn.discardCardsFromHand(cld)) {
				turn.drawCards(numCards);
				turn.doneProcessing();
			}
		}
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) {
			gui.trashCardSelection(playerNum, (CardListDecision)decision);
		}
	}	

	public class Moat extends DefaultCard implements ReactionCard {
		private static final long serialVersionUID = 1L;
		@Override public boolean reaction(Turn turn) { return false; }
		@Override public void playCard(Turn turn) { turn.drawCards(2); }
		@Override public int getCost() { return 2; }
	}	
	
	//threes
	public class Village extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(2);
		}
	}

	public class Woodcutter extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		@Override
		public void playCard(Turn turn) {
			turn.addBuys(1);
			turn.addBuyingPower(2);
		}
	}

	//fours
	public class Bureaucrat extends VictorySelectionCard implements AttackCard, ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void reactToCard(ServerTurn turn) {
			int numVictory = 0;
			Card firstVictory = null;
			for(Card c : turn.inHand)
				if(c instanceof VictoryCard) {
					if(numVictory == 0) firstVictory = c;
					numVictory++;
				}
			if(numVictory == 0) turn.revealHand();
			else if(numVictory == 1) turn.putOnDeckFromHand(firstVictory);
			else {
				turn.requestDecision(this);
				//this is a bit weird, setting something inProgress for non-current player
				turn.setInProgress(this);
				return;
			}
			turn.doneReacting();
		}

		@Override public void playCard(Turn turn) { 
			if(turn instanceof ServerTurn) {
				((ServerTurn) turn).putCardOnTopOfDeck(Card.treasureCards[1]);
				((ServerTurn) turn).setInProgress(this);
			}
			//reaction code takes care of the rest
		}

		//this will be called on the gui of any opponent with multiple victory cards
		@Override public void createAndSendDecisionObject(DominionGUI gui) {
			gui.setupCardSelection(1, true, SelectionType.undraw, this);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) { 
			// server will send message to remove
		}

		@Override
		public void startProcessing(ServerTurn turn) { /* nothing to do */	}

		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) { 
			// see if everyone else is done
			if(turn.playerNum() == turn.currentPlayer()) {
				System.out.println("continued on curr player");
				if(turn.isDoneReacting())
					turn.doneProcessing();
				return;
			}
			// there'd better be exactly 1, reprompt if not
			if(((CardListDecision)decision).list.size() != 1) turn.requestDecision(this);
			else {
				turn.putOnDeckFromHand(((CardListDecision)decision).list.get(0));
				turn.doneProcessingOutOfTurn();
			}
		}

	}

	public class Feast extends DefaultCard implements ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				((ServerTurn) turn).setInProgress(this);
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui) {
			gui.setupGainCard(5, false, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) {
			/* server handles sending gain message */
		}

		@Override
		public void startProcessing(ServerTurn turn) {
			turn.requestDecision(this);
		}

		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			//woo short-circuit evaluation! 
			// if you tried to gain some number other than 1, or the one you wanted
			// isn't in the supply, request a new one, otherwise we're done and we trash the feast
			if(((CardListDecision)decision).list.size() != 1 || 
					!turn.gainCard(((CardListDecision)decision).list.get(0)))
				turn.requestDecision(this);
			else {
				turn.trashCardFromHand(this);
				turn.doneProcessing();
			}
		}
	}
	
	public class Moneylender extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			//TODO maybe each card should be a singleton with a getInstance() method?
			//TODO should you be allowed to play Moneylender if no copper in hand?
			//		I think it should be ok from card text, do rules check
			if(turn.containsCard(Card.treasureCards[0])) {
				turn.trashCardFromHand(Card.treasureCards[0]);
				turn.addBuyingPower(3);
			}
		}
	}
	
	public class Smithy extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(3);
		}
	}

	//fives
	public class CouncilRoom extends DefaultCard implements InteractingCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }
		@Override
		public void playCard(Turn turn) {
			turn.drawCards(4);
			turn.addBuys(1);
		}
		@Override
		public void reactToCard(ServerTurn turn) {
			turn.drawCards(1);
		}
		
	}
	public class Festival extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			turn.addActions(2);
			turn.addBuys(1);
			turn.addBuyingPower(2);
		}
	}

	public class Laboratory extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(2);
			turn.addActions(1);
		}
	}

	public class Market extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(1);
			turn.addBuys(1);
			turn.addBuyingPower(1);
		}
	}


	public class Witch extends DefaultCard implements AttackCard {
		private static final long serialVersionUID = 1L;

		@Override
		public void reactToCard(ServerTurn turn) {
			turn.gainCurse();
		}

		@Override public void playCard(Turn turn) { turn.drawCards(2); }
		@Override public int getCost() { return 5; }
	}

	//Intrigue
	public class Courtyard extends DefaultCard implements ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 2; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(3);
			if(turn instanceof ServerTurn) { ((ServerTurn) turn).setInProgress(this); }
			//wait for processing code to ask for discard
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui) {
			gui.setupCardSelection(1, true, SelectionType.undraw, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) {
			//server will send message to make it happen
		}

		@Override
		public void startProcessing(ServerTurn turn) {
			turn.requestDecision(this);
		}

		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			// if not exactly 1 card, try again
			if(((CardListDecision)decision).list.size() != 1) turn.requestDecision(this);
			else {
				turn.putOnDeckFromHand(((CardListDecision)decision).list.get(0));
				turn.doneProcessing();
			}
		}
	}

	public class GreatHall extends DefaultCard implements ActionCard, VictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(1);
		}

		@Override public int getVictoryPoints() {	return 1; }
	}

	public class ShantyTown extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		@Override
		public void playCard(Turn turn) {
			turn.addActions(2);
			if(!turn.actionsInHand()) {
				turn.revealHand();
				turn.drawCards(2);
			}
		}
	}
	
	public class Conspirator extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			turn.addBuyingPower(2);
			if(turn.inPlay.size() > 2) {
				turn.drawCards(1);
				turn.addActions(1);
			}
		}
	}

	public class SeaHag extends DefaultCard implements AttackCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void reactToCard(ServerTurn turn) {
			turn.discardCard(turn.revealTopCard());
			turn.putCardOnTopOfDeck(Card.curse);
		}

		@Override public void playCard(Turn turn) { /* it's all in the reaction */ }
	}

	public class Tribute extends DefaultCard implements InteractingCard, ComplexDecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) { ((ServerTurn) turn).setInProgress(this); }
			//now wait for the reaction
		}

		@Override
		public void reactToCard(ServerTurn turn) {
			//if you are player to the left, send your top two cards
			if((turn.currentPlayer() + 1)%turn.numPlayers() == turn.playerNum()) {
				ArrayList<Card> list = new ArrayList<Card>();
				for(int i = 0; i < 2; i++) list.add(turn.revealTopCard());
				for(int i = 0; i < 2; i++) turn.discardCard(list.get(i));
				CardListDecision cld = new CardListDecision(list);
				//just do do it directly rather than sending things back and forth
				this.continueProcessing(turn.currentTurn(), cld);
			}
			//everyone else just ignores it
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui) {
			// no decision needed from the GUI for this card			
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision) {
			// everything the tribute does will be put into effect by the continueProcessing method
		}

		@Override
		public void startProcessing(ServerTurn turn) { /* reaction method handles getting the "decision" */ }

		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			System.out.println("doing continueProcessing for a tribute");
			List<Card> list = ((CardListDecision) decision).list;
			if(list.size()!=2) return;//TODO do something smarter here?  not sure what
			for(int i = 0; i < 2; i++) {
				Card c = list.get(i);
				//if both the same, only do it once
				if(i == 1 && c.equals(list.get(0))) break;
				//note, not else if -- if multiple it gets all of the bonuses!
				if(c instanceof ActionCard) turn.addActions(2);
				//TODO need to communicate the additional actions to the clientturn!
				if(c instanceof VictoryCard) turn.drawCards(2);
				if(c instanceof TreasureCard) turn.addBuyingPower(2);
				//curses get nothing, as do nulls (i.e. if no cards were left in deck)
			}
			turn.doneProcessing();
		}

	}

	public class Harem extends DefaultCard implements VictoryCard, TreasureCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 6; }
		@Override public int getVictoryPoints() { return 2; }
		@Override public int getValue() { return 2; }
	}


	//Seaside
	public class Bazaar extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(2);
			turn.addBuyingPower(1);
		}
	}	
}
