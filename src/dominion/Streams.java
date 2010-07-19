package dominion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Streams extends Thread {
	public String name;
	public ObjectInputStream in;
	public ObjectOutputStream out;
	List<StreamListener> inListeners = null;
	
	
	public Streams(String name, ObjectInputStream in, ObjectOutputStream out) {
		this.name = name;
		this.in = in;
		this.out = out;		
	}
	
	@Override
	public void run() {
		while(true) {
			RemoteMessage rm = null;
			try {
				rm = (RemoteMessage)in.readObject();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}//TODO: do something smarter on exceptions! try to re-connect, etc.
			if(inListeners != null) 
				for(StreamListener inListener : inListeners) {
					inListener.recieveMessage(rm);
				}
		}
	}

	public void addInListener(StreamListener sl) {
		if(inListeners == null) 
			inListeners = new ArrayList<StreamListener>();
		inListeners.add(sl);
	}
	
	public void sendMessage(RemoteMessage rm) {
		try {
			out.writeObject(rm);
		} catch (IOException e) {
			//TODO: do something to recover... try to get a new connection?
			e.printStackTrace();
		}
	}

}