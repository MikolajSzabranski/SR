package main.RMI;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class RMIServer implements RMIServerInterface {

  private static final long NODE_CHECK_INTERVAL = 4000; // Check node liveness every 4 seconds
  private List<String> registeredNodes; // List to store registered node IDs
  private static String registryHostname;
  private static Integer registryPort;
  private static Registry registry;
  private Thread serverThread;
  private boolean isServerRunning;
  private static RMIServerInterface stub;

  public RMIServer() {
    super();
    registeredNodes = new ArrayList<>();
  }

  public static void main(String[] args) {
    try {
      registryHostname = "10.0.2.6";
      registryPort = 5696;
      System.setProperty("java.rmi.server.hostname", registryHostname);
      RMIServer server = new RMIServer();
      stub = (RMIServerInterface) UnicastRemoteObject.exportObject(server, (registryPort - 1));

      // Register the stub with the RMI registry
      registry = LocateRegistry.createRegistry(registryPort);
      registry.bind("0", stub);

      System.out.println("RMIServer is ready.");

      // Schedule the node liveness check task
      Timer timer = new Timer();
      timer.scheduleAtFixedRate(new NodeLivenessCheckTask(server), 0, NODE_CHECK_INTERVAL);

    } catch (Exception e) {
      System.err.println("RMIServer exception: " + e.toString());
      e.printStackTrace();
    }
  }

  public void startServer(String host, Integer port) {
    try {
      registryHostname = host;
      registryPort = port;
      System.setProperty("java.rmi.server.hostname", registryHostname);

      System.out.println("Starting server");
      isServerRunning = true;
      serverThread = new Thread(() -> runServer());
      serverThread.start();
      System.out.println("Started server");


    } catch (Exception e) {
      System.err.println("RMIServer exception: " + e.toString());
      e.printStackTrace();
    }
  }

  public void runServer() {
    try {
      RMIServerInterface stub = (RMIServerInterface) UnicastRemoteObject.exportObject(this, (registryPort - 1));
      // Register the stub with the RMI registry
      registry = LocateRegistry.createRegistry(registryPort);
      registry.bind("0", stub);
    } catch (Exception e) {
      System.err.println("Error running server: " + e);
    }
    System.out.println("RMIServer is ready.");
    // Schedule the node liveness check task
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new NodeLivenessCheckTask(this), 0, NODE_CHECK_INTERVAL);

    while (isServerRunning) {
      try {
        Thread.sleep(5000); // Delay of 10 seconds
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    timer.cancel();
    try {
      for (String node : registry.list()) {
        if (!node.equals("0")) {
          this.unregisterNode(node, registry);
        }
      }
      registry.unbind("0");
      UnicastRemoteObject.unexportObject(this, true);
      System.out.println("Unbound and unregistered");
      UnicastRemoteObject.unexportObject(registry, true);
      System.out.println("Deleted registry");
    } catch (Exception e) {
      System.err.println("Error when shutting down server: " + e);
    }


    serverThread.interrupt();

  }

  @Override
  public void registerNode(String nodeID, RMIInterface node) throws RemoteException {
    try {
      registry.bind(nodeID, node);
      registeredNodes.add(nodeID);
      System.out.println("Node " + nodeID + " registered.");
    } catch (Exception e) {
      System.err.println("Binding exception: " + e.toString());
      e.printStackTrace();
    }
  }

  @Override
  public void unregisterNode(String nodeID, Registry reg) throws RemoteException {
    try {
      registeredNodes.remove(nodeID);
      reg.unbind(nodeID);
      System.out.println("Node " + nodeID + " unregistered.");
    } catch (Exception e) {
      System.err.println("Error occurred when unregistering node: " + e.toString());
      e.printStackTrace();
    }
  }

  private void checkNodeLiveness() {
    System.out.println("Checking Node liveliness");
    List<String> nodesCopy;
    synchronized (registeredNodes) {
      nodesCopy = new ArrayList<>(registeredNodes);
    }
    try {
      for (String nodeID : nodesCopy) {
        try {
          RMIInterface nodeStub = (RMIInterface) registry.lookup(nodeID);
          nodeStub.isAlive();
        } catch (Exception e) {
          System.err.println("Node " + nodeID + " inaccessible, unregistering.");
          unregisterNode(nodeID, registry); // Node is inactive, remove it from the registry
        }
      }
    } catch (RemoteException e) {
      System.err.println("Error occurred when accessing the RMI registry: " + e.toString());
      e.printStackTrace();
    }
  }

  private static class NodeLivenessCheckTask extends TimerTask {
    private RMIServer server;

    public NodeLivenessCheckTask(RMIServer server) {
      this.server = server;
    }

    @Override
    public void run() {
      server.checkNodeLiveness();
    }
  }

  public void stopServer() {
    // Stop the server loop
    isServerRunning = false;
  }
}
