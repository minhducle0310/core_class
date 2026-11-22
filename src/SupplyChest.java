record SupplyChest(int x, int y, Loot loot) {
    static final int RADIUS = 16;
    static final int HALF_HEIGHT = 11;

    enum Loot {
        SPARK_WAND(Weapon.SPARK_WAND),
        SHOCK_BATON(Weapon.SHOCK_BATON),
        CINDER_STAFF(Weapon.CINDER_STAFF),
        REPAIR_KIT(null),
        GYRO_AXE(Weapon.GYRO_AXE),
        GRAVITY_HAMMER(Weapon.GRAVITY_HAMMER);

        static final int REPAIR_HP = 25;
        private final Weapon weapon;

        Loot(Weapon weapon) {
            this.weapon = weapon;
        }

        boolean canCollect(Player player) {
            return weapon != null || player.health() < Player.MAX_HEALTH;
        }

        void apply(Player player) {
            if (weapon != null) {
                player.equip(weapon);
            } else {
                player.repair(REPAIR_HP);
            }
        }

    }

    Player findCollector(Player blue, Player red) {
        double blueDistance = eligibleDistance(blue);
        double redDistance = eligibleDistance(red);
        double pickupDistance = RADIUS + Player.RADIUS;

        if (Math.min(blueDistance, redDistance) > pickupDistance
                || Math.abs(blueDistance - redDistance) < 0.000001) {
            // Equally close robots leave the chest for the next tick.
            return null;
        }
        return blueDistance < redDistance ? blue : red;
    }

    private double eligibleDistance(Player player) {
        return player.health() > 0 && loot.canCollect(player)
            && player.y() + Player.RADIUS <= y + HALF_HEIGHT + 1
            ? Math.hypot(player.x() - x, player.y() - y)
            : Double.POSITIVE_INFINITY;
    }
}
