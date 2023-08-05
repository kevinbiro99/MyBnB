public class InputKey {
  private String key;
  private String description;
  private boolean isPressed;
  private int id;

  public InputKey(String key, String description, boolean isPressed) {
    this.id = -1;
    this.key = key;
    this.description = description;
    this.isPressed = isPressed;
  }

  public InputKey(String key, String description) {
    this.id = -1;
    this.key = key;
    this.description = description;
    this.isPressed = false;
  }

  public InputKey(int id, String key, String description) {
    this.id = id;
    this.key = key;
    this.description = description;
    this.isPressed = false;
  }

  public String getKey() {
    return key;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPressed() {
    return isPressed;
  }

  public void setPressed(boolean isPressed) {
    this.isPressed = isPressed;
  }

  public int getId() {
    return id;
  }
}
