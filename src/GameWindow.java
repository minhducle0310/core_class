import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

final class GameWindow extends JFrame {
    private final GamePanel panel;
    private boolean borderless;
    private Rectangle windowedBounds;

    GameWindow(GamePanel panel) {
        super("Core Clash");
        this.panel = panel;
        setContentPane(panel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(480, 340));
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                panel.clearInput();
            }
        });

        bindViewKey(KeyEvent.VK_F11, this::toggleFullScreen);
        bindViewKey(KeyEvent.VK_ESCAPE, () -> {
            if (borderless) {
                showWindowed();
            }
        });
    }

    void showGame(boolean fullScreen) {
        if (fullScreen) {
            showFullScreen();
        } else {
            showWindowed();
        }
        panel.start();
    }

    private void toggleFullScreen() {
        if (!borderless) {
            showFullScreen();
        } else {
            showWindowed();
        }
    }

    private void showFullScreen() {
        Rectangle displayBounds = getGraphicsConfiguration().getBounds();
        if (isDisplayable()) {
            windowedBounds = getBounds();
            dispose();
        }
        panel.clearInput();
        borderless = true;
        setUndecorated(true);
        setBounds(displayBounds);
        setVisible(true);
    }

    private void showWindowed() {
        if (borderless) {
            dispose();
            borderless = false;
        }
        panel.clearInput();
        setUndecorated(false);
        pack();
        if (windowedBounds == null) {
            setLocationRelativeTo(null);
        } else {
            setBounds(windowedBounds);
        }
        setVisible(true);
    }

    private void bindViewKey(int code, Runnable action) {
        // A release toggles once even if the operating system repeats a held key.
        KeyStroke key = KeyStroke.getKeyStroke(code, 0, true);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, key);
        getRootPane().getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }
}
