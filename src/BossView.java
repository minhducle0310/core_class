import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class BossView {
    static final Color AMBER = new Color(255, 190, 94);
    static final Color CORAL = new Color(255, 113, 84);
    static final Color VIOLET = new Color(206, 155, 255);
    static final Color EXPOSED = new Color(112, 243, 181);
    private static final Color INK = new Color(12, 17, 25);
    private static final Color HARD_HAT = new Color(255, 181, 62);
    private static final Color HAT_SHADE = new Color(194, 105, 36);
    private static final Color PALE = new Color(230, 243, 205);
    private static final Color PAPER = new Color(255, 226, 157);
    private static final Color PAPER_INK = new Color(121, 54, 66);
    private static final BasicStroke SIGNAL_LINE = new BasicStroke(2);
    private static final Font SPAWN_FONT = new Font(Font.MONOSPACED, Font.BOLD, 11);

    private BossView() {}

    static void drawHazards(Graphics2D canvas, Boss boss) {
        if (boss.impact() != null) {
            drawImpact(canvas, boss.impact(), boss.impactStrength());
        }
        if (boss.warning() != null) {
            drawWarning(canvas, boss.warning(), boss.warningProgress());
        }
        for (RaidHazard hazard : boss.hazards()) {
            drawHazard(canvas, hazard, warningColor(boss.movingPattern()), boss.animationSeconds());
        }
    }

    static Color warningColor(Boss.Pattern pattern) {
        return switch (pattern) {
            case FLOOR_SURGE, POLARITY_LOW -> AMBER;
            case PAPER_JAM -> PAPER;
            case CORE_BEAM, FIRE_BOLT, FORKED_FLARE -> CORAL;
            case PUNCH_CLOCK -> CORAL;
            case GRID_OVERLOAD -> new Color(93, 217, 255);
            case PUNCH_ECHO, POLARITY_HIGH -> VIOLET;
        };
    }

    private static void drawWarning(Graphics2D canvas, Boss.Strike strike, double charge) {
        Color color = warningColor(strike.pattern());
        for (double angle : strike.angles()) {
            Graphics2D aim = (Graphics2D) canvas.create();
            aim.clipRect(Arena.LEFT, Arena.TOP, Arena.WIDTH, Arena.FLOOR_Y - Arena.TOP);
            aim.translate(Boss.X, Boss.MUZZLE_Y);
            aim.rotate(angle);
            aim.setColor(withAlpha(CORAL, 90 + (int) (125 * charge)));
            int offset = (int) (charge * 36) % 12;
            for (int x = offset; x < Arena.WIDTH; x += 12) aim.fillRect(x, -1, 7, 2);
            aim.dispose();
        }
        for (Boss.Zone zone : strike.zones()) {
            if (strike.pattern().travels()) {
                drawTravelWarning(canvas, zone, color, charge, strike.pattern() == Boss.Pattern.PAPER_JAM);
                continue;
            }
            int x = zone.x();
            int y = zone.y();
            int w = zone.width();
            int h = zone.height();
            canvas.setColor(withAlpha(color, 25 + (int) (charge * 55)));
            canvas.fillRect(x, y, w, h);
            canvas.setColor(withAlpha(color, 190));
            canvas.drawRect(x, y, w, h);
            canvas.fillRect(x, y + h - 4, (int) Math.round(w * charge), 4);
            canvas.setColor(withAlpha(color, 105));
            if (strike.pattern().usesColumns()) {
                for (int mark = y + 10; mark < y + h - 8; mark += 20) {
                    canvas.fillRect(x + w / 2 - 1, mark, 2, 8);
                }
            } else {
                for (int mark = x + 12; mark < x + w - 9; mark += 23) {
                    canvas.drawLine(mark, y + h - 12, mark + 5, y + h - 19);
                    canvas.drawLine(mark + 5, y + h - 19, mark + 10, y + h - 12);
                }
            }
        }
    }

    private static void drawTravelWarning(Graphics2D canvas, Boss.Zone zone, Color color,
                                          double charge, boolean fromEdges) {
        int x = zone.x();
        int y = zone.y();
        int width = zone.width();
        int bottom = y + zone.height();
        canvas.setColor(withAlpha(color, 14 + (int) (charge * 28)));
        canvas.fillRect(x, y, width, zone.height());
        canvas.setColor(withAlpha(color, 100));
        canvas.drawLine(x, y, x + width, y);
        canvas.drawLine(x, bottom - 1, x + width, bottom - 1);
        int middle = y + zone.height() / 2;
        int scroll = (int) (charge * 64) % 32;
        for (int laneX = x + 16; laneX < x + width - 16; laneX += 32) {
            int direction = (laneX < Boss.X ? -1 : 1) * (fromEdges ? -1 : 1);
            int chevron = laneX + direction * (scroll - 16);
            canvas.drawLine(chevron - direction * 4, middle - 4, chevron, middle);
            canvas.drawLine(chevron, middle, chevron - direction * 4, middle + 4);
        }
        canvas.setColor(withAlpha(color, 200));
        int amount = (int) (charge * width / 2);
        canvas.fillRect(Boss.X - amount, bottom - 3, amount * 2, 3);
    }

    private static void drawImpact(Graphics2D canvas, Boss.Strike strike, double strength) {
        Color color = warningColor(strike.pattern());
        for (Boss.Zone zone : strike.zones()) {
            int x = zone.x();
            int y = zone.y();
            int w = zone.width();
            int h = zone.height();
            canvas.setColor(withAlpha(color, (int) (strength * 150)));
            canvas.fillRect(x, y, w, h);
            canvas.setColor(withAlpha(color, (int) (strength * 240)));
            if (strike.pattern().usesColumns()) {
                drawDischarge(canvas, zone, color, strength);
            } else if (strike.pattern() == Boss.Pattern.CORE_BEAM) {
                canvas.setColor(withAlpha(color, (int) (strength * 200)));
                canvas.fillRect(x, y + 4, w, h - 8);
                canvas.setColor(withAlpha(PALE, (int) (strength * 230)));
                canvas.fillRect(x, y + h / 2 - 3, w, 6);
            } else {
                for (int layer = 0; layer < 4; layer++) {
                    int rippleY = y + (int) ((1 - strength) * 34) + 12 + layer * (h - 60) / 4;
                    canvas.fillRect(x, rippleY, w, 2 + (int) (strength * 3));
                }
            }
        }
    }

    private static void drawDischarge(Graphics2D canvas, Boss.Zone zone, Color color, double strength) {
        int center = zone.x() + zone.width() / 2;
        int width = Math.max(2, (int) (zone.width() * strength / 3));
        int y = zone.y();
        int height = zone.height();
        canvas.setColor(withAlpha(color, (int) (strength * 125)));
        canvas.fillRect(center - width, y, width * 2, height);
        canvas.setColor(withAlpha(PALE, (int) (strength * 237)));
        int[] xs = {center, center - width / 2, center + width / 3,
            center - width / 3, center + width / 2, center};
        int[] ys = {y, y + height / 5, y + height * 2 / 5,
            y + height * 3 / 5, y + height * 4 / 5, y + height};
        Graphics2D bolt = (Graphics2D) canvas.create();
        bolt.setStroke(SIGNAL_LINE);
        bolt.drawPolyline(xs, ys, xs.length);
        bolt.dispose();
    }

    private static void drawHazard(Graphics2D canvas, RaidHazard hazard, Color accent, double time) {
        Graphics2D trail = (Graphics2D) canvas.create();
        trail.translate(hazard.x(), hazard.y());
        if (hazard.kind() == RaidHazard.Kind.BOLT) {
            trail.rotate(hazard.angle());
            trail.setColor(hazard.returning() ? EXPOSED : CORAL);
            int plume = 16 + (int) (time * 50) % 7;
            trail.fillPolygon(new int[]{-plume, -5, 3, 8, 3, -5}, new int[]{0,-7,-7,0,7,7}, 6);
            trail.setColor(PALE);
            trail.fillRect(-4, -3, 8, 6);
        } else {
            trail.scale(hazard.direction(), 1);
            if (hazard.kind() == RaidHazard.Kind.SURGE) drawSurge(trail, accent, time);
            else drawCard(trail, time);
        }
        trail.dispose();
    }

    private static void drawSurge(Graphics2D trail, Color accent, double time) {
        int h = RaidHazard.Kind.SURGE.halfHeight;
        int w = RaidHazard.Kind.SURGE.halfWidth;
        int ripple = (int) (time * 32) % 8;
        trail.setColor(withAlpha(accent, 46));
        trail.fillRect(-w, -h, w * 2, h * 2);
        trail.setColor(withAlpha(accent, 120));
        trail.fillPolygon(new int[]{-w - 19, -6, -3 - ripple, w - 4, w, w - 6, w - 2, -w},
            new int[]{h, 8, -5, -h, -h + 9, -3, h - 3, h}, 8);
        trail.setColor(withAlpha(accent, 215));
        trail.fillPolygon(new int[]{-w, -5, 3 - ripple, w - 2, w, w - 7, w - 3},
            new int[]{h, 13, 3, -h + 4, -h + 13, 3, h}, 7);
        trail.setColor(PALE);
        trail.setStroke(SIGNAL_LINE);
        trail.drawPolyline(new int[]{w - 2, w - 4, w - 9, w - 3},
            new int[]{-h + 5, -h + 13, 3, h - 3}, 4);
        trail.fillRect(-w - 12 - ripple, 12, 6, 2);
        trail.fillRect(-w - 2 - ripple, 4, 3, 2);
        trail.fillRect(-w - 17 - ripple, 19, 4, 2);
    }

    private static void drawCard(Graphics2D trail, double time) {
        int w = RaidHazard.Kind.CARD.halfWidth;
        int h = RaidHazard.Kind.CARD.halfHeight;
        trail.setColor(withAlpha(PAPER, 95));
        trail.fillRoundRect(-w, -h, w * 2, h * 2, 5, 5);
        trail.drawLine(-w - 17, -5, -w - 2, -5);
        trail.drawLine(-w - 23, 6, -w - 2, 6);
        trail.rotate(Math.sin(time * 17) * 0.14);
        trail.setColor(PAPER);
        trail.fillRect(-11, -7, 22, 14);
        trail.setColor(PAPER_INK);
        trail.fillRect(-8, -4, 8, 2);
        trail.fillRect(-8, 0, 6, 2);
        trail.drawRect(3, -3, 5, 6);
        trail.fillRect(7, 4, 3, 2);
    }

    static void drawBoss(Graphics2D canvas, Boss boss, GameAssets.BossSprites sprites) {
        Graphics2D robot = (Graphics2D) canvas.create();
        robot.translate(Boss.X, Arena.FLOOR_Y);
        robot.setColor(withAlpha(INK, 180));
        robot.fillOval(-53, -7, 106, 13);

        robot.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        boolean offline = boss.health() == 0;
        Color signal = boss.exposed() ? EXPOSED : AMBER;
        if (!offline) {
            robot.setColor(withAlpha(signal, boss.flashing() ? 210 : 105));
            robot.setStroke(SIGNAL_LINE);
            robot.drawOval(-46, -15, 92, 16);
        }
        double windup = boss.warningProgress();
        double recoil = boss.recoilStrength();
        int shift = windup < 0.35 ? 0 : ((int) (boss.animationSeconds() * 23) % 2) * 4 - 2;
        int height = offline ? 112 - (int) (boss.shutdownProgress() * 30)
            : 112 - (int) (windup * windup * 15) + (int) (recoil * 7);
        int top = -height;
        robot.translate(shift, 0);
        if (sprites.foreman() != null) {
            robot.drawImage(sprites.foreman(), -56, top, 112, height, null);
        } else {
            robot.setColor(new Color(58, 106, 75));
            robot.fillRect(-37, top + 20, 74, height - 24);
            robot.setColor(PALE);
            robot.fillRect(-24, top + 34, 48, 10);
        }
        drawHardHat(robot, top - (int) (recoil * 7));
        drawEyes(robot, boss, top, height);
        if (boss.warning() != null && boss.warning().pattern().firesBolts()) {
            int y = Boss.MUZZLE_Y - Arena.FLOOR_Y;
            int radius = 3 + (int) (windup * 5);
            robot.setColor(INK);
            robot.fillRect(-11, y - 10, 22, 20);
            robot.setColor(CORAL);
            robot.fillRect(-radius, y - radius, 2 * radius, 2 * radius);
            robot.setColor(PALE);
            robot.fillRect(-2, y - 2, 4, 4);
        }
        if (boss.warning() != null && windup > 0.45) {
            Color charge = warningColor(boss.warning().pattern());
            robot.setColor(withAlpha(charge, 145 + (int) (windup * 95)));
            robot.fillRect(-47, top + 15 - shift * 2, 5, 5);
            robot.fillRect(42, top + 9 + shift * 2, 4, 4);
            robot.fillRect(49, top + 23 - shift, 2, 4);
        }
        if (boss.exposed()) {
            drawMug(robot, boss.ventProgress());
        }
        robot.dispose();
    }

    private static void drawHardHat(Graphics2D robot, int top) {
        robot.setColor(INK);
        robot.fillRect(-34, top + 1, 68, 27);
        robot.fillRect(-43, top + 24, 86, 10);
        robot.setColor(HARD_HAT);
        robot.fillRect(-28, top + 5, 56, 20);
        robot.fillRect(-39, top + 24, 78, 6);
        robot.setColor(HAT_SHADE);
        robot.fillRect(19, top + 9, 9, 15);
        robot.fillRect(-31, top + 28, 66, 3);
        robot.setColor(PALE);
        robot.fillRect(-20, top + 7, 26, 4);
    }

    private static void drawEyes(Graphics2D robot, Boss boss, int top, int height) {
        int y = top + height * 5 / 16;
        robot.setColor(INK);
        if (boss.health() == 0) {
            robot.drawLine(-17, y + 1, -7, y + 8);
            robot.drawLine(-17, y + 8, -7, y + 1);
            robot.drawLine(7, y + 1, 17, y + 8);
            robot.drawLine(7, y + 8, 17, y + 1);
        } else {
            int look = boss.facingX() * 3;
            int eyeHeight = boss.exposed() ? 3 : 8;
            robot.fillRect(-15 + look, y + 2, 5, eyeHeight);
            robot.fillRect(10 + look, y + 2, 5, eyeHeight);
        }
    }

    private static void drawMug(Graphics2D robot, double progress) {
        robot.setColor(INK);
        robot.fillRect(27, -58, 23, 18);
        robot.setColor(PALE);
        robot.fillRect(30, -55, 13, 12);
        robot.drawRect(43, -53, 5, 6);
        int steam = (int) (progress * 14) % 8;
        robot.setColor(withAlpha(PALE, 135));
        robot.fillRect(33, -60 - steam, 2, 6);
        robot.fillRect(39, -64 - steam, 2, 5);
    }

    static void drawIntern(Graphics2D canvas, RaidIntern intern, BufferedImage sprite) {
        int x = (int) Math.round(intern.x());
        int floor = Arena.FLOOR_Y;
        if (intern.state() == RaidIntern.State.ARRIVING) {
            canvas.setColor(withAlpha(AMBER, 100));
            canvas.fillRect(x - 16, floor - 36, 32, 36);
            canvas.setColor(AMBER);
            canvas.drawRect(x - 16, floor - 36, 32, 36);
            canvas.fillRect(x - 16, floor - 3, (int) (32 * intern.arrivalProgress()), 3);
            canvas.setFont(SPAWN_FONT);
            String label = "Intern inbound";
            canvas.drawString(label, x - canvas.getFontMetrics().stringWidth(label) / 2, floor - 43);
            return;
        }
        if (intern.state() == RaidIntern.State.WINDUP) {
            int end = x + intern.facing() * RaidIntern.CHARGE_DISTANCE;
            int left = Math.min(x, end) - 17;
            int width = RaidIntern.CHARGE_DISTANCE + 34;
            canvas.setColor(withAlpha(CORAL, 75));
            canvas.fillRect(left, floor - 34, width, 34);
            canvas.setColor(withAlpha(CORAL, 180));
            canvas.drawRect(left, floor - 34, width, 34);
            canvas.fillRect(left, floor - 2, (int) (width * intern.chargeWarningProgress()), 2);
        }
        if (intern.state() == RaidIntern.State.CHARGING) {
            int behind = x - intern.facing() * 22;
            canvas.setColor(withAlpha(CORAL, 140));
            canvas.drawLine(behind, floor - 16, behind - intern.facing() * 16, floor - 16);
            canvas.drawLine(behind, floor - 9, behind - intern.facing() * 10, floor - 9);
        }
        if (sprite != null) {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            canvas.drawImage(sprite, x - 20, floor - 37, 40, 40, null);
        } else {
            canvas.setColor(EXPOSED);
            canvas.fillRect(x - 9, floor - 23, 18, 20);
        }
        canvas.setColor(INK);
        canvas.fillRect(x - 10, floor - 41, 20, 4);
        canvas.setColor(AMBER);
        canvas.fillRect(x - 9, floor - 40, 18 * intern.health() / RaidIntern.MAX_HEALTH, 2);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
