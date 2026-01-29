import java.io.Serializable;

public class ActionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public PlayerAction action;
    public int amount;

    public ActionMessage(PlayerAction action, int amount) {
        this.action = action;
        this.amount = amount;
    }
}