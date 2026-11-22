import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

final class GamePanel extends JPanel {
    private record FootPose(int offset, int lift) {}

    private static final int VIEW_WIDTH = 768;
    private static final int VIEW_HEIGHT = 500;
    private static final int MAX_RENDER_HEIGHT = 1080;
    private static final int FULL_QUALITY_DISPLAY_HEIGHT = 1440;
    private static final int MIN_LARGE_DISPLAY_RENDER_HEIGHT = 720;
    private static final int TICK_MS = 16;
    private static final int BLUE_DASH_KEY = KeyEvent.VK_Q;
    private static final int RED_DASH_KEY = KeyEvent.VK_SLASH;
    private static final int HEALTH_PANEL_WIDTH = 228;
    private static final int WEAPON_ICON_SIZE = 28;
    private static final int DASH_METER_WIDTH = 38;
    private static final Color BACKGROUND = new Color(13, 18, 27);
    private static final Color PANEL = new Color(23, 32, 46);
    private static final Color GRID = new Color(39, 53, 72);
    private static final Color BORDER = new Color(63, 81, 106);
    private static final Color TEXT = new Color(240, 245, 253);
    private static final Color MUTED = new Color(157, 173, 194);
    private static final Color BLUE = new Color(93, 202, 247);
    private static final Color RED = new Color(255, 122, 132);
    private static final Color SUPPLY_GOLD = new Color(255, 208, 104);
    private static final Color REPAIR_GREEN = new Color(126, 228, 162);
    private static final BasicStroke LEG_STROKE = new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final FootPose IDLE_FOOT = new FootPose(0, 0);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    private static final Font RAID_TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 20);
    private static final Font RESULT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 36);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font KEY_FONT = new Font(Font.MONOSPACED, Font.BOLD, 12);

    private Game game;
    private GameMode selectedMode = GameMode.DUEL;
    private final GameAssets assets = new GameAssets();
    private final CombatEffects combatEffects = new CombatEffects(assets);
    private BufferedImage arenaBackground;
    private BufferedImage frame;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> newlyPressedKeys = new HashSet<>();
    private final Timer timer;

    GamePanel(Game game) {
        this.game = game;
        setPreferredSize(new Dimension(VIEW_WIDTH, VIEW_HEIGHT));
        setBackground(BACKGROUND);

        bindKey(KeyEvent.VK_W);
        bindKey(KeyEvent.VK_A);
        bindKey(KeyEvent.VK_D);
        bindKey(KeyEvent.VK_F);
        bindKey(BLUE_DASH_KEY);
        bindKey(KeyEvent.VK_UP);
        bindKey(KeyEvent.VK_LEFT);
        bindKey(KeyEvent.VK_RIGHT);
        bindKey(KeyEvent.VK_ENTER);
        bindKey(RED_DASH_KEY);
        bindKey(KeyEvent.VK_R);
        bindKey(KeyEvent.VK_M);
        bindKey(KeyEvent.VK_1);
        bindKey(KeyEvent.VK_2);
        bindKey(KeyEvent.VK_3);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                selectModeAt(event);
            }
        });

        timer = new Timer(TICK_MS, event -> tick());
    }

    void start() {
        timer.start();
    }

    void clearInput() {
        pressedKeys.clear();
        newlyPressedKeys.clear();
    }

    private void bindKey(int keyCode) {
        bindKeyState(keyCode, false);
        bindKeyState(keyCode, true);
    }

    private void bindKeyState(int keyCode, boolean released) {
        KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, 0, released);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, stroke);
        getActionMap().put(stroke, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (released) {
                    pressedKeys.remove(keyCode);
                } else if (pressedKeys.add(keyCode)) {
                    // Holding a key can generate repeat events. Count only its first press.
                    newlyPressedKeys.add(keyCode);
                }
            }
        });
    }

    private int axis(int negativeKey, int positiveKey) {
        int negative = pressedKeys.contains(negativeKey) ? 1 : 0;
        int positive = pressedKeys.contains(positiveKey) ? 1 : 0;
        return positive - negative;
    }

    private void tick() {
        if (game == null) {
            updateMenu();
            newlyPressedKeys.clear();
            return;
        }
        if (newlyPressedKeys.contains(KeyEvent.VK_M)) {
            selectedMode = game.mode();
            game = null;
            clearInput();
            repaint();
            return;
        }
        if (!newlyPressedKeys.contains(KeyEvent.VK_R) || !game.rematch()) {
            updateGame();
        }
        newlyPressedKeys.clear();
        repaint();
    }

    private void updateMenu() {
        if (newlyPressedKeys.contains(KeyEvent.VK_1)) {
            startMode(GameMode.DUEL);
        } else if (newlyPressedKeys.contains(KeyEvent.VK_2)) {
            startMode(GameMode.BOSS);
        } else if (newlyPressedKeys.contains(KeyEvent.VK_3)) {
            startMode(GameMode.HIVE);
        } else if (newlyPressedKeys.contains(KeyEvent.VK_ENTER)) {
            startMode(selectedMode);
        } else if (newlyPressedKeys.contains(KeyEvent.VK_LEFT)
                || newlyPressedKeys.contains(KeyEvent.VK_RIGHT)) {
            GameMode[] modes = GameMode.values();
            int direction = newlyPressedKeys.contains(KeyEvent.VK_LEFT) ? -1 : 1;
            selectedMode = modes[Math.floorMod(selectedMode.ordinal() + direction, modes.length)];
            repaint();
        }
    }

    private void selectModeAt(MouseEvent event) {
        if (game != null || event.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        Rectangle view = viewport();
        if (view.contains(event.getPoint())) {
            int x = (event.getX() - view.x) * VIEW_WIDTH / view.width;
            int y = (event.getY() - view.y) * VIEW_HEIGHT / view.height;
            GameMode picked = ModeMenu.modeAt(x, y);
            if (picked != null) {
                startMode(picked);
            }
        }
    }

    private void startMode(GameMode mode) {
        game = new Game(mode);
        arenaBackground = null;
        newlyPressedKeys.clear();
        repaint();
    }

    private void updateGame() {
        Game.Input oneInput = new Game.Input(
            axis(KeyEvent.VK_A, KeyEvent.VK_D),
            newlyPressedKeys.contains(KeyEvent.VK_W),
            newlyPressedKeys.contains(KeyEvent.VK_F),
            newlyPressedKeys.contains(BLUE_DASH_KEY)
        );
        Game.Input twoInput = new Game.Input(
            axis(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT),
            newlyPressedKeys.contains(KeyEvent.VK_UP),
            newlyPressedKeys.contains(KeyEvent.VK_ENTER),
            newlyPressedKeys.contains(RED_DASH_KEY)
        );
        game.update(oneInput, twoInput, TICK_MS / 1000.0);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        Rectangle view = viewport();
        if (view.isEmpty()) {
            super.paintComponent(graphics);
            return;
        }
        Graphics2D display = (Graphics2D) graphics.create();
        display.setColor(getBackground());
        // The scene and menu are opaque, so only the margins need clearing.
        if (view.width < panelWidth) {
            display.fillRect(0, 0, view.x, panelHeight);
            display.fillRect(view.x + view.width, 0, panelWidth - view.x - view.width, panelHeight);
        }
        if (view.height < panelHeight) {
            display.fillRect(view.x, 0, view.width, view.y);
            display.fillRect(view.x, view.y + view.height, view.width, panelHeight - view.y - view.height);
        }
        if (game != null) {
            resizeFrameIfNeeded(display, view);
            drawFrame();
            display.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            display.drawImage(frame, view.x, view.y, view.width, view.height, null);
        }

        // Rasterize interface text and shapes at the display's pixel density.
        display.clip(view);
        display.translate(view.x, view.y);
        display.scale(view.width / (double) VIEW_WIDTH, view.height / (double) VIEW_HEIGHT);
        display.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        display.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        display.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        if (game == null) {
            ModeMenu.draw(display, VIEW_WIDTH, VIEW_HEIGHT, selectedMode);
        } else {
            drawHud(display);
            RaidBoss boss = game.boss();
            if (game.playerOne().visibleImpact() == null && game.playerTwo().visibleImpact() == null
                    && (boss == null || boss.health() > 0 || boss.shutdownProgress() >= 1)) {
                drawRoundResult(display, game.roundResult());
            }
        }
        display.dispose();
    }

    private void resizeFrameIfNeeded(Graphics2D display, Rectangle view) {
        // Use actual display pixels, including system scaling on high-DPI monitors.
        var transform = display.getTransform();
        double deviceHeight = view.height * transform.getScaleY();
        // Lower the render budget gradually above 1440 physical pixels.
        double renderLimit = Math.max(MIN_LARGE_DISPLAY_RENDER_HEIGHT,
            MAX_RENDER_HEIGHT * Math.min(1, FULL_QUALITY_DISPLAY_HEIGHT / deviceHeight));
        double scale = Math.min(1, renderLimit / deviceHeight);
        int height = Math.max(1, (int) Math.round(deviceHeight * scale));
        int width = Math.max(1, (int) Math.round(view.width * transform.getScaleX() * scale));
        if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
            frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            arenaBackground = null;
        }
    }

    private Rectangle viewport() {
        double scale = Math.min(getWidth() / (double) VIEW_WIDTH, getHeight() / (double) VIEW_HEIGHT);
        int width = (int) Math.round(VIEW_WIDTH * scale);
        int height = (int) Math.round(VIEW_HEIGHT * scale);
        return new Rectangle((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
    }

    private void drawFrame() {
        if (arenaBackground == null) {
            arenaBackground = createArenaBackground(frame.getWidth(), frame.getHeight());
        }
        // Copy the static arena without scaling, then draw the active scene.
        Graphics2D buffer = frame.createGraphics();
        buffer.drawImage(arenaBackground, 0, 0, null);
        buffer.dispose();

        Graphics2D canvas = sceneGraphics(frame);
        Graphics2D playfield = (Graphics2D) canvas.create();
        playfield.clipRect(Arena.LEFT + 1, Arena.TOP + 1, Arena.WIDTH - 1, Arena.HEIGHT - 1);
        if (game.boss() instanceof CinderQueen queen) {
            ForestArt.worldTransform(playfield);
            CinderQueenView.draw(playfield, queen);
        } else if (game.boss() instanceof Boss boss) {
            GameAssets.BossSprites sprites = assets.bossSprites();
            BossView.drawHazards(playfield, boss);
            for (RaidIntern intern : boss.interns()) {
                BossView.drawIntern(playfield, intern, sprites.intern());
            }
            BossView.drawBoss(playfield, boss, sprites);
        }
        drawSupplyChest(playfield, game.supplyChest());
        if (!game.isRoundOver()) {
            combatEffects.drawDashTrail(playfield, game.playerOne(), true);
            combatEffects.drawDashTrail(playfield, game.playerTwo(), false);
        }
        combatEffects.drawSwing(playfield, game.playerOne(), BLUE);
        combatEffects.drawSwing(playfield, game.playerTwo(), RED);
        drawPlayer(playfield, game.playerOne(), BLUE);
        drawPlayer(playfield, game.playerTwo(), RED);
        for (Projectile shot : game.shots()) {
            combatEffects.drawShot(playfield, shot, shot.owner == game.playerOne() ? BLUE : RED);
        }
        combatEffects.drawImpact(playfield, game.playerOne());
        combatEffects.drawImpact(playfield, game.playerTwo());
        playfield.dispose();

        canvas.dispose();
    }

    private BufferedImage createArenaBackground(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = sceneGraphics(image);
        canvas.setColor(BACKGROUND);
        canvas.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
        if (game.mode() == GameMode.HIVE) {
            canvas.clipRect(Arena.LEFT, Arena.TOP, Arena.WIDTH, Arena.HEIGHT);
            ForestArt.worldTransform(canvas);
            ForestArt.draw(canvas, game.arena());
        } else {
            ArenaArt.draw(canvas);
        }
        canvas.dispose();
        return image;
    }

    private static Graphics2D sceneGraphics(BufferedImage image) {
        Graphics2D canvas = image.createGraphics();
        canvas.scale(image.getWidth() / (double) VIEW_WIDTH, image.getHeight() / (double) VIEW_HEIGHT);
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return canvas;
    }

    private void drawSupplyChest(Graphics2D canvas, SupplyChest chest) {
        if (chest == null) {
            return;
        }

        int x = chest.x();
        int y = chest.y();
        int radius = SupplyChest.RADIUS;
        Color accent = SUPPLY_GOLD;
        canvas.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
        canvas.fillOval(x - 24, y - 24, 48, 48);
        canvas.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 95));
        canvas.drawOval(x - 24, y - 24, 48, 48);

        canvas.setColor(new Color(77, 61, 35));
        int halfHeight = SupplyChest.HALF_HEIGHT;
        canvas.fillRoundRect(x - radius, y - halfHeight, radius * 2, halfHeight * 2, 6, 6);
        canvas.setColor(accent);
        canvas.drawRoundRect(x - radius, y - halfHeight, radius * 2, halfHeight * 2, 6, 6);
        canvas.fillRoundRect(x - radius, y - halfHeight, radius * 2, 8, 6, 6);
        canvas.fillRoundRect(x - 4, y - 4, 8, 9, 3, 3);
        canvas.setColor(BACKGROUND);
        canvas.fillRect(x - 1, y - 1, 2, 3);
    }

    private void drawPlayer(Graphics2D canvas, Player player, Color color) {
        Graphics2D robotCanvas = (Graphics2D) canvas.create();
        robotCanvas.translate(player.x(), player.y());
        double scale = Player.RADIUS / 18.0;
        robotCanvas.scale(player.facingX() * scale, scale);
        if (game.boss() != null && player.health() == 0) {
            robotCanvas.setComposite(AlphaComposite.SrcOver.derive(0.35f));
        }
        boolean running = player.isRunning() && !game.isRoundOver();
        double phase = player.runPhase();
        FootPose front = running ? runFoot(phase) : IDLE_FOOT;
        FootPose rear = running ? runFoot((phase + 0.5) % 1.0) : IDLE_FOOT;
        int rearFootY = (player.isGrounded() ? 10 : 7) - rear.lift();
        drawLeg(robotCanvas, -5, -15 + rear.offset(), rearFootY, 13, color.darker());
        drawLeg(robotCanvas, 5, 1 + front.offset(), 10 - front.lift(), 16, color);

        double bob = Math.max(front.lift(), rear.lift()) * 0.23;
        robotCanvas.translate(0, -bob);
        if (running) {
            robotCanvas.shear(-0.12, 0);
        }
        int armOffset = (int) Math.round(front.offset() * 0.15);

        robotCanvas.setColor(BACKGROUND);
        robotCanvas.fillRoundRect(-16, -11, 31, 25, 7, 7);
        robotCanvas.fillRoundRect(-9, -18, 25, 16, 6, 6);
        robotCanvas.setColor(color.darker());
        robotCanvas.fillRoundRect(-13, -8, 26, 18, 5, 5);
        robotCanvas.setPaint(new GradientPaint(0, -16, color.brighter(), 0, -4, color.darker()));
        robotCanvas.fillRoundRect(-7, -16, 21, 12, 5, 5);
        robotCanvas.setPaint(new GradientPaint(0, -6, color, 0, 8, color.darker()));
        robotCanvas.fillRoundRect(-9, -6, 17, 8, 4, 4);
        robotCanvas.fillRoundRect(8, 1 + armOffset, 10, 6, 3, 3);
        robotCanvas.setColor(BACKGROUND);
        robotCanvas.fillRoundRect(1, -13, 12, 6, 3, 3);
        robotCanvas.fillRect(-11, 5, 8, 2);
        robotCanvas.fillRect(1, 5, 4, 2);
        robotCanvas.setColor(color.brighter());
        robotCanvas.drawLine(-4, -15, 8, -15);
        robotCanvas.drawLine(-6, -4, 5, -4);
        robotCanvas.fillRect(13, 2 + armOffset, 3, 2);
        robotCanvas.setColor(TEXT);
        robotCanvas.fillRect(8, -12, 4, 2);
        if (player.visibleAttack() != null && player.visibleAttack().weapon() == Weapon.CROSS_BEAM) {
            robotCanvas.setColor(color.brighter());
            robotCanvas.fillRect(5, -2, 15, 4);
            robotCanvas.setColor(TEXT);
            robotCanvas.fillRect(15, -9, 4, 15);
        } else if (player.beamReady()) {
            robotCanvas.setColor(TEXT);
            robotCanvas.fillRect(0, -4, 5, 5);
        } else if (player.weapon().flight() != null) {
            robotCanvas.translate(14, 4 + armOffset);
            double pose = player.visibleAttack() != null ? 1 - player.swingProgress() : 0;
            robotCanvas.rotate(0.35 + pose * 0.85);
            robotCanvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            robotCanvas.drawImage(assets.weaponIcon(player.weapon()), -11, -18, 22, 22, null);
        }
        robotCanvas.dispose();
    }

    private static FootPose runFoot(double phase) {
        double cycleDistance = Player.RUN_CYCLE_DISTANCE;
        if (phase < 0.5) {
            // Move backward relative to the mech at the same rate it moves forward.
            int offset = (int) Math.round((0.25 - phase) * cycleDistance);
            return new FootPose(offset, 0);
        }

        double swing = (phase - 0.5) * 2;
        double eased = swing * swing * (3 - 2 * swing);
        int offset = (int) Math.round((eased - 0.5) * cycleDistance / 2);
        int lift = (int) Math.round(Math.sin(swing * Math.PI) * 8);
        return new FootPose(offset, lift);
    }

    private void drawLeg(Graphics2D canvas, int hipX, int x, int y, int width, Color color) {
        canvas.setStroke(LEG_STROKE);
        canvas.setColor(BACKGROUND);
        canvas.drawLine(hipX, 7, x + width / 2, y + 3);
        canvas.fillRoundRect(x, y, width, 8, 4, 4);
        canvas.setColor(color.darker());
        canvas.fillRoundRect(x + 2, y + 2, width - 4, 4, 2, 2);
        canvas.setColor(color);
        canvas.fillRect(x + width - 5, y + 2, 3, 2);
    }

    private void drawHud(Graphics2D canvas) {
        if (game.boss() == null) {
            canvas.setFont(TITLE_FONT);
            canvas.setColor(TEXT);
            drawCentered(canvas, "Core Clash", 41);
        } else {
            drawBossHud(canvas, game.boss());
        }

        int right = Arena.LEFT + Arena.WIDTH;
        drawPlayerStatus(canvas, game.playerOne(), "Blue", Arena.LEFT, BLUE);
        drawPlayerStatus(canvas, game.playerTwo(), "Red", right - HEALTH_PANEL_WIDTH, RED);
    }

    private void drawBossHud(Graphics2D canvas, RaidBoss boss) {
        int width = 178;
        int x = (VIEW_WIDTH - width) / 2;
        Color accent = switch (boss.phase()) {
            case 2 -> BossView.CORAL;
            case 3 -> BossView.VIOLET;
            default -> BossView.AMBER;
        };
        canvas.setFont(RAID_TITLE_FONT);
        canvas.setColor(TEXT);
        drawCentered(canvas, boss.name(), 20);
        canvas.setFont(KEY_FONT);
        canvas.setColor(accent);
        String phase = "Phase " + boss.phase();
        drawCentered(canvas, boss.health() + " / " + boss.maxHealth() + " HP   " + phase, 34);
        canvas.setColor(GRID);
        canvas.fillRoundRect(x, 39, width, 5, 5, 5);
        canvas.setColor(accent);
        canvas.fillRoundRect(x, 39, width * boss.health() / boss.maxHealth(), 5, 5, 5);
        canvas.setColor(boss.exposed() ? BossView.EXPOSED : MUTED);
        String status = boss.health() == 0 ? "Offline" : boss.status();
        drawCentered(canvas, status, 57);
    }

    private void drawPlayerStatus(Graphics2D canvas, Player player, String team, int x, Color color) {
        int right = x + HEALTH_PANEL_WIDTH - 12;
        int barX = x + WEAPON_ICON_SIZE + 18;
        int dashX = right - DASH_METER_WIDTH;
        int barWidth = dashX - barX - 9;
        int fillWidth = barWidth * player.health() / Player.MAX_HEALTH;

        canvas.setColor(PANEL);
        canvas.fillRoundRect(x, 7, HEALTH_PANEL_WIDTH, 47, 12, 12);
        canvas.setColor(BORDER);
        canvas.drawRoundRect(x, 7, HEALTH_PANEL_WIDTH, 47, 12, 12);

        BufferedImage icon = assets.weaponIcon(player.weapon());
        if (icon != null) {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                player.weapon().flight() == null ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
                    : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            canvas.drawImage(icon, x + 9, 16, WEAPON_ICON_SIZE, WEAPON_ICON_SIZE, null);
            canvas.setColor(BORDER);
            canvas.drawRect(x + 9, 16, WEAPON_ICON_SIZE - 1, WEAPON_ICON_SIZE - 1);
        }

        canvas.setFont(LABEL_FONT);
        canvas.setColor(color);
        canvas.drawString(team, barX, 22);
        canvas.setFont(KEY_FONT);
        String hp = player.health() + " / " + Player.MAX_HEALTH + " HP";
        canvas.drawString(hp, right - canvas.getFontMetrics().stringWidth(hp), 22);

        canvas.setFont(LABEL_FONT);
        canvas.setColor(TEXT);
        canvas.drawString(player.weapon().name(), barX, 38);
        canvas.setColor(GRID);
        canvas.fillRoundRect(barX, 44, barWidth, 5, 5, 5);
        canvas.setColor(color);
        canvas.fillRoundRect(barX, 44, fillWidth, 5, 5, 5);

        double charge = player.dashCharge();
        canvas.setFont(KEY_FONT);
        canvas.setColor(charge == 1 ? REPAIR_GREEN : MUTED);
        canvas.drawString("Dash", dashX + (DASH_METER_WIDTH - canvas.getFontMetrics().stringWidth("Dash")) / 2, 38);
        canvas.setColor(GRID);
        canvas.fillRoundRect(dashX, 44, DASH_METER_WIDTH, 5, 5, 5);
        canvas.setColor(charge == 1 ? REPAIR_GREEN : MUTED);
        canvas.fillRoundRect(dashX, 44, (int) Math.round(DASH_METER_WIDTH * charge), 5, 5, 5);
    }

    private void drawRoundResult(Graphics2D canvas, Game.RoundResult result) {
        if (result == Game.RoundResult.IN_PROGRESS) {
            return;
        }

        Color accent = switch (result) {
            case BLUE_WINS -> BLUE;
            case RED_WINS -> RED;
            case TEAM_WINS -> REPAIR_GREEN;
            case TEAM_DEFEATED -> BossView.CORAL;
            default -> TEXT;
        };
        String headline = switch (result) {
            case BLUE_WINS -> "Blue wins";
            case RED_WINS -> "Red wins";
            case TEAM_WINS -> "Raid cleared";
            case TEAM_DEFEATED -> "Team down";
            default -> "Double knockout";
        };

        canvas.setColor(new Color(8, 12, 20, 180));
        canvas.fillRect(Arena.LEFT + 1, Arena.TOP + 1, Arena.WIDTH - 1, Arena.HEIGHT - 1);
        int cardWidth = 332;
        int cardHeight = 126;
        int x = (VIEW_WIDTH - cardWidth) / 2;
        int y = Arena.TOP + (Arena.HEIGHT - cardHeight) / 2;
        canvas.setColor(PANEL);
        canvas.fillRoundRect(x, y, cardWidth, cardHeight, 18, 18);
        canvas.setColor(BORDER);
        canvas.drawRoundRect(x, y, cardWidth, cardHeight, 18, 18);
        canvas.setColor(accent);
        canvas.fillRoundRect(VIEW_WIDTH / 2 - 20, y + 13, 40, 3, 3, 3);
        canvas.setFont(RESULT_FONT);
        drawCentered(canvas, headline, y + 59);

        int keyRight = drawKeycap(canvas, "R", VIEW_WIDTH / 2 - 90, y + 80);
        canvas.setFont(LABEL_FONT);
        canvas.setColor(TEXT);
        canvas.drawString(game.boss() != null ? "Retry" : "Rematch", keyRight + 8, y + 95);
        keyRight = drawKeycap(canvas, "M", VIEW_WIDTH / 2 + 14, y + 80);
        canvas.setFont(LABEL_FONT);
        canvas.setColor(TEXT);
        canvas.drawString("Modes", keyRight + 8, y + 95);
    }

    private int drawKeycap(Graphics2D canvas, String key, int x, int y) {
        canvas.setFont(KEY_FONT);
        int width = canvas.getFontMetrics().stringWidth(key) + 14;
        canvas.setColor(PANEL);
        canvas.fillRoundRect(x, y, width, 22, 7, 7);
        canvas.setColor(BORDER);
        canvas.drawRoundRect(x, y, width, 22, 7, 7);
        canvas.setColor(TEXT);
        canvas.drawString(key, x + 7, y + 15);
        return x + width;
    }

    private void drawCentered(Graphics2D canvas, String text, int baseline) {
        FontMetrics metrics = canvas.getFontMetrics();
        canvas.drawString(text, (VIEW_WIDTH - metrics.stringWidth(text)) / 2, baseline);
    }
}
