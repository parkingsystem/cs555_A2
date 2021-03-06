package A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.Random;

import A2.message.Command;
import A2.message.NodeDetails;
import A2.message.Response;
import A2.message.RegistgerCommand;
import A2.message.ReturnRandomNodeCommand;

public class DiscoveryNode implements Node {

   private String discoveryIP;
   private int discoveryPORT;

   private TCPSender sender = new TCPSender();
   private Random random = new Random();
   
   private ReceiverWorker receiverWorker;
   private Thread receiverWorkerThread;
   private String nodeName;
   private Hashtable<Integer, NodeDetails> registry = new Hashtable<>();

   public static void main(String[] args) throws Exception {
      int discoverPORT = 0;
      String discoverIP = "";

      if (args.length < 1) {
         System.out.println(
               "Exa: java A2.DiscoveryNode <DISCOVERY NODE PORT>");
         System.exit(0);
      }

      try {
         discoverPORT = Integer.parseInt(args[0]);
      } catch (Exception e) {
         System.out.println("Error: Please provide numneric argument.");
         System.exit(0);
      }

      DiscoveryNode node = new DiscoveryNode();
      node.discoveryIP = discoverIP;
      node.discoveryPORT = discoverPORT;
      node.intializeServerNode();

      boolean continueOperations = true;

      while (continueOperations) {
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         String exitStr = br.readLine();
         System.out.println("Received command is:" + exitStr);

         if ("exit".equalsIgnoreCase(exitStr)) {
            System.out.println("Exiting.");
            node.receiverWorkerThread.stop();
            continueOperations = false;
         } 
      }
      System.out.println("Bye.");
   }
   
   public void intializeServerNode() throws IOException {
      System.out.println("Initializing Node ...");

      ServerSocket serversocket = new ServerSocket(this.discoveryPORT);

      InetAddress ip = InetAddress.getLocalHost();
      this.discoveryIP = (ip.getHostAddress()).trim();
      this.nodeName = this.discoveryIP + ":" + this.discoveryPORT;

      receiverWorker = new ReceiverWorker(serversocket, this);
      receiverWorkerThread = new Thread(receiverWorker);
      receiverWorkerThread.start();

      System.out.println(this);
      System.out.println("node started ...");

   }

   @Override
   public String toString() {
      return "Doscovery Node [discoveryIP=" + discoveryIP + ", discoveryPORT=" + discoveryPORT + "]";
   }

   @Override
   public Command notify(Command command) throws Exception {
      System.out.println("Received command >> " + command);
      if(command instanceof RegistgerCommand) {
         // 1. Register request (Check for id collision)
         return registerNode((RegistgerCommand) command);
      } else if(command instanceof ReturnRandomNodeCommand) {
         // 2. Give me random node to resolve the successor
         return randomStaringNode((ReturnRandomNodeCommand) command);
      }
      return null;
   }

   private Command randomStaringNode(ReturnRandomNodeCommand command) throws Exception {
      
      NodeDetails response = null;
      int size = registry.size();
      if(size == 0) {
         response = new NodeDetails("", -1, -1, false, "No registerd nodes.");
      } else {
         Object[] valuesAsArray = registry.values().toArray();

         if(size == 1) {
            response = (NodeDetails) valuesAsArray[0];
         } else {
            for(int i=0; i<100;i++) {
               int nextInt = random.nextInt(size);
               System.out.println(nextInt);
               response = (NodeDetails) valuesAsArray[nextInt];
               if(response.id == command.id) {
                  continue;
               } else {
                  break;
               }
            }
         }
      }
      
      System.out.println("Random Node:" + response);
      return response;
   }

   private Command registerNode(RegistgerCommand command) throws Exception {
      Response response = null;
      if(command==null) {
         response = new Response(false, "Invalid command");  
      } else if(registry.containsKey(command.id)) {
         response = new Response(false, "ID already registered");
      } else {
         registry.put(command.id, new NodeDetails(command.ipAddress, command.port, command.id, true, ""));
         response = new Response(true, "Registered.");
      }
      System.out.println("Response is:" + response);
      return response;
   }

}