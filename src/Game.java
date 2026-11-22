import java.util.ArrayList;
import java.util.List;

final class Game {
    record Input(int horizontal, boolean jumpPressed, boolean attackPressed, boolean dashPressed) {}

    enum RoundResult {
        IN_PROGRESS, BLUE_WINS, RED_WINS, DRAW, TEAM_WINS, TEAM_DEFEATED
    }

    private static final double FIRST_DROP_SECONDS = 2.0;
    private static final double RESUPPLY_SECONDS = 6.0;
    private static final SupplyChest.Loot[] SUPPLY_ROTATION = SupplyChest.Loot.values();
    private final List<Projectile> shots = new ArrayList<>();

    private final Arena arena;
    Arena arena() { return arena; }
    private final GameMode mode;
    private final Weapon blueStartingWeapon;
    private final Weapon redStartingWeapon;
    private Player playerOne;
    private Player playerTwo;
    private RaidBoss boss;
    private double supplyDropRemaining;
    private SupplyChest supplyChest;
    private Arena.SupplySpot previousSupplySpot;
    private int nextSupplyIndex;

    Game() {
        this(GameMode.DUEL);
    }

    Game(GameMode mode) {
        this(mode, mode == GameMode.HIVE ? Weapon.SPARK_WAND : Weapon.PULSE_BLADE,
            mode == GameMode.HIVE ? Weapon.SPARK_WAND : Weapon.PULSE_BLADE);
    }

    Game(Weapon blueStartingWeapon, Weapon redStartingWeapon) {
        this(GameMode.DUEL, blueStartingWeapon, redStartingWeapon);
    }

    private Game(GameMode mode, Weapon blueStartingWeapon, Weapon redStartingWeapon) {
        this.mode = mode;
        arena = new Arena(mode == GameMode.HIVE);
        this.blueStartingWeapon = blueStartingWeapon;
        this.redStartingWeapon = redStartingWeapon;
        startRound();
    }

    private void startRound() {
        shots.clear();
        playerOne = new Player(arena.spawnX(true), arena.spawnY(), 1, mode == GameMode.HIVE);
        playerOne.equip(blueStartingWeapon);
        playerTwo = new Player(arena.spawnX(false), arena.spawnY(), -1, mode == GameMode.HIVE);
        playerTwo.equip(redStartingWeapon);
        boss = switch (mode) {
            case DUEL -> null;
            case BOSS -> new Boss();
            case HIVE -> new CinderQueen();
        };
        supplyDropRemaining = FIRST_DROP_SECONDS;
        supplyChest = null;
        previousSupplySpot = null;
        nextSupplyIndex = 0;
    }

    boolean rematch() {
        if (!isRoundOver()) {
            return false;
        }

        startRound();
        return true;
    }

    void update(Input oneInput, Input twoInput, double seconds) {
        playerOne.updateCombatTimers(seconds);
        playerTwo.updateCombatTimers(seconds);
        if (boss != null) {
            boss.updateEffects(seconds);
        }
        if (isRoundOver()) {
            return;
        }

        // Players who attack on the same tick both get to resolve their swing.
        Attack oneAttack = updatePlayer(playerOne, oneInput, seconds);
        Attack twoAttack = updatePlayer(playerTwo, twoInput, seconds);
        shots.removeIf(shot -> {
            shot.advance(seconds);
            return (boss == null ? shot.hitOpponent(playerOne, playerTwo) : boss.hitBy(shot))
                || shot.expired(arena);
        });
        if (boss == null) {
            if (oneAttack != null && oneAttack.hits(playerTwo)) {
                playerTwo.takeHit(oneAttack);
            }
            if (twoAttack != null && twoAttack.hits(playerOne)) {
                playerOne.takeHit(twoAttack);
            }
        } else {
            boss.takeHit(oneAttack);
            boss.takeHit(twoAttack);
            if (boss.health() > 0) {
                boss.update(seconds, playerOne, playerTwo, arena);
            }
        }
        if (!isRoundOver()) {
            updateSupplyChest(seconds);
        }
    }

    private Attack updatePlayer(Player player, Input input, double seconds) {
        if (player.health() == 0) {
            player.move(0, false, false, seconds, arena);
            return null;
        }
        player.move(input.horizontal(), input.jumpPressed(), input.dashPressed(), seconds, arena);
        Attack attack = input.attackPressed() ? player.tryAttack() : null;
        if (attack != null && attack.weapon().flight() != null) {
            shots.add(new Projectile(player, attack));
            return null;
        }
        return attack;
    }

    Iterable<Projectile> shots() {
        return shots;
    }

    private void updateSupplyChest(double seconds) {
        if (supplyChest == null) {
            supplyDropRemaining = Math.max(0, supplyDropRemaining - seconds);
            if (supplyDropRemaining > 0) {
                return;
            }
            previousSupplySpot = arena.randomSupplySpot(previousSupplySpot);
            supplyChest = new SupplyChest(previousSupplySpot.x(),
                previousSupplySpot.surfaceY() - SupplyChest.HALF_HEIGHT,
                SUPPLY_ROTATION[nextSupplyIndex]);
        }

        Player collector = supplyChest.findCollector(playerOne, playerTwo);
        if (collector != null) {
            supplyChest.loot().apply(collector);
            nextSupplyIndex = (nextSupplyIndex + 1) % SUPPLY_ROTATION.length;
            supplyChest = null;
            supplyDropRemaining = RESUPPLY_SECONDS;
        }
    }

    SupplyChest supplyChest() {
        return supplyChest;
    }

    boolean isRoundOver() {
        return roundResult() != RoundResult.IN_PROGRESS;
    }

    RoundResult roundResult() {
        if (boss != null) {
            if (boss.health() == 0) {
                return RoundResult.TEAM_WINS;
            }
            return playerOne.health() == 0 && playerTwo.health() == 0
                ? RoundResult.TEAM_DEFEATED : RoundResult.IN_PROGRESS;
        }
        if (playerOne.health() == 0) {
            return playerTwo.health() == 0 ? RoundResult.DRAW : RoundResult.RED_WINS;
        }
        if (playerTwo.health() == 0) {
            return RoundResult.BLUE_WINS;
        }
        return RoundResult.IN_PROGRESS;
    }

    Player playerOne() {
        return playerOne;
    }

    Player playerTwo() {
        return playerTwo;
    }

    GameMode mode() {
        return mode;
    }

    RaidBoss boss() {
        return boss;
    }
}
