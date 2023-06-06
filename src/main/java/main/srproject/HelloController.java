package main.srproject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import main.Node;
import main.Priority;
import main.Ring;

public class HelloController {
  @FXML
  private Label nodeValueText;
  @FXML
  private Label result;
  @FXML
  private Button turnOff;
  @FXML
  private TextField inputValue;

  @FXML
  protected void onSetValueClick() {
//    Node.ORDER.add(new Node(nodeValueText.getText()));
//    Ring.NODES.add(new Node(null, null, new Priority(Integer.parseInt(nodeValueText.getText()), null)));
//    Node.CURRENT_NODE.setPriority(new Priority(Integer.parseInt(nodeValueText.getText()), null));
    Node.CURRENT_NODE.setAlive(true);
  }

  @FXML
  protected void onTurnOffClick() {
    Node.CURRENT_NODE.setAlive(!Node.CURRENT_NODE.isAlive());
    turnOff.setText(Node.CURRENT_NODE.isAlive() ? "Wylacz element pierscienia" : "Wlacz element pierscienia");
  }

  protected void printResult(String message) {
    result.setText(message);
  }
}