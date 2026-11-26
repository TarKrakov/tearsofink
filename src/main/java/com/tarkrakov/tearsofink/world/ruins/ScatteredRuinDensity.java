package com.tarkrakov.tearsofink.world.ruins;

/** Independent, seed-stable 50% thinning of the existing scattered ruin candidates. */
public final class ScatteredRuinDensity {
    private ScatteredRuinDensity() {}
    public static boolean keep(long seed, int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        long h = seed ^ key ^ 0x7A6D3B1F98C245EL;
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h ^= h >>> 31;
        return (h & 1L) == 0L;
    }
}
