package main;

import java.util.List;

public class Node {
  private static Node LEADER;
  private Node prev;
  private Node next;
  private Priority priority;
  private List<Node> order; //FIXME - nwm czy na liście to będziemy robić, pewnie nie

  public Node(Node prev, Node next, Priority priority) {
    this.prev = prev;
    this.next = next;
    this.priority = priority;
  }

  public void setOrder(List<Node> order) {
    this.order = order;
  }

  public void setPrev(Node prev) {
    this.prev = prev;
  }

  public void setNext(Node next) {
    this.next = next;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public Priority getPriority() {
    return priority;
  }
}
