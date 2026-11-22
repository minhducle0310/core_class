import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Polygon;
import java.awt.image.BufferedImage;

final class ForestArt {
    static final String FOREST = "Legacy-Fantasy - High Forest 2.3/";
    static final String SWORDS = "Tiny Swords (Free Pack)/Tiny Swords (Free Pack)/";
    private static final BufferedImage SKY = GameAssets.load(FOREST + "Background/Background.png");
    private static final BufferedImage RIDGE = GameAssets.load(FOREST + "Trees/Background.png");
    private static final BufferedImage TILES = GameAssets.load(FOREST + "Assets/Tiles.png");
    private static final BufferedImage PINE = GameAssets.load(FOREST + "Trees/Green-Tree.png");
    private static final BufferedImage ROCK = GameAssets.load(SWORDS + "Terrain/Decorations/Rocks/Rock2.png");
    private static final BufferedImage BUSH = GameAssets.load(SWORDS + "Terrain/Decorations/Bushes/Bushe1.png");

    private ForestArt() {}

    static void worldTransform(Graphics2D g) {
        g.translate(Arena.LEFT, Arena.TOP);
        g.scale(Arena.WIDTH / (double) Arena.FOREST_WIDTH, Arena.HEIGHT / (double) Arena.FOREST_HEIGHT);
        g.translate(-Arena.LEFT, -Arena.TOP);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    static void tile(Graphics2D g, BufferedImage img, int sx, int sy, int sw, int sh,
                     int x, int y, int w, int h) {
        if (img != null) g.drawImage(img, x, y, x+w, y+h, sx, sy, sx+sw, sy+sh, null);
    }

    static void draw(Graphics2D g, Arena arena) {
        int left = Arena.LEFT, top = Arena.TOP, floor = arena.floor;
        g.setColor(new Color(116, 170, 165));
        g.fillRect(left, top, arena.width, Arena.FOREST_HEIGHT);
        tile(g, SKY, 0, 0, 480, 272, left, top, arena.width, Arena.FOREST_HEIGHT);
        for (int x = left; x < left + arena.width; x += 590) {
            tile(g, RIDGE, 704, 17, 96, 238, x, top+108, 295, 540);
            tile(g, RIDGE, 704, 17, 96, 238, x+590, top+108, -295, 540);
        }
        for (int x = left-83; x < left + arena.width; x += 364) {
            tile(g, RIDGE, 0, 0, 112, 255, x, floor-305, 182, 345);
            tile(g, RIDGE, 0, 0, 112, 255, x+364, floor-305, -182, 345);
        }
        for (int x : new int[]{25, 157, 412, 735, 945, 1051}) {
            int height = 194 + x % 105;
            tile(g, PINE, 0, 0, 108, 190, x - 69, floor-height, 138, height);
        }
        g.setColor(new Color(24, 38, 54, 68));
        g.fillRect(left, top, arena.width, Arena.FOREST_HEIGHT);
        int index = 0;
        for (Arena.Platform p : arena.platforms) {
            if (index == 1 || index == 5) bridge(g, p);
            else island(g, p.left(), p.right(), p.y(), index % 3);
            if (index % 3 == 0) {
                tile(g, BUSH, 0, 0, 128, 128, p.left()+5, p.y()-46, 64, 64);
                tile(g, ROCK, 0, 0, 64, 64, p.right()-46, p.y()-33, 40, 38);
            } else if (index % 3 == 2) {
                tile(g, TILES, 241, 240, 49, 31, p.left()+12, p.y()-24, 41, 26);
            }
            index++;
        }
        island(g, left, left + arena.width, floor, 1);
    }

    private static void island(Graphics2D g, int left, int right, int y, int variant) {
        int length = right-left;
        Polygon edge = new Polygon();
        edge.addPoint(left, y);
        edge.addPoint(right, y);
        edge.addPoint(right-3, y+17);
        for (int x=right-12; x>left+10; x-=12) {
            int depth = Math.min(48, Math.min(x-left,right-x)) + Math.floorMod(x*7+variant,13);
            edge.addPoint(x, y+12+depth);
        }
        edge.addPoint(left+2,y+18);
        Graphics2D earth=(Graphics2D)g.create();
        earth.clip(edge);
        for (int x=left; x<right; x+=72) {
            tile(earth, TILES, 2, 12, 73, 64, x, y, 72, 76);
        }
        earth.dispose();
        if (variant == 2 && length < 400) {
            tile(g, TILES, 190, 103, 34, 44, left+length*2/3, y+49, 38, 48);
        }
    }

    private static void bridge(Graphics2D g, Arena.Platform p) {
        for (int x=p.left(); x<p.right(); x+=60) {
            int width=Math.min(60, p.right()-x);
            tile(g, TILES, 81, 115, 78, 44, x, p.y(), width, 39);
            tile(g, TILES, 80, 91, 48, 10, x, p.y(), width, 9);
        }
    }
}
