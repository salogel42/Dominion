package dominion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import dominion.RemoteMessage.Action;
import dominion.card.ActionCard;
import dominion.card.AttackCard;
import dominion.card.Card;
import dominion.card.ConditionalVictoryCard;
import dominion.card.Decision;
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.GainDecision;
import dominion.card.Decision.StackDecision;
import dominion.card.InteractingCard;
import dominion.card.ReactionCard;
import dominion.card.VictoryCard;

public class Game implements StreamListener, Runnable {

	PlayerInfo[] players;
	BlockingQueue<RemoteMessage> messageQ = new LinkedBlockingQueue<RemoteMessage>();
	
	public static class CardStack implements Serializable, Comparable<CardStack>{
		private static final long serialVersionUID = -7828182798286455385L;
		
		Card type;
		int numLeft;
		
		public CardStack(Card type, int numLeft) {
			this.type = type;
			this.numLeft = numLeft;
		}
		public boolean isEmpty() {
			return numLeft == 0;
		}
		@Override
		public String toString() {
			return "There are " + numLeft + " " + type + "s left";
		}
		@Override
		public int compareTo(CardStack other) {
			return type.compareTo(other.type);
		}
	}
	
	public class PlayerInfo {
		public final Streams streams;
		public final int playerNum;
		private Stack<Card> deck = new Stack<Card>();
		private Stack<Card> discard = new Stack<Card>();
		ServerTurn nextTurn;
		
		
		public PlayerInfo(Streams s, int playerNum) {
			this.playerNum = playerNum;
			streams = s;
			streams.addInListener(Game.this);
			setupStartingDeck();
			nextTurn = new ServerTurn(this);
			nextTurn.drawCards(5);
			streams.start();  //TODO: maybe do this elsewhere?
		}
		
		//must stay in sync with HumanPlayer.getStartingHand
		public void setupStartingDeck() {
			for(int i = 0; i < Card.startingHand.length; i++) {
				deck.push(Card.startingHand[i]);
			}
			Collections.shuffle(deck);
		}
		
		public void drawCards(int numCards) {
			for(int i = 0; i < numCards; i++) drawCard();
		}
		
		public Card getTopCard() {
			if(deck.isEmpty()) {
				deck.addAll(discard);
				discard.clear();
				Collections.shuffle(deck);
				//notify all players that you had to shuffle 
				sendShuffled();
			}
			if(!deck.isEmpty()) { //i.e. there was something in discard
				return deck.pop();
			}
			return null;
		}
		public void drawCard() {
			Card c = getTopCard();
			if(c != null) {
				nextTurn.inHand.add(c);
				sendCardToHand(c);
			}
		}

		//Caller's responsibility to ensure card is gainable
		public void gainCard(Card bc) {
			discard.add(bc);
		}
		
		public Card getCardFromSupply(Card c)
		{
			CardStack cs = getCardStack(c);
			if(!cs.isEmpty()) {
				cs.numLeft--;
				return c;
			}
			return null;
		}
		
		public CardStack getCardStack(Card c) {
//			System.out.println("Server: Searching supply for: " + c + " with upperLimit " + upperLimit);
			for(CardStack cs: Game.this.stacks) {
//				System.out.println("Server: Looking at stack " + cs);
				if(cs.type.equals(c)) {
//					System.out.println("Server: found match in stack " + cs);
					return cs;
				}
			}
			return null;
		}

		List<Decision> doInteraction(InteractingCard ic) {
			// TODO: for most interacting cards, this will be full of nulls,
			// so kinda a waste of space, perhaps detect that before creating a list?
			List<Decision> result = new ArrayList<Decision>();
			for(int i = (playerNum + 1) % numPlayers(); i != playerNum; i = (i + 1)%numPlayers()) {
				boolean react = true;
				if(ic instanceof AttackCard) {
					for(Card rc : players[i].nextTurn.inHand)
						if(rc instanceof ReactionCard)
							react = ((ReactionCard)rc).reaction(players[i].nextTurn);
				}
				if(react) result.add(ic.reactToCard(players[i].nextTurn));
				else result.add(null);
//				System.out.println("Server: Player " + i + " should have reacted? " + react);
			}
			return result;
		}

		public int numPlayers() { return Game.this.players.length; }
		
		public void sendPlay(ActionCard c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.playCard, playerNum, c, null));
		}

		public void sendDeckReveal(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.revealFromDeck, playerNum, c, null));
		}

		public void sendHandReveal(CardListDecision cld) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.revealFromHand, playerNum, null, cld));
		}

		public void sendGain(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.gainCard, playerNum, c, null));
		}

		public void sendShuffled() {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.cardsWereShuffled, playerNum, null, null));
		}
		private void sendEndTurn() {
			for(PlayerInfo p : players)
				p.streams.sendMessage(new RemoteMessage(Action.endTurn, playerNum, null, null)); 
		}

		public void sendPutOnDeck(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.putOnDeck, playerNum, c, null));
		}

		public void sendPutOnDeckFromHand(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.putOnDeckFromHand, playerNum, c, null));
		}

		public void sendPutInHand(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.putInHand, playerNum, c, null));
		}

		public void sendDiscardCard(Card c) {
			for(PlayerInfo pi : Game.this.players)
				pi.streams.sendMessage(new RemoteMessage(Action.discardCard, playerNum, c, null));
		}

		//assumes caller has already removed it from appropriate place
		public void trashCard(Card c) {
			trash.add(c);
		}

		//assumes caller has already removed it from appropriate place
		public void discardCard(Card c) {
			discard.add(c);
		}

		public void discardCardPublically(Card c) {
			discardCard(c);
			sendDiscardCard(c);
			
		}

		public void discardDeck() {
			discard.addAll(deck);
			deck.clear();
		}

		public void cleanup() {
			discard.addAll(nextTurn.inPlay);
			discard.addAll(nextTurn.inHand);
//			nextTurn.inHand.clear();
			sendEndTurn();
			nextTurn = new ServerTurn(this);
			nextTurn.drawCards(5);
			Game.this.nextPlayer();
			//TODO: Outpost?
		}

		private void sendCardToHand(Card c) {
			RemoteMessage rm = new RemoteMessage(Action.addCardToHand, playerNum, c, null);
			System.out.println("Server: sending card to player " + rm);
			//TODO send to everyone that you got a card
			streams.sendMessage(rm);
		}
		
		@Override
		public String toString() {
			return playerNum + " " + streams + " " + deck + " "  + nextTurn;
		}
		
		public ServerTurn currentTurn() { return Game.this.players[currentPlayer()].nextTurn; }
		public ServerTurn getTurn(int player) { return Game.this.players[player].nextTurn; }
		public int currentPlayer() { return Game.this.currPlayer; }
		public void putCardOnTopOfDeck(Card c, boolean fromHand) { 
			deck.push(c); 
			if(fromHand) sendPutOnDeckFromHand(c);
			else sendPutOnDeck(c);
		}
		
		public Card getPlay() {
			streams.sendMessage(new RemoteMessage(Action.chooseAction, playerNum, null, null));
			RemoteMessage m = null;
			while(m == null || m.action != Action.playCard || m.playerNum != playerNum) {
				try {
					m = Game.this.messageQ.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(m!=null && (m.action != Action.playCard || m.playerNum != playerNum)) 
					System.out.println("was expecting a playCard from player "+ playerNum + ", got message: " + m);
			}
			return m.card;
		}
		
		public List<Card> getBuys(int upperLimit, int numGains) {
			streams.sendMessage(new RemoteMessage(Action.chooseBuy, playerNum, null, new GainDecision(upperLimit, numGains)));
			RemoteMessage m = null;
			while(m == null || m.action != Action.buyCards || m.playerNum != playerNum) {
				try {
					m = Game.this.messageQ.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(m!=null && (m.action != Action.buyCards || m.playerNum != playerNum)) 
					System.out.println("was expecting a buyCards from player "+ playerNum + ", got message: " + m);
			}
			return ((CardListDecision)m.decisionObject).list;
		}

		// d will often be null, this param is to allow the card to give info as to which 
		// decision needs to be made in the case that there are multiple decisions
		// to be made for this card.
		public Decision getDecision(Card c, Decision d) {
			streams.sendMessage(new RemoteMessage(Action.makeDecision, playerNum, c, d));
			RemoteMessage m = null;
			while(m == null || m.action != Action.sendDecision || m.playerNum != playerNum) {
				try {
					m = Game.this.messageQ.take();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(m!=null && (m.action != Action.sendDecision || m.playerNum != playerNum)) 
					System.out.println("was expecting a sendDecision from player "+ playerNum + ", got message: " + m);
			}
			return m.decisionObject;
		}
		
	}
	private Stack<Card> trash;
	private ArrayList<CardStack> stacks;
	private List<Card> randomizerDeck = new ArrayList<Card>();
	
	public Game(Streams[] playerStreams) {
		this.players = new PlayerInfo[playerStreams.length];
		for(int i = 0; i < players.length; i++) {
			players[i] = new PlayerInfo(playerStreams[i], i);

			System.out.println(players[i]);
		}
		
		//TODO let person who starts the game decide what set(s) they are using, for now use all
		randomizerDeck.addAll(Arrays.asList(Card.baseRandomizerDeck));
		randomizerDeck.addAll(Arrays.asList(Card.intrigueRandomizerDeck));
		randomizerDeck.addAll(Arrays.asList(Card.seasideRandomizerDeck));
//		randomizerDeck.addAll(Arrays.asList(Card.alchemyRandomizerDeck));
		
		setupStacks();
		sendStacks();

		currPlayer = 0;
	}
	
	private int currPlayer;
	public void nextPlayer() {
		currPlayer = (currPlayer + 1) % players.length;
	}
	public void run() {
		while(!gameIsOver()) {
			// Note: takeTurn advances currPlayer appropriately (or at least it should)
			players[currPlayer].nextTurn.takeTurn();
		}
		tallyScores();
		//TODO: start new game now?
	}
	
	/* setup the supply stacks */
	private void setupStacks() {
		trash = new Stack<Card>(); //empty to start
		stacks = new ArrayList<CardStack>(18);

		//setup the trash, copper, silver, gold (TODO: how many of each should there be?)
		for(int i = 0; i < Card.treasureCards.length; i++) 
			stacks.add(new CardStack(Card.treasureCards[i], 100));

		//setup the curses and the victory cards
		stacks.add(new CardStack(Card.curse, (players.length-1)*10));
		int numVictory = (players.length == 2)?8:12;//TODO: for now, only support up to 4
		for(int i = 0; i < Card.victoryCards.length; i++) 
			stacks.add(new CardStack(Card.victoryCards[i], numVictory));

		Collections.shuffle(randomizerDeck);
		
		ArrayList<CardStack> randomized = new ArrayList<CardStack>();
		//TODO probably get rid of this eventually, for now, use for testing
		for(int i = 0; i < Card.mustUse.length; i++) {
			int numCards = (Card.mustUse[i] instanceof VictoryCard)? numVictory : 10;
			randomized.add(new CardStack(Card.mustUse[i], numCards));
		}
		//pick 10 kinds from randomizerDeck
		//TODO: make constants for numStacks, num cards in non-victory stack
		for(int i = Card.mustUse.length; i < 10; i++) {
			int numCards = (randomizerDeck.get(i) instanceof VictoryCard)? numVictory : 10;
			randomized.add(new CardStack(randomizerDeck.get(i), numCards));
		}
		Collections.sort(randomized);
		
		stacks.addAll(randomized);
	}
	
	//must stay in sync with Canvas.recieveStacks
	private void sendStacks() {
		RemoteMessage rm = new RemoteMessage(Action.stack, -1, null, new StackDecision(stacks)); 
		for(PlayerInfo pi : players)
			pi.streams.sendMessage(rm);
	}

	private boolean gameIsOver() {
		//Note: Provinces are in the sixth position
		if(stacks.get(6).isEmpty()) return true;
		int emptyStacks = 0;
		for(CardStack cs : stacks)
			if(cs.isEmpty())
				emptyStacks++;
		return (emptyStacks > 2);
	}

	public void sendScores(ScoresObject scores) {
		//TODO hacky, using decisionobject field for scores..
		RemoteMessage rm = new RemoteMessage(Action.endScore, -1, null, scores);
		for(PlayerInfo pi : Game.this.players)
			pi.streams.sendMessage(rm);
	}
	
	// a bit hacky, not really a decision object logically...
	public static class ScoresObject implements Decision {
		private static final long serialVersionUID = 1L;
		int[] scores;
		int winnerIndex = 0;//TODO ties?

		public ScoresObject(int numPlayers) { scores = new int[numPlayers]; }
		public void setScore(int playerNum, int score) { scores[playerNum] = score; }
		public void setWinner(int playerNum) { winnerIndex = playerNum; }
		
		@Override public String toString() {
			String result = "Scores: " + "[";
			for(int score : scores)
				result += score + " ";
			return  result + "]. Winner: " + winnerIndex;
		}
	}
	

	private void tallyScores() {
		int highScore = Integer.MIN_VALUE;
		int winnerIndex = 0;
		ScoresObject scores = new ScoresObject(players.length);
		for(int i = 0; i < players.length; i++) {
			int playerScore = 0;
			players[i].deck.addAll(players[i].discard);
			players[i].deck.addAll(players[i].nextTurn.inHand);
			players[i].deck.addAll(players[i].nextTurn.inPlay);
			
			for(Card c : players[i].deck) {
				if(c instanceof VictoryCard) {
					if(c instanceof ConditionalVictoryCard) 
						playerScore += ((ConditionalVictoryCard) c).getVictoryPoints(players[i].deck);
					else playerScore += ((VictoryCard) c).getVictoryPoints();
				}
			}
			System.out.println("Server: Player " + i + " had the following deck: " + players[i].deck);
			System.out.println("Server: Player " + i + " had " + playerScore + " points");
			scores.setScore(i, playerScore);
			if(playerScore > highScore) {
				highScore = playerScore;
				winnerIndex = i;
			}
		}
		scores.setWinner(winnerIndex);
		System.out.println("Player " + winnerIndex + " won with " + highScore + " points");
		
		sendScores(scores);
	}
	
	
	@Override
	public void recieveMessage(RemoteMessage message) {
		System.out.println("Server: Got message: " + message);
		try {
			messageQ.put(message);
		} catch (InterruptedException e) {
			// TODO mer? what should i do here?
			e.printStackTrace();
		}
	}
}
