package dominion.card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dominion.ClientTurn;
import dominion.DominionGUI;
import dominion.ServerTurn;
import dominion.Turn;
import dominion.DominionGUI.SelectionType;
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.DecisionAndPlayerDecision;
import dominion.card.Decision.EnumDecision;
import dominion.card.Decision.ListAndOptionsDecision;
import dominion.card.Decision.NumberDecision;
import dominion.card.Decision.SingleCardDecision;
import dominion.card.Decision.TrashThenGainDecision;
import dominion.card.Decision.firstSecond;
import dominion.card.Decision.keepDiscard;
import dominion.card.Decision.minionDecision;
import dominion.card.Decision.stewardDecision;
import dominion.card.Decision.yesNo;
import dominion.card.Decision.TrashThenGainDecision.WhichDecision;

public interface Card extends Serializable, Comparable<Card> {
	public static final TreasureCard[] treasureCards = {new Copper(), new Silver(), new Gold()};
	public static final VictoryCard[] victoryCards = {new Estate(), new Duchy(), new Province()};
	public static final Card curse = new Curse();

	public static final Card[] mustUse = { 

	};

	public static final Card[] baseRandomizerDeck = {
		new Chapel(), new Cellar(), new Moat(), 
		new Chancellor(), new Village(), new Woodcutter(), new Workshop(),
		new Bureaucrat(), new Feast(), new Gardens(), new Militia(), 
		new Moneylender(), new Remodel(), new Smithy(), new Spy(),
		new CouncilRoom(), new Festival(), new Laboratory(), new Library(),
		new Market(), new Mine(), new Witch(),
		new Adventurer()
	};
	public static final Card[] intrigueRandomizerDeck = {
		new Courtyard(), new GreatHall(), new ShantyTown(), new Steward(),
		new Baron(), new Conspirator(), new Coppersmith(), new Ironworks(),
		new MiningVillage(), new SeaHag(),
		new Duke(), new Minion(), new Tribute(), new Upgrade(),
		new Harem()
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
	public abstract static class SwitchByType extends DefaultCard {
		// used by both Tribute and Ironworks when you get bonuses based on type
		public void switchHelper(Turn turn, Decision decision, int numCards, int numGain) {
			System.out.println("doing continueProcessing for a tribute");
			List<Card> list = ((CardListDecision) decision).list;
			if(list.size()!=numCards) return;//TODO do something smarter here?  not sure what
			for(int i = 0; i < numCards; i++) {
				Card c = list.get(i);
				//if both the same, only do it once, only applicable for Tribute
				if(i == 1 && c.equals(list.get(0))) break;
				//note, not else if -- if multiple it gets all of the bonuses!
				if(c instanceof ActionCard) turn.addActions(numGain);
				if(c instanceof VictoryCard) turn.drawCards(numGain);
				if(c instanceof TreasureCard) turn.addBuyingPower(numGain);
				//curses get nothing, as do nulls (i.e. if no cards were left in deck)
			}
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
	public class Chapel extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public void playCard(Turn turn) { 
			if(turn instanceof ServerTurn) {
				CardListDecision decision;
				do {
					//request decision
					decision = (CardListDecision) ((ServerTurn)turn).getDecision(this, null);
					//try using this decision, if it doesn't work, ask again
				} while(decision.list.size() > 4 || !((ServerTurn)turn).trashCardsFromHand(decision, this));
			}
			//on the client side, just wait for the decision request
		}
		@Override public int getCost() { return 2; }

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			//sets the GUI in motion
			gui.setupCardSelection(4, false, SelectionType.trash, null);
		}
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			gui.trashCardSelection(playerNum, (CardListDecision)decision);
		}
	}	

	public class Cellar extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public void playCard(Turn turn) { 
			turn.addActions(1);
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				CardListDecision decision;
				// request cards to discard and attempt to discard them til you get a list that's valid
				while(!st.discardCardsFromHand(decision = (CardListDecision) st.getDecision(this, null), this));
				// now draw as many cards as you discarded
				st.drawCards(decision.list.size());
			}
			// on the client side, just wait for the decision request
		}
		@Override public int getCost() { return 2; }
		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			// sets the GUI in motion
			gui.setupCardSelection(-1, false, SelectionType.discard, null);
		}
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			// TODO: should this be discard, not trash? look into this.
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
	public class Chancellor extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		// TODO: give error if not the right kind of decision?
		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			turn.addBuyingPower(2);
			if(turn instanceof ServerTurn) {
				Decision d = ((ServerTurn) turn).getDecision(this, null);
				if(((EnumDecision<yesNo>)d).enumValue == yesNo.yes)
					((ServerTurn) turn).discardDeck();
			}
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			// Note: nothing to do here unless we add visuals for size of deck/discard
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			gui.makeMultipleChoiceDecision("Do you want to put your deck into your discard pile?", yesNo.class, null);
		}
	}

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

	public class Workshop extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, null);
					// if you tried to gain some number other than 1, it costs more than 4, or the one you wanted 
					// isn't in the supply, request a new one, otherwise we're done
				} while(decision.list.size() != 1 || decision.list.get(0).getCost() > 4 
						|| !st.gainCard(decision.list.get(0)));
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupGainCard(4, false, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			/* server handles sending gain message */
		}
	}

	//fours
	public class Bureaucrat extends VictorySelectionCard implements AttackCard, DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public Decision reactToCard(ServerTurn turn) {
			// The turn here is the turn of the reacting player, not the one who played the card
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
				CardListDecision decision;
				// there'd better be exactly 1, keep prompting till it is, also must be a victory card,
				// and in the player's hand
				while(((decision = (CardListDecision)turn.getDecision(this, null))).list.size() != 1 ||
						!(decision.list.get(0) instanceof VictoryCard) || !turn.inHand.contains(decision.list.get(0)));
				turn.putOnDeckFromHand(decision.list.get(0));
			}
			// don't need to communicate anything back to the caller directly
			return null;
		}

		@Override public void playCard(Turn turn) { 
			if(turn instanceof ServerTurn) {
				// Card.treasureCards[1] is the single instance of Card.Silver
				((ServerTurn) turn).putCardOnTopOfDeck(Card.treasureCards[1]);
				((ServerTurn) turn).doInteraction(this);
			}
			//reaction code takes care of the rest
		}

		//this will be called on the gui of any opponent with multiple victory cards
		@Override public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupCardSelection(1, true, SelectionType.undraw, this);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) { 
			// server will send message to remove
		}
	}

	public class Feast extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, null);
					// if you tried to gain some number other than 1, it costs more than 5, or the one you wanted 
					// isn't in the supply, request a new one, otherwise we're done and we trash the feast
				} while(decision.list.size() != 1 || decision.list.get(0).getCost() > 5 
						|| !st.gainCard(decision.list.get(0)));
				st.trashCardFromPlay(this);
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupGainCard(5, false, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			/* server handles sending gain message */
		}
	}

	public static class Gardens extends DefaultCard implements ConditionalVictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }
		@Override public int getVictoryPoints() { return 0; }
		@Override public int getVictoryPoints(Stack<Card> deck) { return deck.size()/10; }
	}

	public class Militia extends DefaultCard implements AttackCard, DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public Decision reactToCard(ServerTurn turn) {
			// The turn here is the turn of the reacting player, not the one who played the card
			// if you're already at or below 3 cards, no effect
			if(turn.inHand.size() <= 3) return null;
			
			CardListDecision decision;
			int numToDiscard = turn.inHand.size() - 3;
			// there'd better be exactly enough to get you down to 3, and you must actually have the cards you sent in your hand
			while(((decision = (CardListDecision)turn.getDecision(this, new NumberDecision(numToDiscard)))).list.size() 
					!= numToDiscard 
					|| !turn.discardCardsFromHand(decision, this));
			return null;
		}

		@Override public void playCard(Turn turn) { 
			turn.addBuyingPower(2);
			if(turn instanceof ServerTurn) ((ServerTurn) turn).doInteraction(this);
		}

		//this will be called on the gui of any opponent with multiple victory cards
		@Override public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupCardSelection(((NumberDecision)decision).num, true, SelectionType.discard, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) { 
			// TODO: should this be discard, not trash? look into this.
			gui.trashCardSelection(playerNum, (CardListDecision)decision);
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
	
	public class Remodel extends TreasureSelectionCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				TrashThenGainDecision ttgd = new TrashThenGainDecision();
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// prompt til you get 1 card that is in the player's hand
				} while(decision.list.size() != 1 || !turn.inHand.contains(decision.list.get(0)));
				Card toTrash = decision.list.get(0);
				st.trashCardFromHand(toTrash);
				st.sendDecisionToPlayer(this, new ListAndOptionsDecision(ttgd, decision));
				
				ttgd = new TrashThenGainDecision(toTrash);
				do {
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// prompt til you get 1 card that's still available and not too expensive
				} while(decision.list.size() != 1 || decision.list.get(0).getCost() > toTrash.getCost() + 2
						|| !st.gainCard(decision.list.get(0)));
				// the gain happens in the while condition
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			TrashThenGainDecision dec = (TrashThenGainDecision) decision;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.setupCardSelection(1, true, SelectionType.trash, null);
			} else {
				gui.setupGainCard(dec.toTrash.getCost() + 2, false, this);
			}
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			ListAndOptionsDecision lod = (ListAndOptionsDecision) decision;
			TrashThenGainDecision dec = lod.ttgd;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.trashCardFromHand(playerNum, lod.cld.list.get(0));
			} else {
				// should never actually get here, no confirmation sent for 
				// this part since it's just a normal gain
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

	public class Spy extends DefaultCard implements AttackCard, DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(1);
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn) turn;
				List<Decision> decisions = ((ServerTurn) turn).doInteraction(this);
				//Add in my own card!
				decisions.add(new SingleCardDecision(st.revealTopCard()));
				for(int i = 0; i < st.numPlayers(); i++) {
					int playerNum = (st.playerNum() + i + 1)%st.numPlayers();
					if(decisions.get(i) == null) continue; //this means they blocked the attack
					
					Decision d = st.getDecision(this, new DecisionAndPlayerDecision(decisions.get(i), playerNum));
					if(((EnumDecision<keepDiscard>)d).enumValue == keepDiscard.keep) 
						st.getTurn(playerNum).putCardOnTopOfDeck(((SingleCardDecision)decisions.get(i)).card);
					else st.getTurn(playerNum).discardCardPublically(((SingleCardDecision)decisions.get(i)).card);
				}
			}
		}

		@Override
		public Decision reactToCard(ServerTurn turn) {
			// this is called on the opponents, but the opponent doesn't need to make any decisions
			return new SingleCardDecision(turn.revealTopCard());
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			DecisionAndPlayerDecision dapd = (DecisionAndPlayerDecision) decision;
			String name = gui.getPlayerName(dapd.playerNum);
			String pronoun = "his/her";
			if(gui.getLocalPlayer() == dapd.playerNum) {
				name = "You";
				pronoun = "your";
			}
			gui.makeMultipleChoiceDecision("" + name + " revealed the following card.  " +
					"Do you want to put it back on " + pronoun + " deck (keep) or discard it (discard)?", 
					keepDiscard.class, ((SingleCardDecision)dapd.decision).card);
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
			if(turn instanceof ServerTurn) ((ServerTurn) turn).doInteraction(this);
		}
		@Override
		public Decision reactToCard(ServerTurn turn) {
			turn.drawCards(1);
			return null;
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

	public class Library extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		// TODO: give error if not the right kind of decision?
		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn) turn;
				List<Card> setAside = new ArrayList<Card>();
				while(st.inHand.size() < 7) {
					Card c = st.lookAtTopCard(); // pops top card off deck
					if(c == null) break; // all cards are in hand or set-aside list, no more to draw
					if(c instanceof ActionCard) {
						Decision d = ((ServerTurn) turn).getDecision(this, new SingleCardDecision(c));
						if(((EnumDecision<keepDiscard>)d).enumValue == keepDiscard.discard) {
							setAside.add(c);
							continue;
						}
					}
					st.putCardInHand(c);
				}
				for(Card c : setAside) st.discardCard(c);
			}
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			// Note: nothing to do here unless we add visuals for "set aside"
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			gui.makeMultipleChoiceDecision("Do you want to set aside this action (discard) or draw it into your hand (keep)?", 
						keepDiscard.class, ((SingleCardDecision)decision).card);
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

	public class Mine extends TreasureSelectionCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				TrashThenGainDecision ttgd = new TrashThenGainDecision();
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// if you tried to trash some number other than 1, or it's not a treasure, 
					// request a new one, otherwise we move on to the gaining bit
				} while(decision.list.size() != 1 || !(decision.list.get(0) instanceof TreasureCard)
						|| !turn.inHand.contains(decision.list.get(0)));
				TreasureCard toTrash = (TreasureCard) decision.list.get(0);
				st.trashCardFromHand(toTrash);
				st.sendDecisionToPlayer(this, new ListAndOptionsDecision(ttgd, decision));
				
				ttgd = new TrashThenGainDecision(toTrash);
				do {
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// if you tried to gain some number other than 1, or it's not a treasure, or none are left 
					// request a new one, otherwise go ahead and gain it!
				} while(decision.list.size() != 1 || !(decision.list.get(0) instanceof TreasureCard)
						|| decision.list.get(0).getCost() > 3 + toTrash.getCost()
						|| !st.gainCardToHand(decision.list.get(0)));
				st.sendDecisionToPlayer(this, new ListAndOptionsDecision(ttgd, decision));
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			TrashThenGainDecision dec = (TrashThenGainDecision) decision;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.setupCardSelection(1, false, SelectionType.trash, this);
			} else {
				gui.setupGainCard(dec.toTrash.getCost() + 3, false, this);
			}
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			ListAndOptionsDecision lod = (ListAndOptionsDecision) decision;
			TrashThenGainDecision dec = lod.ttgd;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.trashCardFromHand(playerNum, lod.cld.list.get(0));
			} else {
				gui.addCardToHand(playerNum, lod.cld.list.get(0));
			}
		}
	}

	public class Witch extends DefaultCard implements AttackCard {
		private static final long serialVersionUID = 1L;

		@Override
		public Decision reactToCard(ServerTurn turn) {
			turn.gainCurse();
			return null;
		}

		@Override public void playCard(Turn turn) { 
			turn.drawCards(2); 
			if(turn instanceof ServerTurn) ((ServerTurn) turn).doInteraction(this);
		}
		@Override public int getCost() { return 5; }
	}

	public class Adventurer extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 6; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				
				List<Card> revealedCards = new ArrayList<Card>();
				List<Card> treasureToPickUp = new ArrayList<Card>();
				// reveal cards one at a time until you get 2 treasures
				while(treasureToPickUp.size()<2) {
					Card c = st.revealTopCard();
					if(c instanceof TreasureCard) {
						treasureToPickUp.add(c);
						// this sends a message to all the players
						st.putCardInHand(c);
					} else
						revealedCards.add(c);
				}
				// now discard all the revealed cards
				for(Card c : revealedCards)
					st.discardCard(c);
			}
		}
	}

	//Intrigue
	public class Courtyard extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 2; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(3);
			if(turn instanceof ServerTurn) {  
				ServerTurn st = (ServerTurn)turn;
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, null);
					// if you tried to put back some number other than 1, or it's not in your hand
					// request a new one, otherwise we're done 
				} while(decision.list.size() != 1 || !st.putOnDeckFromHand(decision.list.get(0)));
			}
			//wait for processing code to ask for discard
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupCardSelection(1, true, SelectionType.undraw, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			//server will send message to make it happen
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

	public class Steward extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 3; }

		// TODO: give error if not the right kind of decision?
		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				Decision d = ((ServerTurn) turn).getDecision(this, new EnumDecision<firstSecond>(firstSecond.first));
				if(((EnumDecision<stewardDecision>)d).enumValue == stewardDecision.draw)
					turn.drawCards(2);
				else if(((EnumDecision<stewardDecision>)d).enumValue == stewardDecision.money)
					turn.addBuyingPower(2);
				else if(((EnumDecision<stewardDecision>)d).enumValue == stewardDecision.trash) {
					CardListDecision decision;
					do {
						//request decision
						decision = (CardListDecision) ((ServerTurn)turn).getDecision(this, new EnumDecision<firstSecond>(firstSecond.second));
						//try using this decision, if it doesn't work, ask again
					} while(decision.list.size() != 2 || !((ServerTurn)turn).trashCardsFromHand(decision, this));
					// trashCardsFromHands sends a confirmation to the client
				}
				((ServerTurn)turn).sendDecisionToPlayer(this, d);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			if(decision instanceof CardListDecision)
				gui.trashCardSelection(playerNum, (CardListDecision)decision);
			else if(decision instanceof EnumDecision<?> && ((EnumDecision<?>)decision).enumValue instanceof stewardDecision){
				if(((EnumDecision<stewardDecision>)decision).enumValue == stewardDecision.draw)
					turn.drawCards(2);
				else if(((EnumDecision<stewardDecision>)decision).enumValue == stewardDecision.money)
					turn.addBuyingPower(2);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			if(((EnumDecision<firstSecond>)decision).enumValue == firstSecond.first)
				gui.makeMultipleChoiceDecision("Do you want to trash 2 cards, draw two cards, or gain 2 coin?", stewardDecision.class, null);
			else
				gui.setupCardSelection(2, true, SelectionType.trash, null);
		}
	}

	public class Baron extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@SuppressWarnings("unchecked")
		@Override
		// TODO: perhaps make a setting that allows you to always discard the estate if you
		// have one?  Going strictly by the rules I have to ask, but as a player it's super
		// annoying and I'd never play it to gain an estate when I have one I could discard.
		public void playCard(Turn turn) {
			turn.addBuys(1);
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				if(st.containsCard(Card.victoryCards[0])) {
					Decision d = null;
					while(d == null || !(d instanceof EnumDecision<?>))
						d = (EnumDecision<yesNo>) ((ServerTurn) turn).getDecision(this, null);
					st.sendDecisionToPlayer(this, d); //auto
					if(((EnumDecision<yesNo>)d).enumValue == yesNo.yes) {
						st.discardCardFromHand(Card.victoryCards[0]);
						st.addBuyingPower(4);
						return;
					}
				}
				st.gainCard(Card.victoryCards[0]);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			if(((EnumDecision<yesNo>)decision).enumValue == yesNo.yes) {
				turn.discardCardFromHand(Card.victoryCards[0]);
				turn.addBuyingPower(4);
			}
			// Note: if the answer was no, the server deals with gaining the estate
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			gui.makeMultipleChoiceDecision("Do you want to discard an Estate?", yesNo.class, null);
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
	
	public class Coppersmith extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn)
				((ServerTurn) turn).addCoppersmith();
		}
	}

	public class Ironworks extends SwitchByType implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, null);
					// if you tried to gain some number other than 1, it costs more than 4, or the one you wanted 
					// isn't in the supply, request a new one, otherwise we're done and we reap the benefits
				} while(decision.list.size() != 1 || decision.list.get(0).getCost() > 4 
						|| !st.gainCard(decision.list.get(0)));
				switchHelper(turn, decision, 1, 1);
				st.sendDecisionToPlayer(this, decision);
			}
		}


		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.setupGainCard(4, false, null);
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			/* server handles sending gain message, so don't worry about that bit here */
			switchHelper(turn, decision, 1, 1);
		}
	}

	public class MiningVillage extends DefaultCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(2);
			Decision d = null;
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn) turn;
				while(d == null || !(d instanceof EnumDecision<?>))
					d = (EnumDecision<yesNo>) ((ServerTurn) turn).getDecision(this, null);
				st.sendDecisionToPlayer(this, d);
				if(((EnumDecision<yesNo>)d).enumValue == yesNo.yes) {
					st.trashCardFromPlay(this);
					st.addBuyingPower(2);
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum,
				Decision decision, ClientTurn turn) {
			if(((EnumDecision<yesNo>)decision).enumValue == yesNo.yes) {
				turn.trashCardFromPlay(this);
				turn.addBuyingPower(2);
			}
		}
		
		@Override
		public void createAndSendDecisionObject(DominionGUI gui,
				Decision decision) {
			gui.makeMultipleChoiceDecision("Do you want to trash this card for 2 coin?", yesNo.class, null);
		}
	}

	public class SeaHag extends DefaultCard implements AttackCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public Decision reactToCard(ServerTurn turn) {
			turn.discardCard(turn.revealTopCard());
			turn.putCardOnTopOfDeck(Card.curse);
			return null;
		}

		@Override public void playCard(Turn turn) { 
			if(turn instanceof ServerTurn) ((ServerTurn) turn).doInteraction(this);
		}
	}

	public static class Duke extends DefaultCard implements ConditionalVictoryCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }
		@Override public int getVictoryPoints() { return 0; }
		@Override 
		public int getVictoryPoints(Stack<Card> deck) { 
			int count = 0;
			for(Card c : deck) {
				if(c instanceof Duchy) count++;
			}
			return count;
		}
	}

	public class Minion extends DefaultCard implements DecisionCard, AttackCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@SuppressWarnings("unchecked")
		@Override
		public void playCard(Turn turn) {
			turn.addActions(1);
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn) turn;
				Decision d = st.getDecision(this, null);
				if(((EnumDecision<minionDecision>)d).enumValue == minionDecision.money)
					turn.addBuyingPower(2);
				else if(((EnumDecision<minionDecision>)d).enumValue == minionDecision.redraw) {
					st.discardHand();
					st.drawCards(4);
					st.doInteraction(this);
				}
				// note: not strictly necessary, since the buyingPower calculated
				// on the client side is never used...
				st.sendDecisionToPlayer(this, d);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			if(((EnumDecision<minionDecision>)decision).enumValue == minionDecision.money)
				turn.addBuyingPower(2);
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			gui.makeMultipleChoiceDecision("Do you want 2 coin, or do you want to discard your hand and draw 4 new cards, attacking the other players?", minionDecision.class, null);
		}

		@Override
		public Decision reactToCard(ServerTurn turn) {
			if(turn.inHand.size() >= 5) {
				turn.discardHand();
				turn.drawCards(4);
			}
			return null;
		}
	}

	public class Tribute extends SwitchByType implements InteractingCard, DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			if(turn instanceof ServerTurn) ((ServerTurn) turn).doInteraction(this);
		}

		@Override
		public Decision reactToCard(ServerTurn turn) {
			//if you are player to the left, send your top two cards
			if((turn.currentPlayer() + 1)%turn.numPlayers() == turn.playerNum()) {
				ArrayList<Card> list = new ArrayList<Card>();
				for(int i = 0; i < 2; i++) list.add(turn.revealTopCard());
				for(int i = 0; i < 2; i++) turn.discardCard(list.get(i));
				CardListDecision cld = new CardListDecision(list);
				//just do do it directly here, and send a decision confirmation to the current player
				switchHelper(turn.currentTurn(), cld, 2, 2);
				turn.currentTurn().sendDecisionToPlayer(this, cld);
			}
			//everyone else just ignores it
			return null;
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			// no decision needed from the GUI for this card			
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			switchHelper(turn, decision, 2, 2);
		}

	}

	public class Upgrade extends TreasureSelectionCard implements DecisionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 5; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(1);
			turn.addActions(1);
			if(turn instanceof ServerTurn) {
				ServerTurn st = (ServerTurn)turn;
				TrashThenGainDecision ttgd = new TrashThenGainDecision();
				CardListDecision decision;
				do {
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// prompt til you get 1 card that is in the player's hand
				} while(decision.list.size() != 1 || !turn.inHand.contains(decision.list.get(0)));
				Card toTrash = decision.list.get(0);
				st.trashCardFromHand(toTrash);
				st.sendDecisionToPlayer(this, new ListAndOptionsDecision(ttgd, decision));

				// if there are no cards costing exactly one more, we're done, nothing left to do
				// TODO: maybe send a message telling the user there were no cards with that price
				// to gain so they don't get anything?
				if(!st.supplyContainsExactCost(toTrash.getCost() + 1)) return;
				
				// otherwise, gain one!
				ttgd = new TrashThenGainDecision(toTrash);
				do {
					
					decision = (CardListDecision) st.getDecision(this, ttgd);
					// prompt til you get 1 card that's not too expensive and still available
				} while(decision.list.size() != 1 || decision.list.get(0).getCost() != toTrash.getCost() + 1 
						|| !st.gainCard(decision.list.get(0)));
				// the gain happens in the while condition
			}
		}

		@Override
		public void createAndSendDecisionObject(DominionGUI gui, Decision decision) {
			TrashThenGainDecision dec = (TrashThenGainDecision) decision;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.setupCardSelection(1, true, SelectionType.trash, null);
			} else {
				gui.setupGainCard(dec.toTrash.getCost() + 1, true, this);
			}
		}

		@Override
		public void carryOutDecision(DominionGUI gui, int playerNum, Decision decision, ClientTurn turn) {
			ListAndOptionsDecision lod = (ListAndOptionsDecision) decision;
			TrashThenGainDecision dec = lod.ttgd;
			if(dec.whichDecision == WhichDecision.chooseTrash) {
				gui.trashCardFromHand(playerNum, lod.cld.list.get(0));
			} else {
				// we don't actually get a message for this
			}
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
