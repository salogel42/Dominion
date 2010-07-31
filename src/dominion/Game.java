package dominion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import dominion.RemoteMessage.Action;
import dominion.card.ActionCard;
import dominion.card.AttackCard;
import dominion.card.Card;
import dominion.card.Decision;
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.StackDecision;
import dominion.card.InteractingCard;
import dominion.card.ReactionCard;
import dominion.card.VictoryCard;

public class Game implements StreamListener {

	PlayerInfo[] players;
	
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

		void doInteraction(ActionCard c) {
//			System.out.println("Server: About to check for interactivity on card " + c);
			if(c instanceof InteractingCard) {
//				System.out.println("Server: Detected interacting card");
				InteractingCard ic = (InteractingCard) c;
				for(int i = (playerNum + 1) % numPlayers(); i != playerNum; i = (i + 1)%numPlayers()) {
					boolean react = true;
					if(ic instanceof AttackCard) {
						for(Card rc : players[i].nextTurn.inHand)
							if(rc instanceof ReactionCard)
								react = ((ReactionCard)rc).reaction(players[i].nextTurn);
					}
					if(react) ic.reactToCard(players[i].nextTurn);
					System.out.println("Server: Player " + i + " should have reacted? " + react);
				}
			}

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

		//assumes caller has already removed it from appropriate place
		public void trashCard(Card c) {
			trash.add(c);
		}

		//assumes caller has already removed it from appropriate place
		public void discardCard(Card c) {
			discard.add(c);
		}

		public void cleanup() {
			discard.addAll(nextTurn.inPlay);
			discard.addAll(nextTurn.inHand);
//			nextTurn.inHand.clear();
			sendEndTurn();
			nextTurn = new ServerTurn(this);
			nextTurn.drawCards(5);
			//TODO: Outpost?
			if(gameIsOver()) {
				Game.this.tallyScores();
			} else {
				Game.this.nextPlayer();
				Game.this.continueGame();
			}
		}

		private void sendCardToHand(Card c) {
			RemoteMessage rm = new RemoteMessage(Action.addCardToHand, playerNum, c, null);
			System.out.println("Server: sending card to player " + rm);
			//TODO send to everyone that you got a card
			streams.sendMessage(rm);
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
		
		@Override
		public String toString() {
			return playerNum + " " + streams + " " + deck + " "  + nextTurn;
		}
		
		public ServerTurn currentTurn() { return Game.this.players[currentPlayer()].nextTurn; }
		public int currentPlayer() { return Game.this.currPlayer; }
		
		public void sendDecisionToGame(RemoteMessage rm) {
			Game.this.recieveMessage(rm);
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
		continueGame();
	}
	
	private int currPlayer;
	public void nextPlayer() {
		currPlayer = (currPlayer + 1) % players.length;
	}
	public void continueGame() {
		players[currPlayer].nextTurn.continueTurn();
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
	
	private void playCard(RemoteMessage message) {
		if(message.playerNum == currPlayer) {
			players[currPlayer].nextTurn.playCard(message.card);
		}//TODO: what if non-current player tries to play something?
		
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
				if(c instanceof VictoryCard)
					playerScore += ((VictoryCard) c).getVictoryPoints();
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
		switch(message.action) {
		case playCard: playCard(message); break;
		case buyCard:
			if(message.playerNum == currPlayer) {
				Decision.CardListDecision dec = (Decision.CardListDecision)message.decisionObject;  
				players[currPlayer].nextTurn.buyCards(dec.list);
			}//TODO: what if non-current player tries to buy something?
			break;
		case sendDecision:
			if(message.playerNum == currPlayer) {
				players[currPlayer].nextTurn.inProgress.continueProcessing(players[currPlayer].nextTurn, 
						message.decisionObject);
			}
			break;
		case makeDecision:
		case addCardToHand:
		case chooseAction:
		case chooseBuy:
		case stack:
		case endTurn:
		case cardsWereShuffled:
		case endScore:
			System.out.println("Server: The action " + message.action + " should never be sent to the server!  Something is wrong.");
			break;
		default: 
			System.out.println("Server: Missing a case:" + message.action + "!");
		}
	}
}
