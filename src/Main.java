import java.util.List;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        List<String> options = List.of(args);
        boolean weaponDemo = options.contains("--weapon-demo");
        boolean fullScreen = !options.contains("--windowed");
        SwingUtilities.invokeLater(() -> {
            Game game = weaponDemo
                ? new Game(Weapon.SHOCK_BATON, Weapon.GRAVITY_HAMMER)
                : null;
            GameWindow window = new GameWindow(new GamePanel(game));
            window.showGame(fullScreen);
        });
    }
}
