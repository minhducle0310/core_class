import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;

final class CombatEffects {
    private static final int FRAME_SIZE = 32;
    // The middle of the arc in the source art points up and right.
    private static final double SLASH_ROTATION = 1.1;
    private static final int SLASH_RADIUS_IN_FRAME = 13;

    private final GameAssets assets;

    CombatEffects(GameAssets assets) {
        this.assets = assets;
    }

    void drawDashTrail(Graphics2D canvas, Player player, boolean blue) {
        if (!player.isDashing()) {
            return;
        }

        BufferedImage strip = assets.dashStrip(blue);
        if (strip == null) {
            return;
        }
        Graphics2D effect = (Graphics2D) canvas.create();
        effect.translate(player.x(), player.y());
        effect.scale(player.facingX(), 1);
        effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        drawFrame(effect, strip, player.dashProgress(), -84, -31, 84, 42);
        effect.dispose();
    }

    void drawSwing(Graphics2D canvas, Player player, Color teamColor) {
        Attack attack = player.visibleAttack();
        if (attack == null) {
            return;
        }

        Weapon weapon = attack.weapon();
        if (weapon == Weapon.CROSS_BEAM) {
            Graphics2D beam = (Graphics2D) canvas.create();
            beam.translate(attack.x(), attack.y());
            beam.scale(attack.facingX(), 1);
            int thickness = Math.max(2, (int) (20 * (1 - player.swingProgress())));
            beam.setColor(teamColor);
            beam.fillRect(0, -thickness / 2, weapon.range(), thickness);
            beam.setColor(Color.WHITE);
            beam.fillRect(0, -1, weapon.range(), 3);
            beam.dispose();
            return;
        }
        if (weapon.flight() != null) return;
        double progress = player.swingProgress();
        int range = weapon.range();
        int halfAngle = weapon.halfAngleDegrees();
        BufferedImage strip = assets.swingStrip(weapon);
        Graphics2D effect = (Graphics2D) canvas.create();
        effect.translate(attack.x(), attack.y());
        effect.scale(attack.facingX(), 1);
        effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (strip == null) {
            drawFallbackSweep(effect, range, halfAngle, progress, teamColor);
        } else {
            effect.clip(new Arc2D.Double(-range, -range, range * 2, range * 2,
                -halfAngle, halfAngle * 2, Arc2D.PIE));
            if (Weapon.SHOCK_BATON.equals(weapon)) {
                int length = range - Player.RADIUS + 8;
                effect.translate((Player.RADIUS + range) / 2.0, 0);
                effect.rotate(-Math.PI / 2);
                effect.setComposite(AlphaComposite.SrcOver.derive(0.3f));
                drawFrame(effect, strip, progress, -28, -length / 2, 56, length);
                effect.setComposite(AlphaComposite.SrcOver);
                drawFrame(effect, strip, progress, -21, -length / 2, 42, length);
            } else {
                int size = (range - 3) * FRAME_SIZE / SLASH_RADIUS_IN_FRAME;
                effect.rotate(SLASH_ROTATION);
                drawFrame(effect, strip, progress, -size / 2, -size / 2, size, size);
            }
        }
        effect.dispose();
    }

    void drawShot(Graphics2D canvas, Projectile shot, Color teamColor) {
        Graphics2D effect = (Graphics2D) canvas.create();
        effect.translate(Math.round(shot.x), Math.round(shot.y));
        effect.scale(shot.facing, 1);
        effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (shot.weapon == Weapon.GYRO_AXE) {
            effect.rotate(shot.age * 18);
            effect.drawImage(assets.weaponIcon(shot.weapon), -11, -11, 22, 22, null);
        } else {
            Weapon.Flight flight = shot.weapon.flight();
            effect.rotate(Math.atan2(shot.age * flight.gravity() - flight.lift(), flight.speed()));
            boolean ember = shot.weapon == Weapon.CINDER_STAFF;
            int r = flight.radius();
            effect.setColor(ember ? BossView.AMBER : teamColor);
            effect.fillRect(-r - 9, -2, 12, 4);
            effect.fillRect(-r, -r, r * 2, r * 2);
            effect.setColor(Color.WHITE);
            effect.fillRect(0, -2, r, 4);
        }
        effect.dispose();
    }

    void drawImpact(Graphics2D canvas, Player player) {
        Player.Impact impact = player.visibleImpact();
        if (impact == null) {
            return;
        }

        BufferedImage strip = assets.impactStrip(impact.heavy());
        if (strip == null) {
            return;
        }
        int size = impact.heavy() ? 96 : 64;
        Graphics2D effect = (Graphics2D) canvas.create();
        effect.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        effect.translate(impact.x(), impact.y());
        drawFrame(effect, strip, player.impactProgress(), -size / 2, -size / 2, size, size);
        effect.dispose();
    }

    private void drawFrame(Graphics2D canvas, BufferedImage strip, double progress,
                           int x, int y, int width, int height) {
        int frameCount = strip.getWidth() / FRAME_SIZE;
        int frame = Math.min((int) (progress * frameCount), frameCount - 1);
        int frameX = frame * FRAME_SIZE;
        canvas.drawImage(strip, x, y, x + width, y + height,
            frameX, 0, frameX + FRAME_SIZE, FRAME_SIZE, null);
    }

    private void drawFallbackSweep(Graphics2D canvas, int range, int halfAngle,
                                   double progress, Color color) {
        double start = halfAngle - progress * halfAngle * 2;
        double extent = Math.min(35, start + halfAngle);
        canvas.setColor(color);
        canvas.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.draw(new Arc2D.Double(-range, -range, range * 2, range * 2,
            start, -extent, Arc2D.OPEN));
    }
}
