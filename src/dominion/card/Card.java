package dominion.card;

import java.io.Serializable;

import dominion.OooyGUI;
import dominion.ServerTurn;
import dominion.Turn;
import dominion.card.Decision.CardListDecision;

public interface Card extends Serializable, Comparable<Card> {
	public static final TreasureCard[] treasureCards = {new Copper(), new Silver(), new Gold()};
	public static final VictoryCard[] victoryCards = {new Estate(), new Duchy(), new Province()};
	public static final Card curse = new Curse();
	public static final Card[] baseRandomizerDeck = {
		new Chapel(), new Moat(), 
		new Village(), new Woodcutter(), 
		new Smithy(),
		new Festival(), new Laboratory(), new Market(),
		new Witch()
	};
	public static final Card[] intrigueRandomizerDeck = {
		new GreatHall(), new ShantyTown(), new Conspirator(), new Harem()
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
		public void createAndSendDecisionObject(OooyGUI gui) {
			//sets the GUI in motion
			gui.setupCardSelection(4, false);
		}
		@Override
		public void continueProcessing(ServerTurn turn, Decision decision) {
			System.out.println("processing chapel");
			if(((CardListDecision)decision).list.size() <= 4)
				turn.trashCardsFromHand((CardListDecision)decision);
			turn.doneProcessing();
		}
		@Override
		public void carryOutDecision(OooyGUI gui, int playerNum, Decision decision) {
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
	public class Smithy extends DefaultCard implements ActionCard {
		private static final long serialVersionUID = 1L;
		@Override public int getCost() { return 4; }

		@Override
		public void playCard(Turn turn) {
			turn.drawCards(3);
		}
	}

	//fives
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
		public void reactToCard(Turn turn) {
			if(turn instanceof ServerTurn) {
				((ServerTurn)turn).gainCurse();
			} //do nothing if it's on client side
		}

		@Override public void playCard(Turn turn) { turn.drawCards(2); }
		@Override public int getCost() { return 5; }
	}

	//Intrigue
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
