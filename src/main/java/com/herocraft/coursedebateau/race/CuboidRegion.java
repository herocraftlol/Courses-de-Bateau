package com.herocraft.coursedebateau.race;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Zone cuboide simple definie par deux coins. Utilisee pour les points de passage
 * (checkpoints) et la ligne de depart/arrivee des courses de bateau.
 */
public class CuboidRegion {

    private final Location corner1;
    private final Location corner2;

    public CuboidRegion(Location corner1, Location corner2) {
        this.corner1 = corner1;
        this.corner2 = corner2;
    }

    public Location getCorner1() {
        return corner1;
    }

    public Location getCorner2() {
        return corner2;
    }

    /**
     * Verifie si une localisation se trouve dans la zone (meme monde + bornes XYZ).
     * Comparaison en coordonnees de bloc entieres pour eviter qu'une zone posee sur
     * un seul bloc ne devienne plus etroite que le bloc a cause de decimales.
     */
    public boolean contains(Location loc) {
        World world = corner1.getWorld();
        if (world == null || loc.getWorld() == null || !world.equals(loc.getWorld())) {
            return false;
        }

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        int blockX = loc.getBlockX();
        int blockY = loc.getBlockY();
        int blockZ = loc.getBlockZ();

        return blockX >= minX && blockX <= maxX
                && blockY >= minY && blockY <= maxY
                && blockZ >= minZ && blockZ <= maxZ;
    }

    public Location getCenter() {
        double x = (corner1.getX() + corner2.getX()) / 2.0;
        double z = (corner1.getZ() + corner2.getZ()) / 2.0;
        double y = Math.max(corner1.getY(), corner2.getY());
        return new Location(corner1.getWorld(), x, y, z);
    }
}
