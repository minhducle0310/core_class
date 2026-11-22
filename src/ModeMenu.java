import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;

final class ModeMenu {
    private static final Rectangle DUEL_CARD = new Rectangle(32, 190, 224, 154);
    private static final Rectangle BOSS_CARD = new Rectangle(272, 190, 224, 154);
    private static final Rectangle HIVE_CARD = new Rectangle(512, 190, 224, 154);
    private static final Color TEXT = new Color(240, 245, 253);
    private static final Color MUTED = new Color(161, 179, 201);
    private static final Color BLUE = new Color(105, 211, 252);
    private static final Color AMBER = new Color(255, 196, 104);
    private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 56);
    private static final Font MODE = new Font(Font.SANS_SERIF, Font.BOLD, 24);
    private static final Font LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 15);
    private static final Font KEY = new Font(Font.MONOSPACED, Font.BOLD, 14);

    private ModeMenu() {}

    static GameMode modeAt(int x, int y) {
        if (DUEL_CARD.contains(x, y)) {
            return GameMode.DUEL;
        }
        if (HIVE_CARD.contains(x, y)) return GameMode.HIVE;
        return BOSS_CARD.contains(x, y) ? GameMode.BOSS : null;
    }

    static void draw(Graphics2D canvas, int width, int height, GameMode selected) {
        canvas.setPaint(new GradientPaint(0, 0, new Color(23, 38, 54),
            0, height, new Color(10, 15, 25)));
        canvas.fillRect(0, 0, width, height);
        canvas.setColor(new Color(42, 61, 79));
        canvas.drawLine(68, 165, width - 68, 165);
        canvas.drawLine(68, 370, width - 68, 370);
        canvas.setColor(TEXT);
        canvas.setFont(TITLE);
        drawCentered(canvas, "Core Clash", width / 2, 107);
        canvas.setColor(MUTED);
        canvas.setFont(LABEL);
        drawCentered(canvas, "Choose a game mode", width / 2, 143);

        drawCard(canvas, DUEL_CARD, "1", "Local Duel", "Two-player arena duel",
            BLUE, selected == GameMode.DUEL);
        drawCard(canvas, BOSS_CARD, "2", "Overtime Raid", "Face the foreman and crew",
            AMBER, selected == GameMode.BOSS);
        drawCard(canvas, HIVE_CARD, "3", "Ember Hive", "Hunt the Cinder Queen",
            BossView.VIOLET, selected == GameMode.HIVE);

        canvas.setFont(KEY);
        canvas.setColor(MUTED);
        drawCentered(canvas, "\u2190 \u2192  Select     Enter  Start", width / 2, 407);
    }

    private static void drawCard(Graphics2D canvas, Rectangle bounds, String key, String title,
                                 String description, Color accent, boolean selected) {
        int x = bounds.x;
        int y = bounds.y;
        canvas.setColor(selected ? new Color(30, 46, 62) : new Color(20, 30, 44));
        canvas.fillRoundRect(x, y, bounds.width, bounds.height, 20, 20);
        canvas.setStroke(new BasicStroke(selected ? 2 : 1));
        canvas.setColor(selected ? accent : new Color(62, 80, 101));
        canvas.drawRoundRect(x, y, bounds.width, bounds.height, 20, 20);

        canvas.setColor(new Color(11, 18, 29));
        canvas.fillRoundRect(x + 23, y + 21, 28, 28, 8, 8);
        canvas.setColor(accent);
        canvas.setFont(KEY);
        drawCentered(canvas, key, x + 37, y + 40);

        canvas.setColor(TEXT);
        canvas.setFont(MODE);
        canvas.drawString(title, x + 23, y + 91);
        canvas.setColor(MUTED);
        canvas.setFont(LABEL);
        canvas.drawString(description, x + 23, y + 120);
    }

    private static void drawCentered(Graphics2D canvas, String text, int centerX, int baseline) {
        canvas.drawString(text, centerX - canvas.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
