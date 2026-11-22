final class RaidIntern {
    enum State {
        ARRIVING, SEEKING, WINDUP, CHARGING, RECOVERING
    }

    static final int MAX_HEALTH = 34;
    static final int Y = Arena.FLOOR_Y - 12;
    static final int CHARGE_DISTANCE = 100;
    private static final double ARRIVAL_SECONDS = 0.52;
    private static final double WINDUP_SECONDS = 0.44;
    private static final double CHARGE_SECONDS = 0.29;
    private static final double SEEK_SPEED = 100;
    private static final int CHARGE_DAMAGE = 12;

    private double x;
    private int facing;
    private int health = MAX_HEALTH;
    private State state = State.ARRIVING;
    private double untilNext = ARRIVAL_SECONDS;
    private boolean hitBlue;
    private boolean hitRed;

    RaidIntern(double x, int facing) {
        this.x = x;
        this.facing = facing;
    }

    void takeHit(Attack attack) {
        if (state != State.ARRIVING && attack != null && attack.hitsAt(x, Y)) {
            takeDamage(attack.weapon().damage());
        }
    }

    void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    void update(double seconds, Player blue, Player red, Arena arena) {
        double stepSeconds = Math.min(untilNext, seconds);
        untilNext = Math.max(0, untilNext - seconds);
        switch (state) {
            case ARRIVING, RECOVERING -> {
                if (untilNext == 0) {
                    state = State.SEEKING;
                }
            }
            case SEEKING -> seek(blue, red, seconds, arena);
            case WINDUP -> {
                if (untilNext == 0) {
                    state = State.CHARGING;
                    untilNext = CHARGE_SECONDS;
                    hitBlue = false;
                    hitRed = false;
                }
            }
            case CHARGING -> {
                double from = x;
                x = arena.clampX(x + facing * CHARGE_DISTANCE * stepSeconds / CHARGE_SECONDS, 11);
                hitBlue = hitBlue || hitPlayer(blue, from);
                hitRed = hitRed || hitPlayer(red, from);
                if (untilNext == 0 || x == from) {
                    state = State.RECOVERING;
                    untilNext = 0.68;
                }
            }
        }
    }

    private void seek(Player blue, Player red, double seconds, Arena arena) {
        Player target = blue.health() > 0
            && (red.health() == 0 || Math.abs(blue.x() - x) <= Math.abs(red.x() - x)) ? blue : red;
        double offset = target.x() - x;
        if (offset != 0) {
            facing = offset > 0 ? 1 : -1;
        }
        if (Math.abs(offset) < CHARGE_DISTANCE - 10 && target.y() >= Arena.FLOOR_Y - 54) {
            state = State.WINDUP;
            untilNext = WINDUP_SECONDS;
        } else {
            double step = Math.min(Math.abs(offset), SEEK_SPEED * seconds);
            x = arena.clampX(x + facing * step, 11);
        }
    }

    private boolean hitPlayer(Player player, double from) {
        if (player.health() == 0 || player.y() < Arena.FLOOR_Y - 34
                || player.x() < Math.min(from, x) - 17 || player.x() > Math.max(from, x) + 17) {
            return false;
        }
        player.takeHit(CHARGE_DAMAGE);
        return true;
    }

    double x() {
        return x;
    }

    int facing() {
        return facing;
    }

    int health() {
        return health;
    }

    State state() {
        return state;
    }

    double arrivalProgress() {
        return 1 - untilNext / ARRIVAL_SECONDS;
    }

    double chargeWarningProgress() {
        return 1 - untilNext / WINDUP_SECONDS;
    }
}
