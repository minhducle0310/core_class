record Attack(double x, double y, double facingX, Weapon weapon) {
    boolean hits(Player target) {
        return hitsAt(target.x(), target.y());
    }

    boolean hitsAt(double targetX, double targetY) {
        double offsetX = targetX - x;
        double offsetY = targetY - y;
        if (weapon == Weapon.CROSS_BEAM) {
            double forward = offsetX * facingX;
            return forward >= 0 && forward <= weapon.range() && Math.abs(offsetY) <= 10;
        }
        double distance = Math.hypot(offsetX, offsetY);

        double forwardDistance = offsetX * facingX;
        double minimumAlignment = Math.cos(Math.toRadians(weapon.halfAngleDegrees()));
        return distance <= weapon.range() && forwardDistance >= distance * minimumAlignment;
    }
}
