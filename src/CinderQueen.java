import java.util.ArrayList;
import java.util.List;

final class CinderQueen extends RaidBoss {
    enum Wave { STINGER, FIRESTORM, PINWHEEL, COOLING }
    enum Kind { STING, METEOR, CINDER }

    static final class Flame {
        final Kind kind;
        double x, y, vx, vy, age;
        final double warning;
        boolean burst;

        Flame(Kind kind, double x, double y, double vx, double vy, double warning) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.warning = warning;
        }

        int radius() { return kind == Kind.METEOR ? 15 : 9; }

        boolean advance(double dt, Arena arena) {
            age += dt;
            if (burst) return age > 0.38;
            if (age < warning) return false;
            x += vx * dt;
            y += vy * dt;
            if (y >= arena.floor && vy > 0) {
                y = arena.floor;
                burst = true;
                age = 0;
            }
            return x < Arena.LEFT - 28 || x > Arena.LEFT + arena.width + 28 || y < Arena.TOP - 32;
        }

        boolean touches(Player p) {
            return !burst && age >= warning && Math.hypot(p.x() - x, p.y() - y) < radius() + 9;
        }
    }

    private static final int MAX_HP = 540;
    private final List<Flame> flames = new ArrayList<>();
    private Wave wave = Wave.STINGER;
    private int cycle, volley;
    private double clock, age, nextShot = 1.15, shutdown;
    private double x = Arena.LEFT + Arena.FOREST_WIDTH / 2.0, y = Arena.TOP + 195;
    private int facing = -1;
    private double blueGrace, redGrace, flash, lockedAim;
    private boolean aiming;
    private int hp = MAX_HP;

    String name() { return "Cinder Queen"; }
    int health() { return hp; }
    int maxHealth() { return MAX_HP; }
    int phase() { return hp > 360 ? 1 : hp > 180 ? 2 : 3; }
    boolean exposed() { return hp > 0 && wave == Wave.COOLING; }
    double x() { return x; }
    double y() { return y; }
    double clock() { return clock; }
    int facing() { return facing; }
    boolean flashing() { return flash > 0; }
    List<Flame> flames() { return flames; }
    Wave wave() { return wave; }
    boolean aiming() { return aiming && hp > 0; }
    double aim() { return lockedAim; }
    double shutdownProgress() { return Math.min(1, shutdown / 0.8); }

    String status() {
        return switch (wave) {
            case STINGER -> "Stinger fan - cross the spread";
            case FIRESTORM -> "Firestorm - leave marked lanes";
            case PINWHEEL -> "Pinwheel - slip between spokes";
            case COOLING -> "Overheated - double damage!";
        };
    }

    void updateEffects(double seconds) {
        flash = Math.max(0, flash - seconds);
        if (hp == 0) shutdown += seconds;
    }

    void update(double dt, Player blue, Player red, Arena arena) {
        clock += dt;
        age += dt;
        double center = Arena.LEFT + arena.width / 2.0;
        Player target = (volley % 2 == 0 && blue.health() > 0 || red.health() == 0) ? blue : red;
        double destX = center + (arena.width / 2 - 130) * Math.sin(clock * 0.63);
        double destY = Arena.TOP + 235 + 106 * Math.sin(clock * 1.29) + 55 * Math.cos(clock * .53);
        if (wave == Wave.COOLING) {
            destX = (blue.health() == 0 ? red.x() : red.health() == 0 ? blue.x() : (blue.x() + red.x()) / 2);
            destY = arena.floor - 98;
        }
        double ease = 1 - Math.exp(-dt * (exposed() ? 2.0 : 3.0));
        x += (destX - x) * ease;
        y += (destY - y) * ease;
        facing = target.x() < x ? -1 : 1;
        if (wave == Wave.STINGER && !aiming && nextShot - age <= .36) {
            lockedAim = Math.atan2(target.y() - y, target.x() - x);
            aiming = true;
        }

        flames.removeIf(flame -> flame.advance(dt, arena));
        blueGrace = hurt(blue, blueGrace - dt);
        redGrace = hurt(red, redGrace - dt);

        if (wave != Wave.COOLING && age >= nextShot) {
            fire(arena, target);
            aiming = false;
            volley++;
            nextShot += switch (wave) {
                case STINGER -> .86 - .10 * phase();
                case FIRESTORM -> 1.12 - .12 * phase();
                case PINWHEEL -> .51 - .055 * phase();
                default -> throw new IllegalStateException();
            };
        }
        if (age >= (wave == Wave.COOLING ? 2.35 : 4.4)) {
            wave = switch (wave) {
                case STINGER -> Wave.FIRESTORM;
                case FIRESTORM -> Wave.PINWHEEL;
                case PINWHEEL -> Wave.COOLING;
                case COOLING -> Wave.STINGER;
            };
            if (wave == Wave.STINGER) cycle++;
            age = 0;
            aiming = false;
            volley = 0;
            nextShot = .70;
        }
    }

    private void fire(Arena arena, Player target) {
        switch (wave) {
            case STINGER -> fan(lockedAim);
            case FIRESTORM -> {
                int spacing = 140 - 12 * phase();
                int shift = Math.floorMod(volley * 49 + cycle * 31, spacing);
                for (int lane = Arena.LEFT + shift; lane < Arena.LEFT + arena.width; lane += spacing) {
                    flames.add(new Flame(Kind.METEOR, lane, Arena.TOP + 16, 0, 300 + phase() * 38, .68));
                }
                if (phase() == 3 && volley % 2 == 1) fan(Math.atan2(target.y()-y,target.x()-x));
            }
            case PINWHEEL -> {
                int spokes = 4 + phase();
                for (int i = 0; i < spokes; i++) {
                    double angle = i * 2 * Math.PI / spokes + volley * .31 * (cycle % 2 == 0 ? 1 : -1);
                    launch(Kind.CINDER, angle, 162 + 16 * phase());
                }
            }
            default -> throw new IllegalStateException();
        }
    }

    private void fan(double aim) {
        int side = phase() == 1 ? 2 : 3;
        for (int i = -side; i <= side; i++) launch(Kind.STING, aim + i * .19, 253 + 28 * phase());
    }

    private void launch(Kind kind, double angle, double speed) {
        double dx = Math.cos(angle), dy = Math.sin(angle);
        flames.add(new Flame(kind, x + dx * 40, y + dy * 38, dx * speed, dy * speed, 0));
    }

    private double hurt(Player p, double grace) {
        if (grace > 0 || p.health() == 0 || p.isDashing()) return grace;
        for (Flame flame : flames) {
            if (flame.touches(p)) {
                p.takeHit(flame.kind == Kind.METEOR ? 20 : 12);
                return .78;
            }
        }
        return 0;
    }

    void takeHit(Attack attack) {
        if (attack == null || hp == 0) return;
        if (attack.hitsAt(x, y) || attack.hitsAt(x - 40, y) || attack.hitsAt(x + 40, y)
                || attack.hitsAt(x, y + 42) || attack.hitsAt(x, y - 38)) damage(attack.weapon().damage());
    }

    boolean hitBy(Projectile shot) {
        if (hp == 0 || !Double.isFinite(shot.contact(x, y, 53))) return false;
        damage(shot.weapon.damage());
        return true;
    }

    private void damage(int amount) {
        hp = Math.max(0, hp - amount * (exposed() ? 2 : 1));
        flash = .095;
        if (hp == 0) flames.clear();
    }
}
