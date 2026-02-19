package net.sweenus.simplybows.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SimplyBowsLootTestCommand {

    private static final int MIN_ROLLS = 1;
    private static final int MAX_ROLLS = 200000;

    private SimplyBowsLootTestCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("simplybows_loot_test")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("table", IdentifierArgumentType.identifier())
                        .then(CommandManager.argument("rolls", IntegerArgumentType.integer(MIN_ROLLS, MAX_ROLLS))
                                .executes(SimplyBowsLootTestCommand::run))));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        Identifier rawTableId = IdentifierArgumentType.getIdentifier(context, "table");
        Identifier tableId = normalizeTableId(rawTableId);
        int rolls = IntegerArgumentType.getInteger(context, "rolls");

        RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE, tableId);
        LootTable lootTable = source.getServer().getReloadableRegistries().getLootTable(key);

        LootContextParameterSet lootContext = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, source.getPosition())
                .addOptional(LootContextParameters.THIS_ENTITY, source.getEntity())
                .build(LootContextTypes.CHEST);

        Map<Identifier, Long> itemCounts = new HashMap<>();
        long totalSimplyBowsItems = 0L;
        int rollsWithSimplyBowsDrop = 0;

        for (int i = 0; i < rolls; i++) {
            List<ItemStack> generated = lootTable.generateLoot(lootContext, world.getRandom().nextLong());
            boolean foundInRoll = false;
            for (ItemStack stack : generated) {
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
                if (itemId == null || !"simplybows".equals(itemId.getNamespace())) {
                    continue;
                }
                long count = Math.max(1, stack.getCount());
                itemCounts.merge(itemId, count, Long::sum);
                totalSimplyBowsItems += count;
                foundInRoll = true;
            }
            if (foundInRoll) {
                rollsWithSimplyBowsDrop++;
            }
        }

        final int finalRolls = rolls;
        final int finalRollsWithDrop = rollsWithSimplyBowsDrop;
        final long finalTotalSimplyBowsItems = totalSimplyBowsItems;
        if (!rawTableId.equals(tableId)) {
            source.sendFeedback(() -> Text.literal("Normalized loot table id: " + tableId + " (from " + rawTableId + ")"), false);
        }
        source.sendFeedback(() -> Text.literal("Loot test: " + tableId + " | rolls=" + finalRolls), false);
        source.sendFeedback(() -> Text.literal("Rolls with SimplyBows drop: " + finalRollsWithDrop + "/" + finalRolls +
                " (" + formatPct(finalRollsWithDrop, finalRolls) + ")"), false);
        source.sendFeedback(() -> Text.literal("Total SimplyBows items dropped: " + finalTotalSimplyBowsItems), false);

        if (itemCounts.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No SimplyBows items dropped in this simulation."), false);
            return 1;
        }

        List<Map.Entry<Identifier, Long>> sorted = new ArrayList<>(itemCounts.entrySet());
        sorted.sort(Comparator.comparingLong((Map.Entry<Identifier, Long> e) -> e.getValue()).reversed());
        for (Map.Entry<Identifier, Long> entry : sorted) {
            Identifier itemId = entry.getKey();
            long count = entry.getValue();
            source.sendFeedback(() -> Text.literal(" - " + itemId + ": " + count + " (" + formatPct(count, rolls) + " per roll)"), false);
        }
        return 1;
    }

    private static Identifier normalizeTableId(Identifier id) {
        String namespace = id.getNamespace();
        String path = id.getPath();

        // Common typo: "minecraft:minecraft/chests/ancient_city".
        if ("minecraft".equals(namespace) && path.startsWith("minecraft/")) {
            path = path.substring("minecraft/".length());
        }
        // Convenience: allow "ancient_city" shorthand.
        if (!path.contains("/")) {
            path = "chests/" + path;
        }
        // Convenience: allow "/chests/..." prefixes from copied resource paths.
        int chestsIndex = path.indexOf("chests/");
        if (chestsIndex > 0) {
            path = path.substring(chestsIndex);
        }

        return Identifier.of(namespace, path);
    }

    private static String formatPct(long count, int total) {
        if (total <= 0) {
            return "0.000%";
        }
        double pct = (count * 100.0) / total;
        return String.format(Locale.ROOT, "%.4f%%", pct);
    }
}
