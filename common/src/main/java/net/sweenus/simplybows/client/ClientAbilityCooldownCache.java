package net.sweenus.simplybows.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache for bow ability cooldown data, populated via S2C networking packets.
 * Keyed by the bow's tooltip key (e.g. "vine", "ice"). Each entry stores [endMs, totalTicks].
 * This class contains no client-only imports so it can be safely referenced from common code
 * via the {@link net.sweenus.simplybows.item.unique.SimplyBowItem#CLIENT_COOLDOWN_READER} delegate.
 */
public final class ClientAbilityCooldownCache {

    private static final Map<String, long[]> CACHE = new HashMap<>();

    private ClientAbilityCooldownCache() {
    }

    public static void update(String key, long endMs, int totalTicks) {
        long[] existing = CACHE.get(key);
        if (existing != null && endMs <= existing[0] && totalTicks <= (int) existing[1]) {
            return;
        }
        CACHE.put(key, new long[]{endMs, totalTicks});
    }

    /** Returns {@code [endMs, totalTicks]} or {@code null} if no entry exists. */
    public static long[] get(String key) {
        return CACHE.get(key);
    }

    public static void clear(String key) {
        CACHE.remove(key);
    }

    /** Removes all cached cooldowns. Call this when the client joins a new world session so
     *  stale bars from a previous session cannot persist when the server-side state has reset. */
    public static void clearAll() {
        CACHE.clear();
    }
}
