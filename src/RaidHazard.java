final class RaidHazard {
    enum Kind {
        SURGE(18, 23), CARD(14, 11), BOLT(7, 7);

        final int halfWidth;
        final int halfHeight;

        Kind(int halfWidth, int halfHeight) {
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
        }
    }

    private final Kind kind;
    private double y, previousY;
    private double velocityX, velocityY;
    private boolean returning;
    private double x;
    private double previousX;

    RaidHazard(Kind kind, double x, int y, double velocity) {
        this(kind, x, y, velocity, 0);
    }

    RaidHazard(Kind kind, double x, int y, double velocityX, double velocityY) {
        this.kind = kind;
        this.x = x;
        this.previousX = x;
        this.y = y;
        previousY = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    void advance(double seconds) {
        previousX = x;
        previousY = y;
        x += velocityX * seconds;
        y += velocityY * seconds;
    }

    private double contact(Player player) {
        if (player.health() == 0) return Double.POSITIVE_INFINITY;
        return Projectile.contact(previousX, previousY, x, y, player.x(), player.y(),
            Player.RADIUS + kind.halfWidth);
    }

    Player firstContact(Player blue, Player red) {
        double first = contact(blue), second = contact(red);
        if (!Double.isFinite(Math.min(first, second))) return null;
        return first <= second ? blue : red;
    }

    void reflect() {
        returning = true;
        double angle = Math.atan2(Boss.MUZZLE_Y - y, Boss.X - x);
        velocityX = 420 * Math.cos(angle);
        velocityY = 420 * Math.sin(angle);
    }

    boolean reachedCore() {
        return Double.isFinite(Projectile.contact(previousX, previousY, x, y, Boss.X, Boss.MUZZLE_Y, 12));
    }

    boolean returning() {
        return returning;
    }

    double angle() {
        return Math.atan2(velocityY, velocityX);
    }

    boolean hits(Player player) {
        return player.health() > 0
            && Math.abs(player.y() - y) <= kind.halfHeight
            && player.x() >= Math.min(previousX, x) - kind.halfWidth
            && player.x() <= Math.max(previousX, x) + kind.halfWidth;
    }

    boolean outsideArena() {
        return x + kind.halfWidth < Arena.LEFT || x - kind.halfWidth > Arena.LEFT + Arena.WIDTH
            || y - kind.halfHeight > Arena.FLOOR_Y || y + kind.halfHeight < Arena.TOP;
    }

    Kind kind() {
        return kind;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    int direction() {
        return velocityX > 0 ? 1 : -1;
    }
}
