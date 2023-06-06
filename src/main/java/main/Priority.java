package main;

public record Priority(int value, Node node) {
  public Priority newPriority(int value) {
    return new Priority(value, node);
  }
}
