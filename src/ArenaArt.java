import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;

final class ArenaArt {
    private static final Color STRUCTURE = new Color(39, 57, 75);
    private static final Color SEAM = new Color(26, 42, 59);
    private static final Color EDGE = new Color(70, 100, 123);
    private static final Color RAIL = new Color(140, 188, 207);

    private ArenaArt() {}

    static void draw(Graphics2D canvas) {
        int left = Arena.LEFT;
        int top = Arena.TOP;
        int right = left + Arena.WIDTH;
        int bottom = top + Arena.HEIGHT;
        canvas.setPaint(new GradientPaint(0, top, new Color(37, 54, 73),
            0, bottom, new Color(15, 23, 36)));
        canvas.fillRect(left, top, Arena.WIDTH, Arena.HEIGHT);

        for (int x = left + 31; x < right - 10; x += 64) {
            canvas.setColor(SEAM);
            canvas.fillRect(x, top + 1, 8, Arena.HEIGHT - 1);
            canvas.setColor(STRUCTURE);
            canvas.fillRect(x + 1, top + 1, 3, Arena.HEIGHT - 1);
        }
        canvas.setColor(SEAM);
        for (int y = top + 49; y < Arena.FLOOR_Y; y += 66) {
            canvas.drawLine(left, y, right, y);
            canvas.drawLine(left, y + 2, right, y + 2);
        }

        canvas.setColor(STRUCTURE);
        canvas.fillRect(left, top + 9, Arena.WIDTH, 11);
        canvas.setColor(EDGE);
        canvas.drawLine(left, top + 19, right, top + 19);
        for (int x = left + 18; x < right - 19; x += 41) {
            canvas.fillRoundRect(x, top + 12, 18, 3, 2, 2);
        }
        drawReactorWindow(canvas, Arena.CENTER_X, top + 45);
        canvas.setColor(new Color(42, 70, 88));
        for (int x = left + 12; x < right; x += 46) {
            int buildingHeight = 20 + Math.floorMod(x * 37, 55);
            canvas.fillRect(x, Arena.FLOOR_Y - buildingHeight, 32, buildingHeight);
            canvas.setColor(new Color(87, 137, 147));
            for (int y = Arena.FLOOR_Y - buildingHeight + 8; y < Arena.FLOOR_Y - 8; y += 12) {
                canvas.fillRect(x + 6, y, 4, 3);
                canvas.fillRect(x + 19, y, 4, 3);
            }
            canvas.setColor(new Color(42, 70, 88));
        }
        drawServicePanel(canvas, left + 33);
        drawServicePanel(canvas, right - 117);

        for (Arena.Platform platform : Arena.PLATFORMS) {
            drawLedge(canvas, platform.left(), platform.right(), platform.y(), 12);
        }
        drawLedge(canvas, left, right, Arena.FLOOR_Y, bottom - Arena.FLOOR_Y);
        canvas.setColor(EDGE);
        canvas.drawRect(left, top, Arena.WIDTH, Arena.HEIGHT);
    }

    private static void drawReactorWindow(Graphics2D canvas, int center, int top) {
        int lensY = top + 45;
        canvas.setColor(new Color(17, 29, 44));
        canvas.fillRoundRect(center - 105, top, 210, 92, 13, 13);
        canvas.setColor(EDGE);
        canvas.drawRoundRect(center - 105, top, 210, 92, 13, 13);
        canvas.setPaint(new RadialGradientPaint(center, lensY, 87, new float[]{0f, 0.55f, 1f},
            new Color[]{new Color(52, 107, 124), new Color(27, 57, 75), new Color(19, 34, 51)}));
        canvas.fillRoundRect(center - 98, top + 7, 196, 78, 8, 8);
        canvas.setColor(STRUCTURE);
        canvas.drawLine(center - 96, lensY + 22, center + 96, lensY + 22);
        canvas.drawLine(center - 65, top + 8, center - 65, top + 84);
        canvas.drawLine(center + 65, top + 8, center + 65, top + 84);
        canvas.setColor(new Color(74, 132, 148));
        canvas.drawOval(center - 23, lensY - 23, 46, 46);
        canvas.drawOval(center - 16, lensY - 16, 32, 32);
        canvas.fillRoundRect(center - 5, lensY - 11, 10, 22, 5, 5);
    }

    private static void drawServicePanel(Graphics2D canvas, int x) {
        int y = Arena.FLOOR_Y - 70;
        canvas.setColor(SEAM);
        canvas.fillRoundRect(x, y, 84, 63, 8, 8);
        canvas.setColor(STRUCTURE);
        canvas.drawRoundRect(x, y, 84, 63, 8, 8);
        for (int vent = 0; vent < 4; vent++) {
            canvas.fillRoundRect(x + 12, y + 16 + vent * 8, 59, 3, 3, 3);
        }
    }

    private static void drawLedge(Graphics2D canvas, int left, int right, int y, int height) {
        canvas.setColor(SEAM);
        canvas.fillRect(left, y, right - left, height);
        canvas.setColor(STRUCTURE);
        canvas.fillRect(left, y + 3, right - left, height - 4);
        canvas.setColor(RAIL);
        canvas.fillRect(left, y, right - left, 2);
        canvas.setColor(EDGE);
        canvas.drawLine(left, y + height - 1, right - 1, y + height - 1);
        for (int x = left + 13; x < right - 8; x += 24) {
            canvas.drawLine(x, y + 5, x - 4, y + height - 3);
            canvas.fillOval(x + 3, y + 5, 2, 2);
        }
    }
}
