final class Player {
    record Impact(double x, double y, boolean heavy) {}

    static final int RADIUS = 14;
    static final int MAX_HEALTH = 100;
    private static final double SPEED = 290.0;
    static final double RUN_CYCLE_DISTANCE = 60.0;
    private static final double GRAVITY = 1700.0;
    private static final double JUMP_SPEED = 680.0;
    private static final double WALL_SLIDE_SPEED = 160.0;
    private static final double WALL_JUMP_SPEED = 360.0;
    private static final double DASH_SPEED = 820.0;
    private static final double DASH_SECONDS = 0.144;
    private static final double DASH_COOLDOWN_SECONDS = 1.40;
    private static final double IMPACT_SECONDS = 0.24;

    private double x;
    private double y;
    private double facingX;
    private double verticalVelocity;
    private double wallJumpVelocity;
    private final boolean hasAirJump;
    private boolean airJumpSpent;
    private boolean grounded = true;
    private boolean running;
    private double runPhase;
    private int health = MAX_HEALTH;
    private Weapon weapon = Weapon.PULSE_BLADE;
    private double dashRemaining;
    private double dashCooldownRemaining;
    private double cooldownRemaining;
    private double swingRemaining;
    private Attack visibleAttack;
    private double impactRemaining;
    private Impact visibleImpact;

    Player(double x, double y, double facingX) {
        this(x, y, facingX, false);
    }

    Player(double x, double y, double facingX, boolean hasAirJump) {
        this.hasAirJump = hasAirJump;
        this.x = x;
        this.y = y;
        this.facingX = facingX;
    }

    void move(int horizontal, boolean jumpPressed, boolean dashPressed, double seconds, Arena arena) {
        boolean wasGrounded = grounded;
        double runDistance = moveHorizontally(horizontal, dashPressed, seconds, arena);
        int wall = wallContact(horizontal, arena);
        if (grounded) airJumpSpent = false;
        if (jumpPressed && (grounded || wall != 0 || (hasAirJump && !airJumpSpent))) {
            if (!grounded && wall == 0) airJumpSpent = true;
            verticalVelocity = -JUMP_SPEED;
            grounded = false;
            if (wall != 0 && !wasGrounded) {
                wallJumpVelocity = -wall * WALL_JUMP_SPEED;
                facingX = -wall;
            }
        } else if (!grounded && wall != 0 && verticalVelocity > WALL_SLIDE_SPEED) {
            verticalVelocity = WALL_SLIDE_SPEED;
        }
        applyGravity(seconds, arena);
        running = wasGrounded && grounded && runDistance > 0;
        runPhase = running ? (runPhase + runDistance / RUN_CYCLE_DISTANCE) % 1.0 : 0;
    }

    private double moveHorizontally(int horizontal, boolean dashPressed, double seconds, Arena arena) {
        dashCooldownRemaining = Math.max(0, dashCooldownRemaining - seconds);
        if (!grounded && wallJumpVelocity != 0) {
            x = arena.clampX(x + wallJumpVelocity * seconds, RADIUS);
            wallJumpVelocity *= Math.max(0, 1 - 5 * seconds);
            if (Math.abs(wallJumpVelocity) < 20) {
                wallJumpVelocity = 0;
            }
            return 0;
        }
        if (grounded) {
            wallJumpVelocity = 0;
        }

        if (dashPressed && !isDashing() && dashCooldownRemaining == 0) {
            if (horizontal != 0) {
                facingX = Math.signum(horizontal);
            }
            dashRemaining = DASH_SECONDS;
            dashCooldownRemaining = DASH_COOLDOWN_SECONDS;
        }

        if (isDashing()) {
            double dashTime = Math.min(dashRemaining, seconds);
            x = arena.clampX(x + facingX * DASH_SPEED * dashTime, RADIUS);
            dashRemaining -= dashTime;
            seconds -= dashTime;
        }

        if (horizontal != 0 && !isDashing()) {
            facingX = Math.signum(horizontal);
            double startX = x;
            x = arena.clampX(x + facingX * SPEED * seconds, RADIUS);
            return Math.abs(x - startX);
        }
        return 0;
    }

    private int wallContact(int horizontal, Arena arena) {
        if (!grounded && horizontal < 0 && x <= Arena.LEFT + RADIUS + 0.01) {
            return -1;
        }
        if (!grounded && horizontal > 0 && x >= Arena.LEFT + arena.width - RADIUS - 0.01) {
            return 1;
        }
        return 0;
    }

    private void applyGravity(double seconds, Arena arena) {
        double feet = y + RADIUS;
        double nextFeet = feet + verticalVelocity * seconds + 0.5 * GRAVITY * seconds * seconds;
        double landing = nextFeet >= feet
            ? arena.landingHeight(feet, nextFeet, x) : Double.POSITIVE_INFINITY;
        grounded = Double.isFinite(landing);
        if (grounded) {
            y = landing - RADIUS;
            verticalVelocity = 0;
        } else {
            y = nextFeet - RADIUS;
            verticalVelocity += GRAVITY * seconds;
            if (y < Arena.TOP + RADIUS) {
                y = Arena.TOP + RADIUS;
                verticalVelocity = Math.max(0, verticalVelocity);
            }
        }
    }

    boolean isGrounded() {
        return grounded;
    }

    boolean isRunning() {
        return running;
    }

    double runPhase() {
        return runPhase;
    }

    boolean isDashing() {
        return dashRemaining > 0;
    }

    double dashProgress() {
        return 1.0 - dashRemaining / DASH_SECONDS;
    }

    double dashCooldownRemaining() {
        return dashCooldownRemaining;
    }

    double dashCharge() {
        return 1.0 - dashCooldownRemaining / DASH_COOLDOWN_SECONDS;
    }

    void updateCombatTimers(double seconds) {
        cooldownRemaining = Math.max(0, cooldownRemaining - seconds);
        swingRemaining = Math.max(0, swingRemaining - seconds);
        if (swingRemaining == 0) {
            visibleAttack = null;
        }
        impactRemaining = Math.max(0, impactRemaining - seconds);
        if (impactRemaining == 0) {
            visibleImpact = null;
        }
    }

    Attack tryAttack() {
        if (cooldownRemaining > 0) {
            return null;
        }

        boolean brake = beamReady();
        Weapon technique = brake ? Weapon.CROSS_BEAM : weapon;
        if (brake) dashRemaining = 0;
        cooldownRemaining = technique.cooldownSeconds();
        swingRemaining = technique.swingSeconds();
        visibleAttack = new Attack(x, y, facingX, technique);
        return visibleAttack;
    }

    void equip(Weapon weapon) {
        this.weapon = weapon;
    }

    boolean beamReady() {
        return isDashing() && dashProgress() >= 0.35 && cooldownRemaining == 0;
    }

    Weapon weapon() {
        return weapon;
    }

    void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    void repair(int amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }

    void takeHit(Attack attack) {
        takeHit(attack.weapon().damage(), attack.weapon() == Weapon.GRAVITY_HAMMER);
    }

    void takeHit(int damage) {
        takeHit(damage, false);
    }

    private void takeHit(int damage, boolean heavy) {
        takeDamage(damage);
        visibleImpact = new Impact(x, y, heavy);
        impactRemaining = IMPACT_SECONDS;
    }

    int health() {
        return health;
    }

    Attack visibleAttack() {
        return visibleAttack;
    }

    double swingProgress() {
        return visibleAttack == null ? 1.0 : 1.0 - swingRemaining / visibleAttack.weapon().swingSeconds();
    }

    Impact visibleImpact() {
        return visibleImpact;
    }

    double impactProgress() {
        return 1.0 - impactRemaining / IMPACT_SECONDS;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double facingX() {
        return facingX;
    }
}
