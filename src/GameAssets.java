import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

final class GameAssets {
    record BossSprites(BufferedImage foreman, BufferedImage intern) {}

    private static final String FX_FOLDER = "tiny-pixel-fx-vol1/tiny-pixel-fx-vol1/sheets_32px/";
    private final Map<Weapon, BufferedImage> weaponIcons = new HashMap<>();
    private final Map<Weapon, BufferedImage> swingStrips = new HashMap<>();
    private final BufferedImage hitSpark = load(FX_FOLDER + "hit_spark_mono.png");
    private final BufferedImage hammerImpact = load(FX_FOLDER + "impact_ring_arcane.png");
    private final BufferedImage blueDashDust = load(FX_FOLDER + "dash_dust_frost.png");
    private final BufferedImage redDashDust = load(FX_FOLDER + "dash_dust_ember.png");
    private BossSprites bossSprites;

    GameAssets() {
        weaponIcons.put(Weapon.SPARK_WAND, load("spark-wand.png"));
        weaponIcons.put(Weapon.CINDER_STAFF, load("cinder-staff.png"));
        weaponIcons.put(Weapon.GYRO_AXE, load("gyro-axe.png"));
        weaponIcons.put(Weapon.PULSE_BLADE, load("pulse-blade.png"));
        weaponIcons.put(Weapon.SHOCK_BATON, load("shock-baton.png"));
        weaponIcons.put(Weapon.GRAVITY_HAMMER, load("gravity-hammer.png"));
        swingStrips.put(Weapon.PULSE_BLADE, load(FX_FOLDER + "slash_arc_frost.png"));
        swingStrips.put(Weapon.SHOCK_BATON, load(FX_FOLDER + "zap_ember.png"));
        swingStrips.put(Weapon.GRAVITY_HAMMER, load(FX_FOLDER + "slash_arc_arcane.png"));
    }

    BufferedImage weaponIcon(Weapon weapon) {
        return weaponIcons.get(weapon);
    }

    BufferedImage swingStrip(Weapon weapon) {
        return swingStrips.get(weapon);
    }

    BufferedImage impactStrip(boolean heavy) {
        return heavy ? hammerImpact : hitSpark;
    }

    BufferedImage dashStrip(boolean blue) {
        return blue ? blueDashDust : redDashDust;
    }

    BossSprites bossSprites() {
        if (bossSprites == null) {
            BufferedImage sheet = load("boss/foreman-sheet.png");
            bossSprites = new BossSprites(pixelSprite(sheet, 48, 0, false), pixelSprite(sheet, 0, 48, true));
        }
        return bossSprites;
    }

    private static BufferedImage pixelSprite(BufferedImage sheet, int left, int top, boolean internPalette) {
        int cell = 16;
        if (sheet == null || sheet.getWidth() < left + cell || sheet.getHeight() < top + cell) {
            return null;
        }
        BufferedImage sprite = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cell; y++) {
            for (int x = 0; x < cell; x++) {
                int color = sheet.getRGB(left + x, top + y);
                // The original sheet uses a solid pale green backdrop.
                if (color != 0xFFE0F0E8) {
                    if (internPalette) {
                        color = switch (color) {
                            case 0xFF204631 -> 0xFF56392D;
                            case 0xFF527F39 -> 0xFFC27C42;
                            case 0xFFAEC440 -> 0xFFFFBD55;
                            case 0xFFD7E894 -> 0xFFFFEBB0;
                            default -> color;
                        };
                    }
                    sprite.setRGB(x, y, color);
                }
            }
        }
        return sprite;
    }

    static BufferedImage load(String file) {
        Path path = Path.of("assets").resolve(file);
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IOException("Unsupported image format");
            }
            return image;
        } catch (IOException exception) {
            System.err.println("Could not load image " + path + " (" + exception.getMessage() + ")");
            return null;
        }
    }
}
