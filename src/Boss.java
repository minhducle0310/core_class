import java.util.ArrayList;
import java.util.List;

final class Boss extends RaidBoss {
    String name() { return "Foreman B.O.L.T."; }
    int maxHealth() { return MAX_HEALTH; }
    String status() { return exposed() ? "Core exposed" : "Armored"; }

    static final int MAX_HEALTH = 520;
    static final int X = Arena.CENTER_X;
    static final int Y = Arena.FLOOR_Y - 35;
    static final int MUZZLE_Y = Arena.FLOOR_Y - 55;
    static final int LOW_CARD_Y = Arena.FLOOR_Y - 20;
    static final int HIGH_CARD_Y = Arena.FLOOR_Y - 120;
    private static final double IMPACT_SECONDS = 0.32;
    private static final double RECOIL_SECONDS = 0.20;
    private static final double SHUTDOWN_SECONDS = 0.78;
    private static final int COLUMN_RADIUS = 44;
    private static final int MAX_INTERNS = 2;
    private static final int SURGE_SPEED = 800;
    private static final int PAPER_SPEED = 520;
    private static final Pattern[] PHASE_ONE = {Pattern.FLOOR_SURGE, Pattern.FIRE_BOLT};
    private static final Pattern[] PHASE_TWO = {
        Pattern.PUNCH_CLOCK, Pattern.FIRE_BOLT, Pattern.PAPER_JAM, Pattern.CORE_BEAM, Pattern.FLOOR_SURGE
    };
    private static final Pattern[] PHASE_THREE = {
        Pattern.POLARITY_LOW, Pattern.FIRE_BOLT, Pattern.PAPER_JAM, Pattern.PUNCH_CLOCK
    };
    private static final PaperShot[] PAPER_SHOTS = {
        new PaperShot(0, true, LOW_CARD_Y),
        new PaperShot(0.23, false, HIGH_CARD_Y),
        new PaperShot(0.54, false, LOW_CARD_Y),
        new PaperShot(0.82, true, HIGH_CARD_Y)
    };

    private record PaperShot(double delay, boolean fromLeft, int y) {}

    enum Pattern {
        FLOOR_SURGE(0.76, 27),
        PUNCH_CLOCK(0.69, 25),
        PUNCH_ECHO(0.61, 24),
        PAPER_JAM(0.80, 18),
        CORE_BEAM(0.95, 32),
        GRID_OVERLOAD(0.72, 16),
        FIRE_BOLT(0.70, 19),
        FORKED_FLARE(0.58, 21),
        POLARITY_LOW(0.65, 30),
        POLARITY_HIGH(0.62, 36);

        final double windup;
        final int damage;

        Pattern(double windup, int damage) {
            this.windup = windup;
            this.damage = damage;
        }

        boolean usesColumns() {
            return this == PUNCH_CLOCK || this == PUNCH_ECHO || this == GRID_OVERLOAD;
        }

        boolean travels() {
            return this == FLOOR_SURGE || this == POLARITY_LOW || this == PAPER_JAM || firesBolts();
        }

        boolean firesBolts() {
            return this == FIRE_BOLT || this == FORKED_FLARE;
        }
    }


    record Strike(Pattern pattern, List<Zone> zones, List<Double> angles) {
        boolean hits(Player player) {
            if (player.health() == 0) {
                return false;
            }
            for (Zone zone : zones) {
                if (zone.contains(player)) {
                    return true;
                }
            }
            return false;
        }
    }

    private int health = MAX_HEALTH;
    private int facingX = 1;
    private int patternPhase = 1;
    private int patternIndex;
    private int regularTurns;
    private int gridPulse;
    private double warningDuration;
    private final List<RaidIntern> interns = new ArrayList<>(MAX_INTERNS);
    private final List<RaidHazard> hazards = new ArrayList<>(4);
    private boolean nextInternOnLeft = true;
    private int reinforcedCycles;
    private boolean venting;
    private double untilEvent = 0.9;
    private double ventDuration;
    private double impactRemaining;
    private double recoilRemaining;
    private double hitFlashRemaining;
    private double animationSeconds;
    private double shutdownAge;
    private Strike warning;
    private Strike impact;
    private Pattern movingPattern;
    private double movingTime;
    private int paperShotIndex;
    private boolean blueHitByMovingAttack;
    private boolean redHitByMovingAttack;

    void takeHit(Attack attack) {
        if (health == 0 || attack == null) {
            return;
        }
        for (RaidIntern intern : interns) {
            intern.takeHit(attack);
        }
        if (!hitsBody(attack)) {
            return;
        }
        hitArmor(attack.weapon().damage());
    }

    boolean hitBy(Projectile shot) {
        if (health == 0) return false;
        double first = Math.min(shot.contact(X, Y, 31), shot.contact(X, Arena.FLOOR_Y - 68, 25));
        RaidIntern firstIntern = null;
        for (RaidIntern intern : interns) {
            if (intern.health() == 0 || intern.state() == RaidIntern.State.ARRIVING) continue;
            double contact = shot.contact(intern.x(), RaidIntern.Y, 14);
            if (contact < first) {
                first = contact;
                firstIntern = intern;
            }
        }
        if (!Double.isFinite(first)) return false;
        if (firstIntern == null) hitArmor(shot.weapon.damage());
        else firstIntern.takeDamage(shot.weapon.damage());
        return true;
    }

    private void hitArmor(int damage) {
        hitFlashRemaining = 0.10;
        if (venting) damageCore(damage);
    }

    private void damageCore(int damage) {
        hitFlashRemaining = 0.10;
        health = Math.max(0, health - damage);
        if (health == 0) {
            venting = false;
            warning = null;
            impact = null;
            interns.clear();
            clearMovingAttack();
        }
    }

    private boolean hitsBody(Attack attack) {
        return attack.hitsAt(X, Y) || attack.hitsAt(X, Arena.FLOOR_Y - 72)
            || attack.hitsAt(X - 24, Arena.FLOOR_Y - 20)
            || attack.hitsAt(X + 24, Arena.FLOOR_Y - 20);
    }

    void updateEffects(double seconds) {
        animationSeconds += seconds;
        if (health == 0) {
            shutdownAge += seconds;
        }
        hitFlashRemaining = Math.max(0, hitFlashRemaining - seconds);
        recoilRemaining = Math.max(0, recoilRemaining - seconds);
        impactRemaining = Math.max(0, impactRemaining - seconds);
        if (impactRemaining == 0) {
            impact = null;
        }
    }

    void update(double seconds, Player blue, Player red, Arena arena) {
        interns.removeIf(intern -> intern.health() == 0);
        for (RaidIntern intern : interns) {
            intern.update(seconds, blue, red, arena);
        }
        if (blue.health() == 0 && red.health() == 0) {
            warning = null;
            clearMovingAttack();
            return;
        }
        if (movingPattern != null) {
            updateMovingAttack(seconds, blue, red);
            return;
        }
        untilEvent -= seconds;
        if (untilEvent > 0) {
            return;
        }

        if (warning == null) {
            venting = false;
            Pattern next;
            if (regularTurns == 2) {
                regularTurns = 0;
                gridPulse = 0;
                next = Pattern.GRID_OVERLOAD;
            } else {
                regularTurns++;
                next = nextPattern();
            }
            facingX = -facingX;
            beginWarning(next, blue, red);
            return;
        }

        Strike strike = warning;
        warning = null;
        if (strike.pattern().travels()) {
            startMovingAttack(strike);
            return;
        }

        impact = strike;
        impactRemaining = IMPACT_SECONDS;
        recoilRemaining = RECOIL_SECONDS;
        hitPlayer(impact, blue);
        hitPlayer(impact, red);
        if (blue.health() == 0 && red.health() == 0) {
            return;
        }
        finishAttack(impact.pattern(), blue, red);
    }

    private void startMovingAttack(Strike strike) {
        Pattern pattern = strike.pattern();
        movingPattern = pattern;
        movingTime = 0;
        paperShotIndex = 0;
        blueHitByMovingAttack = false;
        redHitByMovingAttack = false;
        if (pattern.firesBolts()) {
            for (double angle : strike.angles()) {
                hazards.add(new RaidHazard(RaidHazard.Kind.BOLT, X, MUZZLE_Y,
                    365 * Math.cos(angle), 365 * Math.sin(angle)));
            }
            recoilRemaining = RECOIL_SECONDS;
        } else if (pattern == Pattern.PAPER_JAM) {
            launchDuePaper();
        } else {
            recoilRemaining = RECOIL_SECONDS;
            int y = Arena.FLOOR_Y - RaidHazard.Kind.SURGE.halfHeight;
            hazards.add(new RaidHazard(RaidHazard.Kind.SURGE, X, y, -SURGE_SPEED));
            hazards.add(new RaidHazard(RaidHazard.Kind.SURGE, X, y, SURGE_SPEED));
        }
    }

    private void updateMovingAttack(double seconds, Player blue, Player red) {
        int returnedDamage = 0;
        for (var iterator = hazards.iterator(); iterator.hasNext();) {
            RaidHazard hazard = iterator.next();
            hazard.advance(seconds);
            if (hazard.kind() == RaidHazard.Kind.BOLT) {
                if (hazard.returning()) {
                    if (hazard.reachedCore()) {
                        returnedDamage += 18;
                        iterator.remove();
                    }
                } else {
                    Player target = hazard.firstContact(blue, red);
                    if (target != null) {
                        if (target.isDashing()) hazard.reflect();
                        else {
                            target.takeHit(movingPattern.damage);
                            iterator.remove();
                        }
                    }
                }
                continue;
            }
            if (!blueHitByMovingAttack && hazard.hits(blue)) {
                blue.takeHit(movingPattern.damage);
                blueHitByMovingAttack = true;
            }
            if (!redHitByMovingAttack && hazard.hits(red)) {
                red.takeHit(movingPattern.damage);
                redHitByMovingAttack = true;
            }
        }
        hazards.removeIf(RaidHazard::outsideArena);
        if (returnedDamage > 0) {
            damageCore(returnedDamage);
            if (health == 0) return;
        }
        if (blue.health() == 0 && red.health() == 0) {
            clearMovingAttack();
            return;
        }

        movingTime += seconds;
        if (movingPattern == Pattern.PAPER_JAM) {
            launchDuePaper();
        }
        if (hazards.isEmpty()
                && (movingPattern != Pattern.PAPER_JAM || paperShotIndex == PAPER_SHOTS.length)) {
            Pattern completed = movingPattern;
            clearMovingAttack();
            finishAttack(completed, blue, red);
        }
    }

    private void launchDuePaper() {
        while (paperShotIndex < PAPER_SHOTS.length && PAPER_SHOTS[paperShotIndex].delay() <= movingTime) {
            PaperShot shot = PAPER_SHOTS[paperShotIndex++];
            int start = shot.fromLeft() ? Arena.LEFT : Arena.LEFT + Arena.WIDTH;
            int velocity = shot.fromLeft() ? PAPER_SPEED : -PAPER_SPEED;
            hazards.add(new RaidHazard(RaidHazard.Kind.CARD, start, shot.y(), velocity));
            recoilRemaining = RECOIL_SECONDS;
        }
    }

    private void clearMovingAttack() {
        hazards.clear();
        movingPattern = null;
    }

    private void finishAttack(Pattern completed, Player blue, Player red) {
        Pattern followUp = switch (completed) {
            case GRID_OVERLOAD -> ++gridPulse < 3 + phase() ? Pattern.GRID_OVERLOAD : null;
            case FORKED_FLARE -> phase() == 3 ? Pattern.CORE_BEAM : null;
            case FIRE_BOLT -> phase() >= 2 ? Pattern.FORKED_FLARE : null;
            case POLARITY_LOW -> Pattern.POLARITY_HIGH;
            case PUNCH_CLOCK -> Pattern.PUNCH_ECHO;
            default -> null;
        };
        if (followUp != null) {
            beginWarning(followUp, blue, red);
        } else {
            venting = true;
            ventDuration = (completed == Pattern.FLOOR_SURGE ? 1.04 : 1.20) - 0.10 * (phase() - 1);
            untilEvent = ventDuration;
            deployIntern();
        }
    }

    private void deployIntern() {
        if (phase() < 2 || reinforcedCycles++ % 2 != 0 || interns.size() >= MAX_INTERNS) {
            return;
        }
        int offset = Arena.WIDTH / 9;
        int x = nextInternOnLeft ? Arena.LEFT + offset : Arena.LEFT + Arena.WIDTH - offset;
        interns.add(new RaidIntern(x, nextInternOnLeft ? 1 : -1));
        nextInternOnLeft = !nextInternOnLeft;
    }

    private Pattern nextPattern() {
        int phase = phase();
        if (phase != patternPhase) {
            patternPhase = phase;
            patternIndex = 0;
        }
        Pattern[] patterns = switch (phase) {
            case 1 -> PHASE_ONE;
            case 2 -> PHASE_TWO;
            default -> PHASE_THREE;
        };
        Pattern next = patterns[patternIndex];
        patternIndex = (patternIndex + 1) % patterns.length;
        return next;
    }

    private void beginWarning(Pattern pattern, Player blue, Player red) {
        List<Double> angles = List.of();
        if (pattern.firesBolts()) {
            double blueDistance = Math.hypot(blue.x() - X, blue.y() - MUZZLE_Y);
            double redDistance = Math.hypot(red.x() - X, red.y() - MUZZLE_Y);
            Player target = blue.health() > 0 && (red.health() == 0 || blueDistance <= redDistance) ? blue : red;
            double aim = Math.atan2(target.y() - MUZZLE_Y, target.x() - X);
            facingX = target.x() < X ? -1 : 1;
            angles = pattern == Pattern.FIRE_BOLT ? List.of(aim) : List.of(aim - 0.26, aim + 0.26);
        }
        List<Zone> zones = switch (pattern) {
            case GRID_OVERLOAD -> gridLanes();
            case FIRE_BOLT, FORKED_FLARE -> List.of();
            case FLOOR_SURGE, POLARITY_LOW ->
                List.of(new Zone(Arena.LEFT + 1, Arena.FLOOR_Y - 46, Arena.WIDTH - 2, 46));
            case POLARITY_HIGH ->
                List.of(new Zone(Arena.LEFT + 1, Arena.TOP + 1, Arena.WIDTH - 2,
                    Arena.FLOOR_Y - Arena.TOP - 55));
            case PUNCH_CLOCK, PUNCH_ECHO ->
                lockedColumns(blue, red, pattern == Pattern.PUNCH_CLOCK ? COLUMN_RADIUS : 36);
            case PAPER_JAM -> List.of(paperLane(LOW_CARD_Y), paperLane(HIGH_CARD_Y));
            case CORE_BEAM -> List.of(new Zone(Arena.LEFT + 1, Arena.FLOOR_Y - 72,
                Arena.WIDTH - 2, 35));
        };
        warning = new Strike(pattern, zones, angles);
        warningDuration = pattern == Pattern.GRID_OVERLOAD && gridPulse > 0
            ? 0.57 - 0.055 * (phase() - 1) : pattern.windup;
        untilEvent = warningDuration;
    }

    private List<Zone> gridLanes() {
        List<Zone> lanes = new ArrayList<>(4);
        for (int lane = gridPulse % 2; lane < 8; lane += 2) {
            int left = Arena.LEFT + lane * Arena.WIDTH / 8;
            int right = Arena.LEFT + (lane + 1) * Arena.WIDTH / 8;
            lanes.add(new Zone(left, Arena.TOP, right - left, Arena.FLOOR_Y - Arena.TOP));
        }
        return lanes;
    }

    private Zone paperLane(int y) {
        int halfHeight = RaidHazard.Kind.CARD.halfHeight;
        return new Zone(Arena.LEFT + 1, y - halfHeight, Arena.WIDTH - 2, halfHeight * 2);
    }

    private List<Zone> lockedColumns(Player blue, Player red, int radius) {
        if (blue.health() == 0) {
            return List.of(columnAt(red.x(), radius));
        }
        if (red.health() == 0) {
            return List.of(columnAt(blue.x(), radius));
        }
        return List.of(columnAt(blue.x(), radius), columnAt(red.x(), radius));
    }

    private Zone columnAt(double x, int radius) {
        return new Zone((int) Math.round(x) - radius, Arena.TOP + 1,
            radius * 2, Arena.FLOOR_Y - Arena.TOP - 1);
    }

    private void hitPlayer(Strike strike, Player player) {
        if (strike.hits(player)) {
            player.takeHit(strike.pattern().damage);
        }
    }

    int health() {
        return health;
    }

    int phase() {
        if (health * 3 <= MAX_HEALTH) {
            return 3;
        }
        return health * 3 <= MAX_HEALTH * 2 ? 2 : 1;
    }

    int facingX() {
        return facingX;
    }

    boolean exposed() {
        return venting;
    }

    boolean flashing() {
        return hitFlashRemaining > 0;
    }

    Strike warning() {
        return warning;
    }

    Strike impact() {
        return impact;
    }

    Pattern movingPattern() {
        return movingPattern;
    }

    Iterable<RaidHazard> hazards() {
        return hazards;
    }

    Iterable<RaidIntern> interns() {
        return interns;
    }

    double animationSeconds() {
        return animationSeconds;
    }

    double warningProgress() {
        return warning == null ? 0 : 1 - untilEvent / warningDuration;
    }

    double impactStrength() {
        return impactRemaining / IMPACT_SECONDS;
    }

    double recoilStrength() {
        return recoilRemaining / RECOIL_SECONDS;
    }

    double ventProgress() {
        return !venting ? 0 : 1 - untilEvent / ventDuration;
    }

    double shutdownProgress() {
        return Math.min(1, shutdownAge / SHUTDOWN_SECONDS);
    }
}
