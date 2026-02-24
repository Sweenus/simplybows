package net.sweenus.simplybows.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public final class ClientAbilityCooldownCache {

    private static final Map<String, long[]> CACHE = new HashMap<>();
    private static LongSupplier GAME_TICK_READER = () -> 0L;

    private ClientAbilityCooldownCache() {
    }

    public static void setGameTickReader(LongSupplier reader) {
        GAME_TICK_READER = reader != null ? reader : () -> 0L;
    }

    public static void update(String key, long endMs, int totalTicks) {
        long nowTick = Math.max(0L, GAME_TICK_READER.getAsLong());
        long endTick = nowTick + Math.max(1, totalTicks);
        long[] existing = CACHE.get(key);
        if (existing != null && endTick <= existing[0] && totalTicks <= (int) existing[1]) {
            return;
        }
        CACHE.put(key, new long[]{endTick, totalTicks});
    }

    public static long[] get(String key) {
        return CACHE.get(key);
    }

    public static void clear(String key) {
        CACHE.remove(key);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}
