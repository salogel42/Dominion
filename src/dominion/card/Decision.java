package dominion.card;

import java.io.Serializable;
import java.util.List;

import dominion.Game.CardStack;

public interface Decision extends Serializable {
	public static class GainDecision implements Decision {
		private static final long serialVersionUID = 1L;
		public final int upperLimit;
		public final int numGains;
		public GainDecision(int upperLimit, int numGains) {
			this.upperLimit = upperLimit;
			this.numGains = numGains;
		}
		@Override
		public String toString() {
			return "(GainDecision with numGains " + numGains + " and upperLimit " + upperLimit+ ")";
		}
		//TODO flesh out
	}
	
	//TODO this is a bit hacky since there is no real decision... but whatever
	public static class StackDecision implements Decision {
		private static final long serialVersionUID = 1L;
		public final List<CardStack> stacks;
		public StackDecision(List<CardStack> stacks) {
			this.stacks = stacks;
		}
		@Override 
		public String toString() { return stacks.toString(); }
	}
	
	public static class CardListDecision implements Decision {
		private static final long serialVersionUID = 1L;
		public final List<Card> list;
		public CardListDecision(List<Card> list) {
			this.list = list;
		}	
		@Override
		public String toString() {
			return "(CardListDecision with list " + list + ")";
		}
	}
	
	public static class HandSelectionDecision implements Decision {
		private static final long serialVersionUID = 1L;
		public final int min, max;
		public HandSelectionDecision(int min, int max) {
			this.min = min;
			this.max = max;
		}		
		@Override
		public String toString() {
			return "(HandSelectionDecision with min="+min+" max="+max+")";
		}
	}
}
