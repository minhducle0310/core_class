import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class Arena {
    record SupplySpot(int x, int surfaceY) {}

    record Platform(int left, int right, int y) {
        boolean supports(double x) {
            return x + Player.RADIUS > left && x - Player.RADIUS < right;
        }
    }

    static final int LEFT = 20;
    static final int TOP = 62;
    static final int WIDTH = 728;
    static final int HEIGHT = 428;
    static final int CENTER_X = LEFT + WIDTH / 2;
    static final int FLOOR_Y = TOP + HEIGHT - 18;
    static final int UPPER_PLATFORM_Y = FLOOR_Y - 174;
    static final int SIDE_PLATFORM_Y = FLOOR_Y - 104;
    static final int STEP_Y = FLOOR_Y - 54;
    static final int BALCONY_Y = FLOOR_Y - 210;
    static final List<Platform> PLATFORMS = List.of(
        new Platform(82, 246, SIDE_PLATFORM_Y),
        new Platform(306, 462, UPPER_PLATFORM_Y),
        new Platform(522, 686, SIDE_PLATFORM_Y),
        new Platform(252, 316, STEP_Y),
        new Platform(452, 516, STEP_Y),
        new Platform(128, 218, BALCONY_Y),
        new Platform(550, 640, BALCONY_Y)
    );
    private static final List<SupplySpot> SUPPLY_SPOTS = List.of(
        new SupplySpot(LEFT + 94, FLOOR_Y),
        new SupplySpot(LEFT + WIDTH - 94, FLOOR_Y),
        new SupplySpot(164, SIDE_PLATFORM_Y),
        new SupplySpot(604, SIDE_PLATFORM_Y),
        new SupplySpot(284, STEP_Y),
        new SupplySpot(484, STEP_Y),
        new SupplySpot(173, BALCONY_Y),
        new SupplySpot(595, BALCONY_Y),
        new SupplySpot(CENTER_X - 46, UPPER_PLATFORM_Y),
        new SupplySpot(CENTER_X, UPPER_PLATFORM_Y),
        new SupplySpot(CENTER_X + 46, UPPER_PLATFORM_Y)
    );

    static final int FOREST_WIDTH = WIDTH * 3 / 2;
    static final int FOREST_HEIGHT = HEIGHT * 3 / 2;
    final int width;
    final int floor;
    final List<Platform> platforms;
    private final List<SupplySpot> spots;

    Arena() { this(false); }

    Arena(boolean forest) {
        width = forest ? FOREST_WIDTH : WIDTH;
        floor = forest ? TOP + FOREST_HEIGHT - 28 : FLOOR_Y;
        platforms = forest ? List.of(
            new Platform(55, 343, floor - 105), new Platform(448, 666, floor - 63),
            new Platform(780, 1072, floor - 115), new Platform(210, 475, floor - 214),
            new Platform(663, 922, floor - 228), new Platform(404, 700, floor - 325),
            new Platform(65, 256, floor - 345), new Platform(863, 1072, floor - 365),
            new Platform(525, 794, floor - 439)) : PLATFORMS;
        spots = forest ? platforms.stream().map(p -> new SupplySpot((p.left + p.right) / 2, p.y)).toList()
            : SUPPLY_SPOTS;
    }

    SupplySpot randomSupplySpot(SupplySpot previous) {
        int previousIndex = previous == null ? -1 : spots.indexOf(previous);

        int index = ThreadLocalRandom.current().nextInt(spots.size() - (previousIndex < 0 ? 0 : 1));
        return spots.get(previousIndex >= 0 && index >= previousIndex ? index + 1 : index);
    }

    double clampX(double x, int radius) {
        return Math.max(LEFT + radius, Math.min(x, LEFT + width - radius));
    }

    double landingHeight(double startFeet, double endFeet, double x) {
        double landing = endFeet >= floor ? floor : Double.POSITIVE_INFINITY;
        for (Platform platform : platforms) {
            if (platform.supports(x) && startFeet <= platform.y() && endFeet >= platform.y()) {
                landing = Math.min(landing, platform.y());
            }
        }
        return landing;
    }

    double spawnX(boolean leftSide) {
        int distanceFromLeft = leftSide ? width / 4 : width * 3 / 4;
        return LEFT + distanceFromLeft;
    }

    double spawnY() {
        return floor - Player.RADIUS;
    }
}
