package dominion;

import dominion.card.ActionCard;
import dominion.card.Card;


//TODO: maybe use this or something like it on the server-side when AI is introduced
public abstract class Player {
	public final int playerNumber;
	
	public Player(int playerNumber) { 
		this.playerNumber = playerNumber; 
	}
	
	abstract ActionCard chooseActionToPlay();
	abstract Card chooseCardToTrash();
	abstract Card chooseCardToGainOrBuy(int upperLimit);

}
