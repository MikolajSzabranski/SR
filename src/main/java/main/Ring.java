package main;

import java.util.ArrayList;

public class Ring {   //TODO - może statyczna klasa nawet, zobaczymy
  private static ArrayList<Node> NODES;
  private static Priority TOP_PRIORITY;

  public void setPriorityAndOrderToNodes(ArrayList<Node> newOrder, Priority topPriority) {
    NODES.forEach(node -> {
          node.setOrder(newOrder);
          node.setPriority(topPriority);
        }
    );
  }

  public void findTopPriority() {
    TOP_PRIORITY = new Priority(0, null);
    NODES.forEach(node ->
        {
          if (node.getPriority().value() > TOP_PRIORITY.value()) {
            TOP_PRIORITY = node.getPriority();
          }
        }
    );
  }
}
