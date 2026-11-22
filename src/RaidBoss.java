abstract class RaidBoss {
    record Zone(int x, int y, int width, int height) {
        boolean contains(Player player) {
            return player.x() >= x && player.x() <= x + width
                && player.y() >= y && player.y() <= y + height;
        }
    }

    abstract String name();
    abstract int health();
    abstract int maxHealth();
    abstract int phase();
    abstract boolean exposed();
    abstract String status();
    abstract void takeHit(Attack attack);
    abstract boolean hitBy(Projectile shot);
    abstract void update(double seconds, Player blue, Player red, Arena arena);
    abstract void updateEffects(double seconds);
    abstract double shutdownProgress();
}
