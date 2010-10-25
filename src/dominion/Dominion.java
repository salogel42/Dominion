package dominion;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import dominion.Game.CardStack;
import dominion.Game.ScoresObject;
import dominion.RemoteMessage.Action;
import dominion.card.ActionCard;
import dominion.card.Card;
import dominion.card.Decision;
import dominion.card.DecisionCard;
import dominion.card.SelectionCard;
import dominion.card.Decision.CardListDecision;
import dominion.card.Decision.EnumDecision;
import dominion.card.Decision.GainDecision;
import dominion.card.Decision.StackDecision;

@SuppressWarnings("serial")
public class Dominion extends JFrame implements StreamListener, ActionListener, DominionGUI {
	static final int DEFAULT_PORT = 39587;
	static final String HOME_PORT = "127.0.0.1";
	
	static final String IMAGE_PATH = "images/";

	static final int NUM_CARDS_IN_HAND = 5;

	private int windowWidth, windowHeight;

	public int localPlayer;
	private Streams streams;
	private int[] scores = null;
	private int winner;
	
	static enum GameState { none, waitingForAction, waitingForBuy, waitingForGain, waitingForSelectFromHand }
	GameState state = GameState.none;
	PlayerModel[] playerModels;
	List<CardStack> stacks;
	String[] names;
	
	List<Card> fromSupply = new ArrayList<Card>();
	List<HandCardButton> fromHand = new ArrayList<HandCardButton>();
		
	JDesktopPane desktopPane;
	JInternalFrame supplyFrame, handFrame, playFrame, messageBox;
	JEditorPane messagePane;
	String messageText = "";
	
	JLabel trash, message;
	JButton supply[];
	
	JButton nomoreAct, nomoreGain, clearGains, clearSelection, nomoreSelections ;
	JPanel handPane, playPane;
	
	static {
		for(int i = 0; i < 7; i++) 
			Card.startingHand[i] = Card.treasureCards[0];
		for(int i = 7; i < 10; i++) 
			Card.startingHand[i] = Card.victoryCards[0];
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) { /* if it didn't work, it didn't work. */ }

		new Dominion().setVisible(true);
	}

	public Dominion() {
		setTitle("Dominion!!!");
		setContentPane(desktopPane = new JDesktopPane());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //JFrame..DO_NOTHING_ON_CLOSE);
		windowWidth = 720;
		windowHeight = 280;
		this.setSize(windowWidth, windowHeight);

		messageBox = new JInternalFrame("Game Messages: ", true, false, true, true);
		desktopPane.add(messageBox);
		messageBox.setVisible(true);
		//TODO tweak the size/location
		messageBox.setSize(200, 200);
		messageBox.add(messagePane = new JTextPane());

		JMenuBar mb = new JMenuBar();
		mb.add(newMenu("Game", 'G',
				newItem("Start New Game", 'S', this, null),
				newItem("Join Game", 'J', this, null),
				null,
				newItem("Exit", 'x', this, null)));
		setJMenuBar(mb);
		
		JPanel pane = new JPanel(new GridLayout(2,0));
		
		supply = new JButton[17];
		pane.add(trash  = new JLabel("Trash"));
		trash.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(IMAGE_PATH + "trash.jpg").getScaledInstance(100, -1, 0)));
		trash.setVerticalTextPosition(SwingConstants.BOTTOM);
		trash.setHorizontalTextPosition(SwingConstants.CENTER);

	    pane.add(supply[0] = newButton("Copper", null, 'C', true, this));
		pane.add(supply[1] = newButton("Silver", null, 'S', true, this));
		pane.add(supply[2] = newButton("Gold",   null, 'G', true, this));

		for(int i = 7; i < 12; i++)	{
			pane.add(supply[i] = newButton("Card "+(i-6), null, '\0', true, this));
		}

		pane.add(supply[3] = newButton("Curse",    null,  '\0', true, this));
		pane.add(supply[4] = newButton("Estate",   null,   'E', true, this));
		pane.add(supply[5] = newButton("Duchy",    null,   'D', true, this));
		pane.add(supply[6] = newButton("Province", null, 'P', true, this));

		for(int i = 12; i < 17; i++) {
			pane.add(supply[i] = newButton("Card "+(i-6), null, '\0', true, this));
		}
		supplyFrame = new JInternalFrame("Supply", true, false, true, true);
		supplyFrame.getContentPane().add(BorderLayout.CENTER, pane);
		
		pane = new JPanel();
		pane.add(message = new JLabel("Message Space"));
		pane.add(clearGains = newButton("Clear Gains", null, '\0', true, this));
		pane.add(nomoreGain = newButton("Submit Gains", null, '\0', true, this));
		pane.add(clearSelection = newButton("Clear Selection", null, '\0', true, this));
		pane.add(nomoreSelections = newButton("Submit Selection", null, '\0', true, this));
		supplyFrame.getContentPane().add(BorderLayout.NORTH, pane);
	
		desktopPane.add(supplyFrame);
		supplyFrame.pack();
		supplyFrame.setLocation(0,0);
		
		handFrame = new JInternalFrame("Your hand: ", true, false, true, true);
		handFrame.getContentPane().add(BorderLayout.CENTER, handPane = new JPanel());
	
		pane = new JPanel();
		pane.add(nomoreAct = newButton("No More Actions", null, 'N', true, this));
		handFrame.getContentPane().add(BorderLayout.EAST, pane);
		
		desktopPane.add(handFrame);
		handFrame.pack();
		handFrame.setLocation(0, supplyFrame.getSize().height);

		playFrame = new JInternalFrame("Plays thus far: ", true, false, true, true);

		playFrame.getContentPane().add(BorderLayout.CENTER, playPane = new JPanel());
		
		desktopPane.add(playFrame);
		resetFrameLocations();
//		playFrame.pack();
//		playFrame.setLocation(handFrame.getSize().width, supplyFrame.getSize().height);
	}
	
	public void setupCanvas(String[] names, Streams streams, int playerNum) {
		localPlayer = playerNum;
		this.names = names;
		this.streams = streams;
		streams.addInListener(this);
		setTitle(names[localPlayer]);
		
		playerModels = new PlayerModel[names.length];
		for(int i = 0; i < names.length; i++)
			playerModels[i] = new PlayerModel(i);
		System.out.println(stacks);
		
		supplyFrame.setVisible(true);
		handFrame.setVisible(true);
		playFrame.setVisible(true);
		enableButtons(-1, null);
	}

	private void setupSupplyGUI() {
		for(int i = 0; i < stacks.size(); i++)
		{
			CardStack cs = stacks.get(i);
		  	supply[i].setText(cs.numLeft + " left");
		  	supply[i].setIcon(new ImageIcon(getImageForCard(cs.type)));
		  	supply[i].setToolTipText("<html><img src=\"" +
		  			"file:" + IMAGE_PATH + getImageNameForCard(cs.type) + 
		  			"\"> ");
		  	supply[i].setActionCommand("Gain " + i);
	  		supply[i].setEnabled(cs.numLeft > 0);
	  		supply[i].setMargin(new Insets(0, 0, 0, 0));
		}
		supplyFrame.pack();
		resetFrameLocations();
	}

	private void resetFrameLocations() {
		messageBox.setLocation(0,supplyFrame.getHeight());
		handFrame.pack();
		handFrame.setLocation(messageBox.getWidth(), supplyFrame.getHeight());
		playFrame.pack();
		playFrame.setLocation(messageBox.getWidth() + handFrame.getWidth(), supplyFrame.getHeight());
		int width = Math.max(supplyFrame.getWidth(), 
				messageBox.getWidth() + handFrame.getWidth() + playFrame.getWidth()) + 20;
		int height = supplyFrame.getHeight() + Math.max(handFrame.getHeight(), 
				playFrame.getHeight()) + 50;
		if(windowWidth < width || windowHeight < height) {
			this.setSize(windowWidth = Math.max(windowWidth, width), 
					windowHeight = Math.max(windowHeight, height));
		}
	}

	private void removeFromSupply(Card c) {
		for(int i = 0; i < stacks.size(); i++) {
			CardStack cs = stacks.get(i);
			if(cs.type.equals(c)) {
				cs.numLeft--;
			  	supply[i].setText(cs.numLeft + " left");
		  		supply[i].setEnabled(cs.numLeft > 0);
				break;
			}
		}
	}
	
	private void enableButtons(int upperLimit, SelectionCard selectable)
	{
		boolean tab = false;
		boolean han = false;
		boolean select = false;
		
		switch(state){
		case waitingForAction:
			han = true;
			break;
		case waitingForBuy:
		case waitingForGain:
			tab = true;
			nomoreGain.setText("Submit " + getLabelForState());
			clearGains.setText("Clear " + getLabelForState() + "s");
			
			break;	
		case waitingForSelectFromHand:
			select = true;
			break;
		case none:
			break;
		}
		
		nomoreAct.setVisible(han);
		nomoreGain.setVisible(tab);
		clearSelection.setVisible(select);
		nomoreSelections.setVisible(select); 
		//TODO set disabled if too many, too few are selected
		clearGains.setVisible(tab && !fromSupply.isEmpty());
		if(!tab || upperLimit == -1 || playerModels[localPlayer].turn.numBuysLeft <= fromSupply.size()) {
			for(int i = 0; i < supply.length; i++) {
				supply[i].setEnabled(false);
			}
		} else {
			for(int i = 0; i < supply.length; i++) {
				CardStack cs = stacks.get(i);
				supply[i].setEnabled(cs.type.getCost() <= upperLimit && !cs.isEmpty());
			}
		}
		
		int i = 0;
		for(Card c : playerModels[localPlayer].turn.inHand) {
			//TODO may need to add upperLimit to trash?
			handPane.getComponent(i).setEnabled((han && c instanceof ActionCard) || 
					((selectable != null) ? selectable.isSelectable(c) : select));
			i++;
		}
		supplyFrame.pack();
		resetFrameLocations();
		repaint();
	}
	
	private static class HandCardButton extends JButton {
		public final Card card;
		public HandCardButton(Card c, ActionListener l) {
			card = c;
			Icon icon = new ImageIcon(getImageForCard(c));
			this.setIcon(icon);
			this.addActionListener(l);
			this.setActionCommand("Play " + c);
			if(!(c instanceof ActionCard))
				this.setDisabledIcon(icon);
			this.setToolTipText("<html><img src=\"" +
		  			"file:" + IMAGE_PATH + getImageNameForCard(c) +	"\"> ");
			this.setEnabled(c instanceof ActionCard);
			this.setVerticalTextPosition(SwingConstants.BOTTOM);
			this.setHorizontalTextPosition(SwingConstants.CENTER);
			this.setMargin(new Insets(0, 0, 0, 0));
		}
	}
	private RemoteMessage.Action getActionForState() {
		switch(state) {
			case waitingForBuy: return Action.buyCards; 
			case waitingForGain: return Action.sendDecision;
			default: return null;
		}
	}

	private String getLabelForState() {
		switch(state) {
			case waitingForBuy: return "Buy"; 
			case waitingForGain: return "Gain";
			default: return null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String act = e.getActionCommand();
		RemoteMessage rm;
		
		switch(state){
		case waitingForAction: 
			if(act.equals("No More Actions"))
				rm = new RemoteMessage(Action.playCard, localPlayer, null, null);
			else if(e.getSource() instanceof HandCardButton)//act.startsWith("Play "))
				rm = new RemoteMessage(Action.playCard, localPlayer, ((HandCardButton)e.getSource()).card, null);
			else
				break;
			streams.sendMessage(rm);
			state = GameState.none;
			message.setText("");
			break;
		case waitingForBuy: case waitingForGain:
			if(act.equals("Submit " + getLabelForState()))
			{
				Decision dec = new Decision.CardListDecision(new ArrayList<Card>(fromSupply));
				rm = new RemoteMessage(getActionForState(), localPlayer, null, dec);
				streams.sendMessage(rm);
				state = GameState.none;
				message.setText("");
				fromSupply.clear();
			} else if(act.startsWith("Gain ")) { 
				int upperLimit = playerModels[localPlayer].turn.buyingPower;
				int buysLeft = playerModels[localPlayer].turn.numBuysLeft;
				String mess = "";
				fromSupply.add(stacks.get(Integer.parseInt(act.substring(5))).type);
				for(Card c : fromSupply) {
					upperLimit -= c.getCost();
					buysLeft--;
					mess += c + ", ";
				}
				mess = mess.substring(0, mess.length()-2);
				if(state == GameState.waitingForBuy)
					message.setText("Currently buying: "+mess+".  May buy " + buysLeft + " additional item(s) costing a total of " + upperLimit + ".");
				else 
					message.setText("Currently gaining: "+mess+".");
				enableButtons(upperLimit, null);
			} else if(act.equals("Clear " + getLabelForState() + "s")){
				fromSupply.clear();
				int upperLimit = playerModels[localPlayer].turn.buyingPower;
				int buysLeft = playerModels[localPlayer].turn.numBuysLeft;
				message.setText("May buy " + buysLeft + " item(s) costing a total of " + upperLimit + "."); 
				enableButtons(upperLimit, null);
			}
			
			break;
		case waitingForSelectFromHand:
			if(act.equals("Submit Selection"))
			{
				ArrayList<Card> toSend = new ArrayList<Card>();
				for(HandCardButton hcb : fromHand)
					toSend.add(hcb.card);
				Decision dec = new Decision.CardListDecision(toSend);
				//TODO is this the right place to send this?
				rm = new RemoteMessage(Action.sendDecision, localPlayer, null, dec);
				streams.sendMessage(rm);
				state = GameState.none;
				message.setText("");
				fromHand.clear();
			} else if(e.getSource() instanceof HandCardButton) {
				String mess = "";
				fromHand.add((HandCardButton) e.getSource());
				for(HandCardButton hcb : fromHand) {
					mess += hcb.card + ", ";
				}
				mess = mess.substring(0, mess.length()-2);
				message.setText("Currently selecting: "+mess+".");
				enableButtons(-1, null);
			} else if(act.equals("Clear Selection")){
				fromHand.clear();
				message.setText("Select some cards."); 
			}
			
			break;
		case none:
			if(act.equals("Start New Game")) {
				String name = JOptionPane.showInputDialog(this, "What is your name?");
				int numPlayers = -1;
				while(numPlayers < 2 || numPlayers > 4)
					numPlayers = Integer.parseInt(JOptionPane.showInputDialog(this, 
							"How many players (must be between 2 and 4, inclusive)?"));
				new ServerAccepter(numPlayers).start();
				new ClientHandler(name, HOME_PORT, DEFAULT_PORT).start();
			} else if(act.equals("Join Game")) {
				String name = JOptionPane.showInputDialog(this, "What is your name?");
				new ClientHandler(name, null, DEFAULT_PORT).start();
			}
			break;
		default:
			// do nothing				
		}
	}
	
	
	/* local player behaviors */

	private void endTurn(int player) {
		playerModels[player].turn = new ClientTurn(this, player);
		if(player == localPlayer) {
			handPane.removeAll();
		}
		playPane.removeAll();
		playPane.repaint();
		
		player = (player+1)%playerModels.length;
		if(player != localPlayer)
		{
			message.setText("It is " + names[player] + "'s turn.");
			state =  GameState.none;
			enableButtons(-1, null);
		}
	}

	
	public class PlayerModel {
		final int playerNumber;
		ClientTurn turn;
		//TODO: potentially some indication of how many cards are in
		//deck/discard pile? (but not exact counts, since you 
		//aren't allowed to count them)
		
		public PlayerModel(int playerNumber) {
			this.playerNumber = playerNumber;
			turn = new ClientTurn(Dominion.this, playerNumber);
		}
		//TODO how is ClientTurn going to be able to trash cards?
	}

	public void clearHand(int playerNum) {
		playerModels[playerNum].turn.inHand.clear();
		handPane.removeAll();
		handFrame.pack();
	}

	private void removeCardFromHand(int playerNum, Card c) {
		int i = playerModels[playerNum].turn.inHand.indexOf(c);
		handPane.remove(i);
		playerModels[playerNum].turn.inHand.remove(i);
		handFrame.pack();
	}

	private void removeCardFromPlay(int playerNum, Card c) {
		int i = playerModels[playerNum].turn.inPlay.indexOf(c);
		playPane.remove(i);
		playerModels[playerNum].turn.inPlay.remove(i);
		playFrame.pack();
	}

	@Override
	public void discardCard(int playerNum, Card c) {
		removeCardFromHand(playerNum, c);
	}

	@Override
	public void trashCardFromHand(int playerNum, Card c) {
		removeCardFromHand(playerNum, c);
		// TODO add to trash pile!
	}
	@Override
	public void trashCardFromPlay(int playerNum, Card c) {
		removeCardFromPlay(playerNum, c);
		// TODO add to trash pile!
	}
	@Override
	public void undrawCard(int playerNum, Card c) {
		removeCardFromHand(playerNum, c);
		//TODO add 1 to # in deck
	}
	
	@Override
	public void addCardToHand(int playerNum, Card c) {
		playerModels[playerNum].turn.inHand.add(c);
		handPane.add(new HandCardButton(c, this));
		resetFrameLocations();
	}
	// Assumed to be trashing from hand
	@Override
	public void trashCardSelection(int playerNum, CardListDecision cld) {
		for(Card c : cld.list) {
			System.out.println("Player " + playerNum + " trashed " + c);
			if(playerNum == localPlayer) {
				trashCardFromHand(localPlayer, c);
			}
			//TODO: add to trash
		}

	}

	static public String getImageNameForCard(Card c) {
		return c.toString().toLowerCase() + ".jpg";
	}
	
	static public Image getImageForCard(Card c) {
		return Toolkit.getDefaultToolkit().getImage(IMAGE_PATH + getImageNameForCard(c))
		              .getScaledInstance(100, -1, 0);
	}

	static public JButton newButton(String s, String act, char h, boolean enabled, ActionListener l) {
		JButton b = new JButton(s);
		b.addActionListener(l);
		if(act != null) b.setActionCommand(act);
		if(h != '\0') b.setMnemonic(h);
		b.setEnabled(enabled);
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);
		return b;
	}
	
	static public Component newPlayDisplay(Card c) {
		JLabel b = new JLabel(c.toString(), new ImageIcon(getImageForCard(c)), SwingConstants.CENTER);
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);
		return b;
	}

	static public String prettyListToString(List<?> l) {
		String result = "[";
		for(int i = 0; i < l.size(); i+=3) {
			for(int j = i; j < l.size() && j < i + 3 ; j++)
				result += l.get(j) + ", ";
			result += "\n";
		}
		return result + "]";
	}

	static public JMenuItem newItem(String s, char h, ActionListener l, Icon icon)
	{
		JMenuItem i = new JMenuItem(s);
		i.addActionListener(l);
		if(h != '\0') i.setMnemonic(h);
		if(icon != null) i.setIcon(icon);
		return i;
	}

	static public JMenu newMenu(String s, char h, JMenuItem... items)
	{
		JMenu m = new JMenu(s);
		if(h != '\0') m.setMnemonic(h);
		for(JMenuItem item : items)
		{
			if(item == null)
				m.addSeparator();
			else
				m.add(item);
	    }
	    return m;
	}

	// Client runs this to handle messages, and set up the player and gui
	private class ClientHandler extends Thread {
		private String playerName;
		private String host;
		private int port;
		private Socket toServer;

		ClientHandler(String playerName, String host, int port) {
			this.playerName = playerName;
			this.host = host;
			this.port = port;
		}

		public void getHostAndPort() {
			String result = JOptionPane.showInputDialog(Dominion.this, "Connect to host:port", HOME_PORT + ":" + DEFAULT_PORT);
			while(true) {
				if (result==null) continue;
				String[] parts = result.split(":");
				host = parts[0].trim();
				port = Integer.parseInt(parts[1].trim());
				try {
					toServer = new Socket(host, port);
					break;
				} catch (UnknownHostException e) {
					System.out.println(e.getLocalizedMessage());
					result = JOptionPane.showInputDialog("Unknown Host! try agin! host:port", HOME_PORT + ":" + DEFAULT_PORT);
				} catch (IOException ex) {
					System.out.println(ex.getLocalizedMessage());
					result = JOptionPane.showInputDialog("Invalid port! try agin! host:port", HOME_PORT + ":" + DEFAULT_PORT);
				}
			}			
		}

		// Connect to the server, loop getting messages
		@SuppressWarnings("hiding")
		@Override
		public void run() {
			while(true) {
				try {
					if(host==null)
						getHostAndPort();
					else
						toServer = new Socket(host, port);


					ObjectOutputStream out = new ObjectOutputStream(toServer.getOutputStream());
					out.writeObject(playerName);
					ObjectInputStream in = new ObjectInputStream(toServer.getInputStream());
//						System.out.println("client: connected!");

					int numPlayers = in.readInt();
					String[] names = new String[numPlayers];
					int southPlayer = 0;
					for(int i = 0; i<numPlayers; i++) {//get other players' names
						// get object from server; blocks until object arrives.
						names[i] = (String)in.readObject();
						if(names[i].equals(playerName)) {
							southPlayer = i;
						}
					}
					Streams streams = new Streams(playerName, in, out);
					Dominion.this.setupCanvas(names, streams, southPlayer);
					streams.start();
					break; //so we can start reading RemoteMessages
				}
				catch (IOException ex) { 
					System.out.println(ex.getLocalizedMessage());
					String result = JOptionPane.showInputDialog("Invalid port! try agin! host:port", HOME_PORT + ":" + DEFAULT_PORT);
					
					if (result!=null) {
						String[] parts = result.split(":");
						host = parts[0].trim();
						port = Integer.parseInt(parts[1].trim());
					}
//						ex.printStackTrace();
				}
				catch (Exception ex) { //ClassNotFoundException, ClassCastException
					ex.printStackTrace();
				}
			}
			// Could null out client ptr.
			// Note that exception breaks out of the while loop,
			// thus ending the thread.
		}
	}

	// Server thread accepts incoming client connections
	class ServerAccepter extends Thread {
		private boolean keepGoing = true;
		private int port;
		private JComboBox[] boxes;
		private final JDialog d = new JDialog(Dominion.this,  "Choose a player for each spot (this will update as people join).");
		private int numPlayers;

		private Map<String, Streams> nameToStreams;
		
		ServerAccepter(int numPlayers) {
			String result = JOptionPane.showInputDialog(Dominion.this, "Run server on port", DEFAULT_PORT);
			this.port = Integer.parseInt(result.trim());
			nameToStreams = new HashMap<String, Streams>();
			this.numPlayers = numPlayers;
		}

		private void setupDialog() {
			d.setLayout(new BorderLayout());

			boxes = new JComboBox[numPlayers];
			for(int i = 0; i < numPlayers; i++) {
				boxes[i] = new JComboBox();
				boxes[i].addItem("none");
//					boxes[i].addItem("AI");
			}

			d.add(BorderLayout.SOUTH, boxes[0]);
			d.add(BorderLayout.WEST, boxes[1]);
			if(numPlayers > 2)
				d.add(BorderLayout.NORTH, boxes[2]);
			if(numPlayers > 3)
				d.add(BorderLayout.EAST, boxes[3]);

			JButton launch = new JButton("Launch");
			launch.addActionListener(new ActionListener() {

				@SuppressWarnings("hiding")
				@Override
				public void actionPerformed(ActionEvent arg0) {
					//TODO maybe check all valid first?
					keepGoing = false;
					d.setVisible(false);
					
					Streams[] players = new Streams[numPlayers];
					String[] names = new String[numPlayers];
					
					int boxNum = 0;
					for(int i = 0; i < numPlayers; i++, boxNum++) {
						while((names[i] = (String) boxes[boxNum].getSelectedItem()).equals("none"))
							boxNum++;
						System.out.println(names[i]);
						//TODO: do something in following case:
//							if(boxNum >= numPlayers)
						
						//TODO: figure out how to deal with AI players 
						//once I introduce them
						//spawn a new thread for each, have them listening for messages
						
						players[i] = nameToStreams.get(names[i]);
					}
					//send out the number of players and their names
					for(int i = 0; i < numPlayers; i++) {
						try {
							players[i].out.writeInt(numPlayers);
							for(int j = 0; j < numPlayers; j++) {
		//						System.out.println("sending name " + names[j]);
									players[i].out.writeObject(names[j]);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}

					//while(true) {
						new Thread(new Game(players)).start();
						//TODO: prompt play again?
						//TODO: allow to go back and change players?
						//i.e. conditionally call setupPlayerConnections();
					//}
				}
			});
			d.add(BorderLayout.CENTER, launch);
			d.pack();
			d.setVisible(true);	
		}

		@Override
		public void run() {
			setupPlayerConnections();
		}
		
		void setupPlayerConnections(){
			try {
				setupDialog();

				ServerSocket serverSocket = new ServerSocket(port);
				while (keepGoing) {
					Socket toClient = null;
					// this blocks, waiting for a Socket to the client
					toClient = serverSocket.accept();
					System.out.println("server: got client");

					// Get an output stream to the client, and add it to
					// the list of outputs
					
					ObjectInputStream in = new ObjectInputStream(toClient.getInputStream());
					String name = (String)in.readObject();
					for(JComboBox box : boxes)
						box.addItem(name);
					ObjectOutputStream out = new ObjectOutputStream(toClient.getOutputStream());
					
					nameToStreams.put(name, new Streams(name, in, out));
					d.pack();
				}  
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setupCardSelection(int upperLimit, boolean exact, SelectionType type, SelectionCard c) {
		String amount = (upperLimit == -1) ? "any number of cards":
			(((exact) ? "exactly " : "at most ") + upperLimit + " card(s)");
		message.setText("Choose " + amount + " to " + type + " from your hand.");

//		TODO enforce how many more you MUST and/or CAN select with GUI
		//right now, this is enforced only on the server side, and a new selection
		//will be requested if an invalid one is sent.
		state = GameState.waitingForSelectFromHand;
		
		enableButtons(-1, c);
	}
	
	@Override
	public void setupGainCard(int upperLimit, boolean exact, SelectionCard c, String s) {
		if(s == null) {
			String amount = (upperLimit == -1) ? "any number of cards":
				(((exact) ? "exactly " : "at most ") + upperLimit);
			message.setText("Choose a card costing " + amount + " to gain.");
		} else {
			message.setText(s);
		}
		//TODO add take into hand as a bool?

//		TODO enforce exact w/ GUI
		state = GameState.waitingForGain;
		
		enableButtons(upperLimit, c);
		
	}

	private String getImageLinkForCard(Card c) {
	  	return c.toString();
//	  	return "<html><img src=\"" +
//			"file:" + IMAGE_PATH + getImageNameForCard(c) + 
//			"\"></img></html>" + c;
	}
	
	@Override
	public void recieveMessage(RemoteMessage m) {
		System.out.println("Got message: " + m);
		switch(m.action) {
		case stack:
			stacks = ((StackDecision)m.decisionObject).stacks;
			setupSupplyGUI();
			break;
		case cardsWereShuffled:
			messageText += names[m.playerNum] + " shuffled his/her cards.\n";
			messagePane.setText(messageText);
			//TODO visual display
			break;
		case revealFromDeck:
			messageText += names[m.playerNum] + " revealed from deck: " + getImageLinkForCard(m.card) + ".\n";
			messagePane.setText(messageText);
			//TODO visual display
			break;
		case revealFromHand:
			messageText += names[m.playerNum] + " revealed from hand: " + m.decisionObject + ".\n";
			messagePane.setText(messageText);
			//TODO visual display
			break;
		case putOnDeck:
			messageText += names[m.playerNum] + " put card on deck: " + getImageLinkForCard(m.card) +  ".\n";
			messagePane.setText(messageText);
			//TODO visual display
			break;
		case discardCard:
			messageText += names[m.playerNum] + " discarded card: " + getImageLinkForCard(m.card) +  ".\n";
			messagePane.setText(messageText);
			//TODO visual display
			break;
		case discardCardList:
			messageText += names[m.playerNum] + " discarded cards: " + ((CardListDecision) m.decisionObject).list + ".\n";
			messagePane.setText(messageText);
			if(m.playerNum == localPlayer)
				for(Card c : ((CardListDecision) m.decisionObject).list)
					discardCard(m.playerNum, c);
			//TODO visual display
			break;
		case putOnDeckFromHand:
			messageText += names[m.playerNum] + " put card on deck from hand: " + getImageLinkForCard(m.card) +  ".\n";
			messagePane.setText(messageText);
			if(m.playerNum == localPlayer) undrawCard(m.playerNum, m.card);
			break;
		case putInHand:
			messageText += names[m.playerNum] + " put card into hand: " + getImageLinkForCard(m.card) +  ".\n";
			messagePane.setText(messageText);
			if(m.playerNum == localPlayer) addCardToHand(m.playerNum, m.card);
			break;
		case gainCard:
			messageText += names[m.playerNum] + " gained: " + getImageLinkForCard(m.card) + "\n";
			messagePane.setText(messageText);
			removeFromSupply(m.card);
			//TODO add to discard
			break;
		case playCard:
			messageText += names[m.playerNum] + " played: " + getImageLinkForCard(m.card) + "\n";
			messagePane.setText(messageText);
			if(m.playerNum == localPlayer) {
				int i = playerModels[m.playerNum].turn.inHand.indexOf(m.card);
				handPane.remove(i);
				handPane.repaint();
				playerModels[m.playerNum].turn.playCard(m.card);
			}
			playPane.add(newPlayDisplay(m.card));
			resetFrameLocations();
			break;
		case addCardToHand:
			playerModels[m.playerNum].turn.inHand.add(m.card);
			if(m.playerNum == localPlayer){
				handPane.add(new HandCardButton(m.card, this));
				resetFrameLocations();
			} else {
				messageText += names[m.playerNum] + " drew a card.\n";
				messagePane.setText(messageText);
			}
			break;
		case chooseAction: 
			if(m.playerNum == localPlayer){
				message.setText("Choose an action from your hand (" + playerModels[localPlayer].turn.numActionsLeft + " actions left).");
				state = GameState.waitingForAction;
				enableButtons(0, null);
			}
			break;
		case chooseBuy: 
			if(m.playerNum == localPlayer) {				
				message.setText("May buy " + ((GainDecision)m.decisionObject).numGains + " item(s) costing a total of " + ((GainDecision)m.decisionObject).upperLimit + "."); 
				state = GameState.waitingForBuy;
				fromSupply.clear();
				playerModels[localPlayer].turn.buyingPower = ((GainDecision)m.decisionObject).upperLimit;
				playerModels[localPlayer].turn.numBuysLeft = ((GainDecision)m.decisionObject).numGains;
				enableButtons(((GainDecision)m.decisionObject).upperLimit, null);
			}
			break;
		case endTurn: 
			endTurn(m.playerNum); 
			if(m.playerNum == localPlayer)
				//clear so that you only ever see what happened since your last turn
				messageText = "";
			else
				messageText += names[m.playerNum] + " finished his/her turn.\n";
			messagePane.setText(messageText);
			break;
		case endScore: 
			scores = ((ScoresObject)m.decisionObject).scores;
			winner = ((ScoresObject)m.decisionObject).winnerIndex;
			String mess = "";
			for(int i = 0; i < scores.length; i++)
				mess += (names[i] + " had " + scores[i] + " points.") + "\n";
			mess += (names[winner] + " wins!");
			System.out.println(mess);
			
			JOptionPane.showMessageDialog(this, mess);
			//TODO ties?
			break;
		case makeDecision:
			if(m.playerNum == localPlayer){
				((DecisionCard)m.card).createAndSendDecisionObject(this, m.decisionObject);
			}
			break;
		case sendDecision: //On this side, this means that decision was accepted
			if(m.playerNum == localPlayer){
				((DecisionCard)m.card).carryOutDecision(this, localPlayer, m.decisionObject, playerModels[localPlayer].turn);
			}
			break;
		case buyCards:
			System.out.println("The action " + m.action + " should never be sent to the client!  Something is wrong.");
			break;
		default: 
			System.out.println("Missing a case:" + m.action + "!");
		}
	}

	@Override
	public <E extends Enum<E>> void makeMultipleChoiceDecision(String text, Class<E> enumType, Card c) {
		E[] options = enumType.getEnumConstants();
		Icon icon = null;
		Object displayMessage = text;
		if(c != null) {
			Image image = Toolkit.getDefaultToolkit().getImage(IMAGE_PATH + getImageNameForCard(c));
			icon = new ImageIcon(image);
			displayMessage = new Object[]{text, icon};
		}
		int result = JOptionPane.showOptionDialog(this, displayMessage, "Make a decision",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

		EnumDecision<E> ed = new EnumDecision<E>(options[result]);
		streams.sendMessage(new RemoteMessage(Action.sendDecision, localPlayer, null, ed));
	}

	@Override
	public String getPlayerName(int playerNum) {
		return names[playerNum];
	}

	@Override
	public int getLocalPlayer() {
		return localPlayer;
	}


}