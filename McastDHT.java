/*--------------------------------------------------------

 1. Michael Nguyen  3/4/15:

 2. Java version 1.8.0_25


 3. Precise command-line compilation examples / instructions:

  In cmd shell open directory where McastDHT.java is located
 > javac *.java


 4. Precise examples / instructions to run this program:

 In separate cmd shell windows start the McastDHT class by
 >java McastDHT

 In each shell the McastDHT will be part of a node of a
 distributed hash table. THe first node started will always
 have port 40001, and id 1. All nodes after will be given
 a random id with port 40000+id.


 5. List of files needed for running the program.

 a). McastDHT.java

 6. Notes: When manually entering command, follow the format
    Dr.Elliot has in his instruction page, or follow the 
    command assistant option in this program.

 ----------------------------------------------------------*/

//Import classes necessary for the program
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.concurrent.TimeUnit;





public class McastDHT {
	
	public static void main(String args[]) {
		System.out.println("\nWelcome to Mike's DHT \n");    //Intro
		MenuBuilder menu = new MenuBuilder();                //Class build a menu to assit user 
		
		//Thread for the menu
		Thread t = null; 
		t = new Thread(menu);
		t.start();
		
		//Wait for menu thread to finish before closing
		try {
			t.join();
		} catch (InterruptedException e) {e.printStackTrace();}	
		
	}
	
}





//Status class, responsible for send and receiving UDP package and setting up Node id and port
//Update variable based on the instruction inside the package
class Status extends Thread{
	
	
	//Variable needed for sending and receive DatagramPacket
	private static DatagramSocket dhtSocket;             //UDP connection, use to send/receive package
	private static DatagramPacket dhtPackage;            //Convert the message into DatagramPacket, so it can be send over DatagramSocket
	private static byte[] rsize;                         //Use in DatagramPacket, initialize to arbitrary value
	private static byte[] ssize;                         //Store String being send in bytes
	private static InetAddress dhtAddress;               //Store "localhost" converted to Inet Address
	private static final  int CPORT = 40000;             //Constant value the Node id will be added to
	private static final int rootPort = 40001;           //Constant value for the root node
	private static int port;                             //Port value of the  current Node
	
	
	
	
	private static int predecessor,successor;            //Hold the predecessor and successor port number of the Node
	private static Group[] mGroup;                       //Hold Mcast group, data type is a inner static class found in this method
	private static int c;                                //Loop condition, will try 998 times to find an id for a Node 
	private static String text;                          //Hold the received package
	private static int id;                               //Id of the Node
	
	Status(){
		
		id = 1;                                          //Each Node is initialize to id 1, will change if root Node exists
		
		//Initialize variables
		c = 1;                                           
		predecessor = 0;                      
		successor = 0;
		rsize = new byte[512];
		mGroup = new Group[1000];
		
	}
	
	
	
	
	//Inner class, use as data type to house the Mcast group info
	static final class Group{
	    int gID;          
	    boolean root,forward;

	    Group(int g, boolean f, boolean r) {
	    	gID = g;                  //Initialize group id
	    	forward =f;               //Initialize forwarder status for gID group
	    	root = r;                 //Initialize root, determine if this Node is the root Node for the gID group
	    }
    }
	
	public void run(){
	
		try{
			
			dhtAddress = InetAddress.getByName("localhost");           //Initialize to use "localhost" as address for DatagramPacket
			setPort();                                                 //Method assigned id to Node, new node contacts the root node to join DHT
			while(true){
				try {
				//Wait for incoming package from other node to process command
					
				//Also wait for package from this node and send that to sendCommand() method
				//I had trouble calling sendCommand() directly without distorting the code,
				//command() method will be call instead and send package to itself, which
			    //will be send to sendCommand()
				dhtPackage = new DatagramPacket(rsize, rsize.length);
				dhtSocket.receive(dhtPackage);
				text = new String(dhtPackage.getData(), 0,
						dhtPackage.getLength());
				if(text.contains("SendCommand"))
					sendcommand(text);
				else	
					receiveMessage(text, dhtPackage);
				
				} catch (SocketTimeoutException sto){continue;}
			}
			
		}catch(IOException io){}
		
		
	}
	
	//Method process incoming Package and output is based on command in package
	void receiveMessage(String msg, DatagramPacket sport){
		
	    String old = msg;                    //Store unmodified version of the String inside the package
	    String loopMsg,groupMsg;             //Variable hold the loop message and group message
	                                         //Each message are modified differently
	    
		int p = sport.getPort();             //Store the port of the sender
		groupMsg = msg;                      //Initialize to unmodified message
		
		
		String[] split = msg.split(" ");     //Split the message by spaces and put each word/number into a String array
		
		loopMsg = msg.replace(split[0],"");  //Remove first word in the String
		loopMsg = loopMsg.trim();            //Remove leading and trailing spaces
		msg = msg.toUpperCase();             //Upper case the whole string
		split = msg.split(" ");              //Split the upper case message by spaces and put each word/number into a String array
		String temp;                         //Hold String so it can be split, use in case "ADD"
		
		
		
		

		switch(split[0]){
		    case "ADD":
		    	//Process add request to DHT from new node
		    	
		    	//Root node put the sender port number in the message
		    	if(port == 40001){
		    		old = (old + " " + p);
		    		split = old.split(" ");
		    	}
		    	
		    	//If id collision, request sender to get new id
		    	if (successor == (Integer.parseInt(split[1])+40000)){
		    		try {
		    		ssize = ("No, node collision").getBytes();
		    		dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, Integer.parseInt(split[2]));
		    		dhtSocket.send(dhtPackage);
		    		}  catch (Exception e) {System.out.println("Unable to add Node.\n");}
		    		break;
		    	}
		    	
		    	//if the successor port is less than the sender port
		    	if(successor < (Integer.parseInt(split[1])+40000)){
		    		//Trailing node successor in the DHT will point to themselves
		    		//So the request node will add itself after the trailing node
		    		//and change the successor to the request node
		    		if(successor == port){
		    			p = (Integer.parseInt(split[1])+40000);
		    			temp = ("OK " + old + " " + successor + " " + p);
		    			split = temp.split(" ");
						ssize = temp.getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, Integer.parseInt(split[3]));
						successor =p;
					//Request Node move on to the next node in DHT
		    		}else{
						ssize = old.getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, successor);
						}
		    	//Request node found a larger id than itself	
		    	}else{
		    		//Request node position itself before the larger node and change it's predecessor to the request node
		    		//Request node is currently in the larger node
		    		if(id > Integer.parseInt(split[1])){
		    			p = port;
		    			temp = ("OK " + old + " " + predecessor + " " + p);
		    			split = temp.split(" ");
						ssize = temp.getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, Integer.parseInt(split[3]));
						
						predecessor = (Integer.parseInt(split[2])+40000);
					//Request node change the successor of this node to point to the request node
					//Request node is currently in the node before the next node which is larger than the request node
		    		}else{
		    			p = (Integer.parseInt(split[1])+40000);
						ssize = old.getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, successor);
						successor =p;
						}
		    		
		    		
		    		
		    	}
		    	//Send the package either to the successor of this node, or back to the request node
				try {
					dhtSocket.send(dhtPackage);
				}  catch (Exception e) {System.out.println("Unable to add Node.\n");}
				
				break;
			case "PING":
				//Process ping reply
				
                //Send back a ping reply to the pinging node
				//Display ping message from pinging node
				try {
					ssize = ("Reply ping: Hi from Node: " + id).getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, p);
					dhtSocket.send(dhtPackage);
					System.out.println(old + "\nSending ping reply.\n");
					
				}  catch (Exception e) {System.out.println("Unable to send back reply ping.\n");}
				
				break;
			case "REPLY":
				//Reply send by node bing ping, display to console
				System.out.println(old + "\n");
			    break;
			case "LOOPPING":
				//Process loopPing command
				
				//break if trailing node
				if(successor == port){
					System.out.println("LoopPing Message:\n" + loopMsg  + "\n");
					break;
				}
				
				//Get the loopPing message and send it to its successor
				//Display the message to console
				try {
					ssize = old.getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, successor);
					dhtSocket.send(dhtPackage);
					System.out.println("LoopPing Message:\n" + loopMsg  + "\n");
				}  catch (Exception e) {System.out.println("Unable to forward LoopPing.\n");}
				
				break;
				
			case "SURVEY":
				//Process survey command
				
				//Trailing node send back package to original request node
				//Attach its id at the end and "DONE" at the beginning before sending package
				if(successor == port){
					try {
						ssize =("Done "+ old + " " + id).getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, Integer.parseInt(split[1])+CPORT);
						dhtSocket.send(dhtPackage);
					} catch (IOException e) {System.out.println("Unable to send survey back to original node.\n");}
                    break;
				}
				
				//Each node in the DHT will attach it id to the message and send it to its
				//successor, The original request node does not add its own id
				try {
					if(Integer.parseInt(split[1]) != id)
						ssize = (old + " " + id).getBytes();
					else
						ssize = old.getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, successor);
					dhtSocket.send(dhtPackage);
				}  catch (IOException e) {System.out.println("Unable to collect survey\n");}
					
				break;
			
			case "DONE":
				//Display the survey request
				
				//The request node will display first, and all other node in order
				System.out.print("Current Node: ");
				for(int z = 2; z<split.length;z++){
					System.out.print(split[z] + " ");
				}
				System.out.print("\n");
				break;
			case "FORWARDER":
				//Process Forwarder
				
				//Add this node to the mGroup[] as a forwarder for the group
				if(mGroup[(Integer.parseInt(split[1]))]==null){
					mGroup[(Integer.parseInt(split[1]))]= new Group(0,true,false);
					System.out.println("Node: " +id+ " is now a forwarder for Mcast Group: "+Integer.parseInt(split[1])+ ".\n");
				}
				//break out of trailing node
				if(successor==port)
					break;
				//Send forwarder request to its successor 
				 try {
						ssize = old.getBytes();
						dhtPackage = new DatagramPacket(ssize, ssize.length,
								dhtAddress, successor);
						dhtSocket.send(dhtPackage);
					}  catch (IOException e) {System.out.println("Unable to add forwarder.");}
						
					break;
			case "MCAST":
				switch(split[1]){
				
				   case "CREATE":
					   //Process creation of new Mcast group
					   
					   //Found a node to serve as a root node for group
					   if(Integer.parseInt(split[2])<=id){
						   mGroup[Integer.parseInt(split[2])] = new Group(Integer.parseInt(split[2]),false,true);
						   System.out.println("Mcast Group: " + Integer.parseInt(split[2])+ " create at this node.\n");
						   break;
						//No node available to house group
					   }else if(successor == port){
						   System.out.println("Unable to create group");
						   break;
					   }
					   
					   //Send creation request to successor until the correct node is found
					   try {
							ssize = old.getBytes();
							dhtPackage = new DatagramPacket(ssize, ssize.length,
									dhtAddress, successor);
							dhtSocket.send(dhtPackage);
						}  catch (IOException e) {System.out.println("Unable to create group");}
							
						break;
				   
						
					case "ADD":
						//Process add request to Mcast group
						
						//Node found in dht and will be added to Mcast group
						//Node send forwarder command to its successor to change status to forwarder for this group
						if(Integer.parseInt(split[3])==id){
							 mGroup[Integer.parseInt(split[2])] = new Group(Integer.parseInt(split[2]),false,false);
							 System.out.println("Node: " +Integer.parseInt(split[3])+ " added to Mcast Group: "+Integer.parseInt(split[2])+ ".\n");
							 try {
									ssize = ("Forwarder " + split[2]).getBytes();
									dhtPackage = new DatagramPacket(ssize, ssize.length,
											dhtAddress, successor);
									dhtSocket.send(dhtPackage);
								}  catch (IOException e) {System.out.println("Unable to add to group\n");}
									
							 break;
						}
						//transverse through DHT to find the node to add to Mcast group
						   try {
								ssize = old.getBytes();
								dhtPackage = new DatagramPacket(ssize, ssize.length,
										dhtAddress, successor);
								dhtSocket.send(dhtPackage);
							}  catch (IOException e) {System.out.println("Unable to add to group\n");}
								
							break;
						
					case "SEND":
						//Process message send to Mcast group
						
						//Get rid of leading tags
						split = groupMsg.split(" ");
						groupMsg = groupMsg.replace(split[0]+ " "+ split[1] +" "+ split[2],"");
						groupMsg = groupMsg.trim();
						
						//Display the message if the node is in the group
						if(mGroup[Integer.parseInt(split[2])]!=null && mGroup[Integer.parseInt(split[2])].gID != 0){
							   System.out.println("Group message: \n" + groupMsg +"\n");
							   
						}
						//break out of trailing node
						if(successor==port)
							break;
						//transverse through DHT to find the node in the group
						   try {
								ssize = old.getBytes();
								dhtPackage = new DatagramPacket(ssize, ssize.length,
										dhtAddress, successor);
								dhtSocket.send(dhtPackage);
							}  catch (IOException e) {System.out.println("Unable to send message\n");}
								
							break;
					case "REMOVE":
						//Process node removal form Mcast group
						
						//Change the node to forwarder
						if(Integer.parseInt(split[3])==id){
							mGroup[Integer.parseInt(split[2])].gID = 0;
							mGroup[Integer.parseInt(split[2])].forward = true;
							mGroup[Integer.parseInt(split[2])].root = false;
							System.out.println("Node: " +Integer.parseInt(split[3])+ " is now a forwarder for Mcast Group: "+Integer.parseInt(split[2])+ ".\n");
							break;
						}
						
						//Break out of trailing node
						if(successor==port)
							break;
						
						//transverse through DHT to find the node in the group	  
						   try {
								ssize = old.getBytes();
								dhtPackage = new DatagramPacket(ssize, ssize.length,
										dhtAddress, successor);
								dhtSocket.send(dhtPackage);
							}  catch (IOException e) {System.out.println("Unable to remove group\n");}
							break;
			}
			break;
		}
	}
	
	//Method initialize the node to an id and port, and request to be added to the DHT if the node is not a root node
	private static void setPort() {
	   String textFromServer;              //Reply from the dht
	   String[] split;	                   //hold the reply, split into an array
	   
	   //Initialize the root node, every node will attempt to do this
		try {
			dhtSocket = new DatagramSocket(id + CPORT,dhtAddress); //assign port 40001 and Inet address "localhost" to this DatagramSocket
			dhtSocket.setSoTimeout(180);                           //Initialize time out to prevent stallling
			port = id + CPORT;                                     //Initialize port to 40001
			predecessor = port;                                    //Initialize predecessor port to itself
			successor = port;                                       //Initialize successor port to itself
			System.out.println("Root node: "+ id +" starting up at port " + port +"\n");               //Display id and port to console
		} catch (SocketException se) {System.out.println("Unable to initialize as Root Node.\n");id = getNewID();}      //Root node already exist, assign new id
		
		
		//Attempt to assign a new id and port to node create after the root node
		if (id != 1) {
			
			do {
				try {
					//Initialize node to a generic port
					dhtSocket = new DatagramSocket();
					dhtSocket.setSoTimeout(180);         //Initialize time out to prevent stalling
					ssize = ("Add " + id).getBytes();    //Message send to the root node, contain the request node id
                    
					//Send the request to the root node
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, rootPort);
					dhtSocket.send(dhtPackage);
					
					//Receive reply back, include successor and predecessor in reply
					dhtPackage = new DatagramPacket(rsize, rsize.length);
					dhtSocket.receive(dhtPackage);
					c++;
                     
					//Convert package into a string
					textFromServer = new String(dhtPackage.getData(), 0,
							dhtPackage.getLength());
					split = textFromServer.split(" ");
					
					//If node was added to the DHT
					if (split[0].equals("OK")) {
						dhtSocket.close();          //close generic DatagramSocket
						dhtSocket = new DatagramSocket(id + CPORT,dhtAddress);     //Open new DatagramSocket at the approved port, with "localhost" Inet address
						port = id + CPORT;                                         //Initialize the approved port
						predecessor = Integer.parseInt(split[4]);                  //Initialize the predecessor
						successor = Integer.parseInt(split[5]);                    //Initialize the successor
						System.out.println("Node: " + id +" starting up at port " + port +"\n");   //Display id and port to console
						dhtSocket.setSoTimeout(180);                               //Initialize time out to prevent stalling
						break;
					//Node was not accepted into the DHT, tries a different id
					} else{
						
						dhtSocket.close();                                         //Close the generic DatagramSocket
						System.out.println("NodeID collisions, getting new ID. \n");     //Display to console
						id = getNewID();                                           //Get new random id
					}
				} catch (SocketTimeoutException sto) {                             //Unable to received package
					System.out
							.println("Unable to receive package, trying again. \n");
					         dhtSocket.close();
					         id = getNewID();
				} catch (Exception e) {
					System.out.println("Socket error.");                          //Error occur
					dhtSocket.close();
			         id = getNewID();
					
				}
			} while (c < 1000);                                                    //Will attempt to 998 times to find a new id,
			                                                                       //this number should be higher to ensure all possible value are produce.
			                                                                       //For this program I limited the number assuming no one wants to produce
			                                                                       //999 different nodes
		}
		if (c >= 1000)                                                             //Exit program with an error of 1, if no nodes can be produce
			System.exit(1);
		

	}
	


	
	//Generate a random int value between 2 -999
	private static int getNewID(){
		Random r = new Random();
		return r.nextInt((999 - 2)+1) + 2;
	}
	
	//An intermediate method that send a package to itself so the class doesn't break out of its' run method() and badly affect the code
	public void command(String commmand){
		try {
			ssize = ("SendCommand " + commmand).getBytes();                      //Adds tag so package will be pass to sendCommand()
			dhtPackage = new DatagramPacket(ssize, ssize.length,
					dhtAddress, port);
			dhtSocket.send(dhtPackage);
		}  catch (Exception e) {System.out.println("Unable to process message");}
		
		
	}
	
	//Process command inputed from the user
	public void sendcommand(String command){
		int i = 0;                      //Store calculated value
		String loogMsg,groupMsg;        //Store loop and group message, each one is process differently
		
	 	command = command.replace("SendCommand ","");          //remove the tag the command() method attach
		groupMsg = command;                                    //Initialize unmodified command
		String[] split = command.split(" ");                   //Split command by spaces into split[]
		loogMsg = command.replace(split[0],"");                //LoopMsg remove the tag, and store only the loop message
		command = command.toUpperCase();                       //Upper case the command String
		split = command.split(" ");                            //Store upper case command in split[]
		
		switch(split[0]){
			case "STATUS":
				//Process status request
				
				//Display the status of this node
				StringBuilder builder = new StringBuilder();
				builder.append("NodeID: " + id +"\n");
				i = predecessor - CPORT;
				builder.append("Predecessor NodeID: " + i + "\n");
				i = successor - CPORT;
			    builder.append("Successor NodeID: " + i + "\n\n");
			    
			    //Loop though array and look for groups this node belongs to
			    
			    //If I had time, I would had change the array to a map so it doesn't have
			    //to loop 999 times
			    for(int t = 1; t < mGroup.length;t++){
			    	if(mGroup[t]!=null){
			    		builder.append("Mcast ID: " + mGroup[t].gID + "\n");
			    		builder.append("Mcast Root: " + mGroup[t].root + "\n");
			    		builder.append("Forwarder: " + mGroup[t].forward + " for Mcast group " + t +"\n\n");
			    	}
			    }
			   
			    
				System.out.println(builder.toString());          //Display status to console
				break;
			case "PING":
				//Process ping request
				
				//Ping node input by user
				try {
					ssize = ("Ping by Node: " + id ).getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, Integer.parseInt(split[1]));
					dhtSocket.send(dhtPackage);
					System.out.println("Pinging port: " + split[1]);      //Display pinging to console
				}  catch (Exception e) {System.out.println("Unable to send/receive ping\n");}
			
				break;
			case "LOOPPING":
				//Process loopPing message request
				
				//Send message to root node
				try {
					ssize = (split[0] + " " + loogMsg).getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, rootPort);
					dhtSocket.send(dhtPackage);
					System.out.println("LoopPing message send.\n");        //Info user
				}  catch (Exception e) {System.out.println("Unable to send loopPing\n");}
				break;
			case "SURVEY":	
				//Process survey request
				
				//Attach node id to message and send to root node
				try {
					ssize = (split[0] + " " + id).getBytes();
					dhtPackage = new DatagramPacket(ssize, ssize.length,
							dhtAddress, rootPort);
					dhtSocket.send(dhtPackage);
					
				}  catch (Exception e) {System.out.println("Unable to send survey\n");}
				break;
			case "FILE":
				//Process file command read request
				
				String read;             //Store command from file
				BufferedReader buff;     //read file
                try {
                	//Initialize variables
					buff= new BufferedReader(new FileReader(split[1].trim()));
					read = buff.readLine();
					
					while(read!=null){       //Read until null
						
						read.trim();
						if(!read.equals(" ")){       //If blank line do not call command()
						
							command(read);
						}
						read = buff.readLine();      //Read and store command from file
						
					}
					buff.close();
	            } catch (Exception t) {System.out.println("Invalid file");}
				
				break;
			case "MCAST":
				switch(split[1]){
				
				   case "CREATE":
				       //Process Mcast group creation 
					   
					   //Send request to root node
						try {
							ssize = (split[0] + " " + split[1] + " "  + split[2]).getBytes();
							dhtPackage = new DatagramPacket(ssize, ssize.length,
									dhtAddress, rootPort);
							dhtSocket.send(dhtPackage);
							
						}  catch (Exception e) {System.out.println("Unable to create group\n");}
						
						break;
					
					case "SEND":
						//Process Mcast group message process
						
						//Remove tags, groupMsg only hold the message String, allows for message separated by spaces
						split=groupMsg.split(" ");
						groupMsg = groupMsg.replace(split[0]+ " "+ split[1] +" "+ split[2],"");
						groupMsg = groupMsg.trim();
						
						 //Send request to root node
						try {
							ssize = (split[0] + " " + split[1] + " " + split[2] + " " + groupMsg).getBytes();
							dhtPackage = new DatagramPacket(ssize, ssize.length,
									dhtAddress, rootPort);
							dhtSocket.send(dhtPackage);
							
						}  catch (Exception e) {
							e.printStackTrace();
							
						}
						break;
					case "ADD":
					case "REMOVE":	
						//Process add and remove nod request
						
						//Send request to root node
						try {
							ssize = (split[0] + " " + split[1] + " " + split[2] + " " + split[3]).getBytes();
							dhtPackage = new DatagramPacket(ssize, ssize.length,
									dhtAddress, rootPort);
							dhtSocket.send(dhtPackage);
							
						}  catch (Exception e) {
							e.printStackTrace();
							
						}
						break;
			}
			break;
		}
		
	}
	
}







//Build and display a list of command to assist user
class MenuBuilder implements Runnable{
	
	  static boolean stop;                //Loop condition for display menu
	  
	  //Inner class, use as data type for the menu
	  static class Menu{
		    final String option;
		    final Action action;

		    Menu(String o, Action r) {
		    	option = o;
		    	action = r;
		    }
	 }
	 
	 //Interface that will be defined with different run() methods
	 //Use in Menu class
	 interface Action{
		 
		 public void run(Status status, BufferedReader in);
	 }
	 
	  public void run(){
		  final Menu[] menu;                       //Holds the list of command
		  BufferedReader in;                       //Read user input
		  Status status = new Status();            //Start new Status and associate it with this class
		  int i = 0;                               //Holds user input parse to int
		  String command;                          //Holds user input
		  //Start a thread for status
		  Thread t = null;  
		   t = new Thread(status);
		   t.start();
		  
		  
		    //Sleep so t thread has time to process and display its intro
			try {
				TimeUnit.MILLISECONDS.sleep(30);
			} catch (InterruptedException e) {e.printStackTrace();}
			
			menu = new Menu[12];
			setup(menu);
			stop=true;
			in = new BufferedReader(new InputStreamReader(System.in));  
			
		  
		  
		  
		 
		
		 do{
			 try {
				 //Primary command, takes in a int value
				 
				 System.out.println("\n1. Manually input command \n" +
			 		            	"2. See list of command for assistant \n" +
			 		            	"3. Quit\n");
				 i = Integer.parseInt(in.readLine());
			     System.out.println();
				 if(i==1){
					 
					 //User manually input command, follow the strucutre provided by Dr.Elliott in the instruction page 
					 do{
						 System.out.print("Enter a command or quit: ");
						 command = in.readLine();
						 System.out.println("\n");
				
						 if(command.indexOf("quit") < 0){
							 status.command(command);
							 
							 //Sleep so t thread has time to process and display result
							 try {
									TimeUnit.MILLISECONDS.sleep(30);
								} catch (InterruptedException e) {e.printStackTrace();}
						 }
					 }while(command.indexOf("quit") < 0);
					 
					//A list of available command will display, user will input an int value
				 }else if(i == 2){
					 while(stop){
						 
						 //Display menu[]
						 System.out.println ("\nSelect an option \n");
						 for(int c = 1; c <menu.length;c++){
							 System.out.println(menu[c].option);
						 }
						 System.out.println("\n");
						 
						 
						 i = Integer.parseInt(in.readLine());
						 System.out.println("\n");
						 
						 if(i>0 && i<12){
							 
							 //run action related to user input
							 menu[i].action.run(status,in);
							 
							//Sleep so t thread has time to process and display result
							 try {
									TimeUnit.MILLISECONDS.sleep(30);
								} catch (InterruptedException e) {e.printStackTrace();}
						 }
						 System.out.println("\n");
						 
					 }
			 
				 }
				
			} catch (NumberFormatException | IOException e) {System.out.println("Invalid input. Type in a number ");} //Invalid input from user
			 stop=true;             //User break out of menu[] loop, reset to true
		 }while(i != 3);
		 System.out.println("\nNode shutting down.\n");          //Use exited out of node
		 
	 }
	 
	 //Set up the menu[] and initialize each index to perform a certain action
	 //Pass the request to command()
	 static void setup(Menu[] menu){
		
		 //Index o is set to invalid
		 menu[0] = new Menu(
					" 0. Invalid ",
					new Action() {
						public void run(Status status, BufferedReader in) {
							System.out.println("Invalid command.");
							}
	               });

		//Index 1 process status request, no user input
		menu[1] = new Menu(
				"1. Status ",
				new Action() {
					public void run(Status status, BufferedReader in) {
						status.command("status");
						}
               });
		//Index 2 display addition string, user needs to input a comPort to ping  
		menu[2] = new Menu(
				"2. Ping [ComPort] ",
				new Action() {
					public void run(Status status, BufferedReader in) {
						String comPort;
                        
                        System.out.print("Enter a ComPort to ping: ");
                        
                        try {
							comPort = ("Ping " + in.readLine());
							 status.command(comPort);
						} catch (IOException e) {System.out.println("Unable to process ping");}
                      
                        
					}
				});
		//Index 3 display addition string, user needs to input message to loopPing
		menu[3] = new Menu(
				"3. LoopPing [Msg]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						String msg;
						
	                    System.out.print("Enter a message to loopPing: ");
                        try {
							msg = ("LoopPing " + in.readLine());
							 status.command(msg);
						} catch (IOException e) {System.out.println("Unable to process loopPing");}
                       
					}
				});
		//Index 4 process survey request, no user input
		menu[4] = new Menu(
				"4. Survey",
				new Action() {
					public void run(Status status, BufferedReader in) {
						status.command("survey");
					}
				});
		//Index 5 display addition string, user needs to input a file name
		menu[5] = new Menu(
				"5. File [FileName]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						String read;
						
	                    System.out.print("Enter file name: ");
						try {
							read = ("File "+ in.readLine());
							status.command(read);
							try {
								TimeUnit.SECONDS.sleep(2);
							} catch (InterruptedException e) {e.printStackTrace();}
						} catch (IOException e) {System.out.println("Unable to read file name");}
						
					}
				});
		//Index 6 display addition string, user needs to input Mcast id to create the new group
		menu[6] = new Menu(
				"6. Mcast Create [McastID]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						
						String create;
						System.out.print("Enter McastID to create group: ");
						try {
							create = ("Mcast create "+ in.readLine());
							status.command(create);
						} catch (IOException e) {System.out.println("Unable to create group");}
						
                       
					}
				});
		//Index 7 display addition string, user needs to input Mcast id and node id 
		menu[7] = new Menu(
				"7. Mcast Add [McastID] [NodeID]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						
						String mID;
						System.out.print("Enter McastID and NodeID: ");
						try {
							mID = ("Mcast add "+ in.readLine());
							status.command(mID);
						} catch (IOException e) {System.out.println("Unable to add node");}
						
						
					}
				});
		//Index 8 display addition string, user needs to input Mcast id and message for the group
		menu[8] = new Menu(
				"8. Mcast Send [McastID] [Msg]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						
						String mID;
					    System.out.print("Enter McastID and Message: ");
						try {
							mID = ("Mcast send "+ in.readLine());
							status.command(mID);
						} catch (IOException e) {System.out.println("Unable to send message");}
						
						
					}
				});
		//Index 9 display addition string, user needs to input Mcast id and node id
		menu[9] = new Menu(
				"9. Mcast Remove [McastID] [NodeID] ",
				new Action() {
					public void run(Status status, BufferedReader in) {
						
						String mID;
						System.out.print("Enter McastID and NodeID: ");
						try {
							mID = ("Mcast remove "+ in.readLine());
							status.command(mID);
						} catch (IOException e) {System.out.println("Unable to remove node");}
					
                        
					}
				});
		//Index 10 is not implemented
		menu[10] = new Menu(
				"10. Mcast Destroy [McastID]",
				new Action() {
					public void run(Status status, BufferedReader in) {
						System.out.println("Not implemented.\n ");
						
						/*
						String mID;
						System.out.print("Enter McastID and NodeID: ");
						try {
							mID = ("Mcast destroy "+ in.readLine());
							status.command(mID);
						} catch (IOException e) {System.out.println("Unable to destroy group");}
						*/
                        
					}
				});
		//Index 11 exits out of the loop and return to primary menu
		menu[11] = new Menu(
				"11. Exit command options",
				new Action() {
					public void run(Status status, BufferedReader in) {
						stop=false;
					}
				});
	 }
}
