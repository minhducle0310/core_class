record Weapon(String name, int damage, int range, int halfAngleDegrees,
              double cooldownSeconds, double swingSeconds, Flight flight) {
    record Flight(double speed, double lift, double gravity, int radius) {}

    Weapon(String name, int damage, int range, int halfAngleDegrees,
           double cooldownSeconds, double swingSeconds) {
        this(name, damage, range, halfAngleDegrees, cooldownSeconds, swingSeconds, null);
    }

    static final Weapon PULSE_BLADE = new Weapon("Pulse Blade", 20, 72, 50, 0.45, 0.20);
    static final Weapon SHOCK_BATON = new Weapon("Shock Baton", 15, 54, 40, 0.25, 0.14);
    static final Weapon GRAVITY_HAMMER = new Weapon("Gravity Hammer", 30, 96, 65, 0.8, 0.24);
    static final Weapon SPARK_WAND = new Weapon("Spark Wand", 10, 490, 0, 0.36, 0.12,
        new Flight(560, 0, 0, 3));
    static final Weapon CROSS_BEAM = new Weapon("Cross Beam", 26, 245, 0, 0.48, 0.18);
    static final Weapon CINDER_STAFF = new Weapon("Cinder Staff", 24, 400, 0, 0.75, 0.20,
        new Flight(335, 200, 360, 6));
    static final Weapon GYRO_AXE = new Weapon("Gyro Axe", 28, 235, 0, 0.88, 0.22,
        new Flight(320, 85, 240, 9));
}
