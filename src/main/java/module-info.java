module main.srproject {
  requires javafx.controls;
  requires javafx.fxml;

  requires org.controlsfx.controls;
  requires org.kordamp.bootstrapfx.core;
  requires java.rmi;

  opens main.srproject to javafx.fxml;
  exports main.srproject;
}