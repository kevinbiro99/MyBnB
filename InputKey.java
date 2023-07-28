public class InputKey {
    private String key;
    private String description;
    private String details;
    private boolean isPressed;

    public InputKey(String key, String description, boolean isPressed) {
        this.key = key;
        this.description = description;
        this.isPressed = isPressed;
    }

    public InputKey(String key, String description) {
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
}
