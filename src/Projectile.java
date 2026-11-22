final class Projectile {
    final Player owner;
    final Weapon weapon;
    final double facing;
    private final double startX;
    double x, y, age;
    private double previousX, previousY;

    Projectile(Player owner, Attack attack) {
        this.owner = owner;
        weapon = attack.weapon();
        facing = attack.facingX();
        startX = attack.x();
        previousX = x = startX;
        previousY = y = attack.y();
    }

    void advance(double seconds) {
        Weapon.Flight flight = weapon.flight();
        double step = Math.min(seconds, Math.max(0, weapon.range() / flight.speed() - age));
        previousX = x;
        previousY = y;
        y += (flight.gravity() * (age + step / 2) - flight.lift()) * step;
        age += step;
        x = startX + facing * flight.speed() * age;
    }

    // First point on this tick's flight segment that enters a circular target.
    double contact(double targetX, double targetY, double radius) {
        return contact(previousX, previousY, x, y, targetX, targetY, radius + weapon.flight().radius());
    }

    static double contact(double fromX, double fromY, double toX, double toY,
                          double targetX, double targetY, double radius) {
        double dx = toX - fromX, dy = toY - fromY;
        double ox = fromX - targetX, oy = fromY - targetY;
        double gap = ox * ox + oy * oy - radius * radius;
        if (gap <= 0) return 0;
        double lengthSquared = dx * dx + dy * dy;
        double projection = -(ox * dx + oy * dy);
        double discriminant = projection * projection - lengthSquared * gap;
        if (lengthSquared == 0 || projection < 0 || discriminant < 0) return Double.POSITIVE_INFINITY;
        double entry = (projection - Math.sqrt(discriminant)) / lengthSquared;
        return entry <= 1 ? entry : Double.POSITIVE_INFINITY;
    }

    boolean hitOpponent(Player blue, Player red) {
        Player target = owner == blue ? red : blue;
        if (!Double.isFinite(contact(target.x(), target.y(), Player.RADIUS))) return false;
        target.takeHit(weapon.damage());
        return true;
    }

    boolean expired(Arena arena) {
        return age >= weapon.range() / weapon.flight().speed() || x < Arena.LEFT
            || x > Arena.LEFT + arena.width || y >= arena.floor || y < Arena.TOP;
    }
}
