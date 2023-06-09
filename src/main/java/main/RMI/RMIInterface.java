package main.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
  void electionMessage(String nodeIDString) throws RemoteException;

  void answerAlive(String destinationID, String nodeIdReplying) throws RemoteException;

  void victoryMessage(String nodeIDString) throws RemoteException;

  boolean isAlive() throws RemoteException;
}