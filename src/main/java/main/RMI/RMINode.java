package main.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.*;
import java.util.Enumeration;

public class RMINode implements RMIInterface {
  private static String thisNodeIDString;
  private static String coordinator;
  private static ScheduledExecutorService executor;
  private static String registryHostname;
  private static Integer registryPort;
  static RMIInterface nodestub;
  private boolean isAlgorithmRunning;
  private Thread algorithmThread;
  Registry registry;

  public RMINode() {
    isAlgorithmRunning = false;
  }

  public static void main(String[] args) {
    RMINode node = new RMINode();
    node.startAlgorithm("10.0.2.6", 5696, "3");
  }

  private String getLocalIPAddress() {
    try {
      String ip = "127.0.0.1";

      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();

        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (networkInterface.getDisplayName().equals("LogMeIn Hamachi Virtual Ethernet Adapter")) {
            ip = address.getHostAddress();
            return ip;
          }
          if (!address.isLoopbackAddress() && !address.isLinkLocalAddress() && address.isSiteLocalAddress()) {
            ip = address.getHostAddress();
          }
        }
      }
      return ip;
    } catch (SocketException e) {
      e.printStackTrace();
    }
    return null;
  }

  public void startAlgorithm(String host, Integer port, String id) {
    String localIPAddress = getLocalIPAddress();
    if (localIPAddress != null) {
      System.setProperty("java.rmi.server.hostname", localIPAddress);
      System.out.println("Local RMI IP set to: " + localIPAddress);
    } else {
      System.err.println("Failed to determine the local IP address.");
    }

    System.out.println("Starting algorithm");
    isAlgorithmRunning = true;
    algorithmThread = new Thread(() -> runAlgorithm(host, port, id));
    algorithmThread.start();
    System.out.println("Started algorithm");
  }

  public void runAlgorithm(String host, Integer Port, String ID) {
    try {
      System.out.println("Algorithm running");
      registryHostname = host;
      registryPort = Port;
      thisNodeIDString = ID;

      registry = LocateRegistry.getRegistry(registryHostname, registryPort);
      RMIServerInterface serverStub = (RMIServerInterface) registry.lookup("0");

      nodestub = (RMIInterface) UnicastRemoteObject.exportObject(
          this,
          Integer.parseInt(thisNodeIDString) + registryPort
      );

      serverStub.registerNode(thisNodeIDString, nodestub);

      nodestub.electionMessage(thisNodeIDString);
      // Schedule the coordinator check task
      executor = Executors.newScheduledThreadPool(1);
      executor.scheduleAtFixedRate(this::checkCoordinatorStatus, 0, 10, TimeUnit.SECONDS);
      System.out.println("Algorithm running properly?");
      while (isAlgorithmRunning) {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      if (executor != null) {
        executor.shutdownNow();
      }
      if (nodestub != null) {
        serverStub.unregisterNode(thisNodeIDString, registry);
        System.out.println("Unbound and unregistered");
        UnicastRemoteObject.unexportObject(this, true);
        System.out.println("Unexported");
      }
      algorithmThread.interrupt();
    } catch (Exception e) {
      System.out.println("RMINode exception: " + e);
      e.printStackTrace();
    }
  }

  public void stopAlgorithm() {
    isAlgorithmRunning = false;
  }

  @Override
  public void electionMessage(String NodeIDString) throws RemoteException {
    boolean foundGreater = false;

    if (NodeIDString.equals(thisNodeIDString)) {
      System.out.println("You started the elections");

      for (String nodeID : registry.list()) {
        if (!nodeID.equals(thisNodeIDString) && Integer.parseInt(nodeID) > Integer.parseInt(thisNodeIDString)) {
          try {
            RMIInterface stub = (RMIInterface) registry.lookup(nodeID);
            System.out.println("There are nodes with higher IDs, sending election challenge to " + nodeID);
            stub.electionMessage(thisNodeIDString);
            foundGreater = true;

          } catch (Exception e) {
            System.out.println("Election exception: " + e);
            e.printStackTrace();
          }
        }
      }
      if (!foundGreater) {
        victoryMessage(thisNodeIDString);
      }
    } else {
      System.out.println("Received election request from " + NodeIDString);
      answerAlive(NodeIDString, thisNodeIDString);
    }
  }

  @Override
  public void victoryMessage(String node) throws RemoteException {
    coordinator = node;
    if (node.equals(thisNodeIDString)) {

      System.out.println("This node won the election, sending the message to other nodes.");
      for (String nodeID : registry.list()) {
        RMIInterface stub;
        if (!nodeID.equals(thisNodeIDString) && !nodeID.equals("0")) {
          try {
            stub = (RMIInterface) registry.lookup(nodeID);
            System.out.println("Informing node " + nodeID + " of this node's victory");
            stub.victoryMessage(node);

          } catch (Exception e) {
            System.out.println("Victory exception: " + e);
            e.printStackTrace();
          }
        }
      }
    }
    System.out.println("Node " + coordinator + " has won the election and is the new coordinator.");
  }

  @Override
  public void answerAlive(String destinationIDString, String nodeIdReplying) throws RemoteException {
    if (!thisNodeIDString.equals(destinationIDString)) {
      try {
        RMIInterface stub = (RMIInterface) registry.lookup(destinationIDString);
        System.out.println("Sending alive message to " + destinationIDString);
        stub.answerAlive(destinationIDString, thisNodeIDString);
        // start election after sending OK
        electionMessage(thisNodeIDString);
      } catch (Exception e) {
        System.out.println("Victory exception: " + e);
        e.printStackTrace();
      }
    } else {
      // receive OK
      System.out.println(nodeIdReplying + " is Alive");
    }
  }

  public boolean isAlive() throws RemoteException {
    return true;
  }

  private void checkCoordinatorStatus() {
    try {
      System.out.println("Coordinator check");
      RMIInterface coordinatorStub = (RMIInterface) registry.lookup(coordinator);
      coordinatorStub.isAlive();
    } catch (RemoteException e) {
      System.out.println("Exception, inactive coordinator ");
      coordinatorCrashed();
    } catch (Exception e) {
      System.out.println("Exception, inactive coordinator");
      coordinatorCrashed();

    }
  }

  private static void coordinatorCrashed() {
    System.out.println("Coordinator inactive, starting elections");
    try {
      nodestub.electionMessage(thisNodeIDString);
    } catch (RemoteException e) {
      e.printStackTrace();
    }

  }

}
