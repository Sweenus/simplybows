package net.sweenus.simplybows.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.sweenus.simplybows.entity.VineFlowerVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VineFlowerFieldManager {

    private static final int FIELD_DURATION_TICKS = 200;
    private static final double FIELD_RADIUS = 5.0;
    private static final double ATTRACTION_RADIUS = 14.0;
    private static final double PATCH_VISUAL_RADIUS = 2.35;
    private static final int PATCH_VISUAL_POINTS = 26;
    private static final double STRING_FIELD_RADIUS_BONUS_PER_LEVEL = 1.0;
    private static final double STRING_ATTRACTION_RADIUS_BONUS_PER_LEVEL = 2.2;
    private static final double STRING_VISUAL_RADIUS_BONUS_PER_LEVEL = 0.85;
    private static final int GROWTH_POINTS_PER_TICK = 6;
    private static final int SPRING_ANIM_TICKS = 8;
    private static final double SPRING_START_OFFSET_Y = -0.62;
    private static final float FRIENDLY_HEAL = 1.0F;
    private static final float HOSTILE_DAMAGE = 2.0F;
    private static final float UNDEAD_BONUS_DAMAGE = 0.6F;
    private static final int GROUND_SCAN_UP = 5;
    private static final int GROUND_SCAN_DOWN = 18;
    private static final int FLOWER_TYPE_SHORT_GRASS = 0;
    private static final int FLOWER_TYPE_FERN = 1;
    private static final int FLOWER_TYPE_DANDELION = 2;
    private static final int FLOWER_TYPE_POPPY = 3;
    private static final int FLOWER_TYPE_CHERRY_LOG = 4;
    private static final int FLOWER_TYPE_CHERRY_LEAVES = 5;
    private static final int MAX_VISUAL_POINTS = 180;
    private static final int FIELD_PULSE_INTERVAL_TICKS = 24;
    private static final int FIELD_PULSE_CUTOFF_TICKS = 80;
    private static final String FIELD_VISUAL_TAG = "simplybows_vine_field_visual";
    private static final Map<ServerWorld, List<ActiveFlowerField>> ACTIVE_FIELDS = new HashMap<>();

    private VineFlowerFieldManager() {
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner) {
        createOrReplaceField(world, center, owner, BowUpgradeData.none());
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades) {
        List<ActiveFlowerField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (ownerId != null) {
            fields.removeIf(field -> {
                if (ownerId.equals(field.ownerId())) {
                    removeField(world, field);
                    return true;
                }
                return false;
            });
        }

        long expiryTick = world.getTime() + FIELD_DURATION_TICKS;
        FieldTuning tuning = buildTuning(upgrades);
        List<FlowerPoint> pendingPoints = buildPatchPoints(world, center, tuning.visualPoints(), tuning.visualRadius());
        if (tuning.cherryTreeVisual()) {
            pendingPoints.addAll(buildCherryTreeVisualPoints(world, center));
        }
        fields.add(new ActiveFlowerField(center, expiryTick, pendingPoints, ownerId, tuning));
        playFieldCreationSound(world, center);
        spawnBurstParticles(world, center, tuning);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center) {
        createOrReplaceField(world, center, null, BowUpgradeData.none());
    }

    public static void tick(ServerWorld world) {
        List<ActiveFlowerField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanFieldVisuals(world);
            }
            return;
        }

        fields.removeIf(field -> {
            if (world.getTime() >= field.expiryTick()) {
                removeField(world, field);
                return true;
            }
            return false;
        });
        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
            return;
        }

        for (ActiveFlowerField field : fields) {
            growFieldVisuals(world, field);
            animateFieldVisuals(world, field);
            spawnAmbientParticles(world, field.center(), field.tuning());
            playFieldPulseSound(world, field);

            if (world.getTime() % 10L == 0L) {
                attractPassiveMobs(world, field.center(), field.tuning());
            }

            if (world.getTime() % field.tuning().auraIntervalTicks() == 0L) {
                applyAuraEffects(world, field);
            }
        }
    }

    private static void attractPassiveMobs(ServerWorld world, Vec3d center, FieldTuning tuning) {
        Box box = Box.of(center, tuning.attractionRadius() * 2.0, 6.0, tuning.attractionRadius() * 2.0);
        for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class, box, LivingEntity::isAlive)) {
            if (animal.squaredDistanceTo(center) > tuning.attractionRadius() * tuning.attractionRadius()) {
                continue;
            }
            animal.getNavigation().startMovingTo(center.x, center.y, center.z, 1.2);
        }
    }

    private static void applyAuraEffects(ServerWorld world, ActiveFlowerField field) {
        Vec3d center = field.center();
        FieldTuning tuning = field.tuning();
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        Box box = Box.of(center, tuning.fieldRadius() * 2.0, 4.0, tuning.fieldRadius() * 2.0);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (entity.squaredDistanceTo(center) > tuning.fieldRadius() * tuning.fieldRadius()) {
                continue;
            }

            if (entity instanceof net.minecraft.entity.mob.HostileEntity || CombatTargeting.isTargetWhitelisted(entity)) {
                if (tuning.damageHostiles() && tuning.hostileDamage() > 0.0F) {
                    float damage = tuning.hostileDamage();
                    if (entity.getType().isIn(EntityTypeTags.UNDEAD)) {
                        damage += tuning.undeadBonusDamage();
                    }
                    boolean died = dealAuraDamage(world, owner, entity, damage);
                    if (died && tuning.bountyLootChance() > 0.0) {
                        trySpawnBountyLoot(world, entity, owner, tuning.bountyLootChance());
                    }
                }
                continue;
            }

            if (tuning.cleanseNegativeEffects()) {
                if (cleanseNegativeEffects(entity)) {
                    world.playSound(
                            null,
                            entity.getX(),
                            entity.getBodyY(0.5),
                            entity.getZ(),
                            SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT,
                            SoundCategory.PLAYERS,
                            0.75F,
                            1.35F + world.getRandom().nextFloat() * 0.15F
                    );
                }
            }
            if (tuning.healFriendlies() && entity.getHealth() < entity.getMaxHealth()) {
                CombatTargeting.applyHealing(owner, entity, tuning.friendlyHeal());
            }
        }
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.35, center.z, 2, 1.6, 0.2, 1.6, 0.0);
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 2, 1.3, 0.1, 1.3, 0.0);
        spawnGlitterParticles(world, center, tuning);
        if (tuning.cherryTreeVisual() && world.getTime() % 2L == 0L) {
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 2.5, center.z, 2, 0.45, 0.6, 0.45, 0.0);
            if (world.getTime() % 6L == 0L) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 2.2, center.z, 1, 0.28, 0.4, 0.28, 0.0);
            }
        }
    }

    private static void spawnGlitterParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        if (world.getTime() % 3L != 0L) {
            return;
        }
        int count = world.random.nextInt(2) + 1;
        double radius = tuning.fieldRadius() * 0.92;
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(world.random.nextDouble()) * radius;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y + 0.45 + world.random.nextDouble() * 0.55;

            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.0, 0.0, 0.01);
            if (world.random.nextFloat() < 0.35F) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.04, z, 1, 0.0, 0.05, 0.0, 0.01);
            }
        }
    }

    private static void playFieldPulseSound(ServerWorld world, ActiveFlowerField field) {
        if (world.getTime() >= field.expiryTick() - FIELD_PULSE_CUTOFF_TICKS) {
            return;
        }
        if (world.getTime() % FIELD_PULSE_INTERVAL_TICKS != 0L) {
            return;
        }
        Vec3d center = field.center();
        float phase = (float) Math.sin((double) world.getTime() * 0.08);
        float pitch = 0.6F + phase * 0.15F + world.getRandom().nextFloat() * 0.06F;
        world.playSound(
                null,
                center.x,
                center.y + 0.25,
                center.z,
                SoundEvents.BLOCK_BEACON_AMBIENT,
                SoundCategory.PLAYERS,
                0.90F,
                pitch
        );
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center, FieldTuning tuning) {
        world.spawnParticles(ParticleTypes.COMPOSTER, center.x, center.y + 0.2, center.z, 10, 0.9, 0.15, 0.9, 0.0);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SHORT_GRASS.getDefaultState()), center.x, center.y + 0.1, center.z, 10, 0.9, 0.1, 0.9, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DANDELION.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.POPPY.getDefaultState()), center.x, center.y + 0.15, center.z, 4, 0.8, 0.12, 0.8, 0.015);
        if (tuning.cherryTreeVisual()) {
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y + 1.5, center.z, 36, 0.9, 1.0, 0.9, 0.01);
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 1.2, center.z, 20, 0.8, 0.8, 0.8, 0.0);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.CHERRY_LEAVES.getDefaultState()), center.x, center.y + 1.0, center.z, 16, 0.75, 0.6, 0.75, 0.01);
        }
    }

    private static void playFieldCreationSound(ServerWorld world, Vec3d center) {
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_GRASS_PLACE,
                SoundCategory.BLOCKS,
                0.9F,
                0.95F + world.getRandom().nextFloat() * 0.2F
        );
    }

    private static List<FlowerPoint> buildPatchPoints(ServerWorld world, Vec3d center, int pointCount, double visualRadius) {
        List<FlowerPoint> points = new ArrayList<>();
        for (int i = 0; i < pointCount; i++) {
            double angle = ((Math.PI * 2.0) / pointCount) * i;
            double ringScale = (0.45 + ((i * 17) % 10) * 0.06);
            double radius = visualRadius * Math.min(1.0, ringScale);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + 0.03;
            int flowerType;

            if (i % 4 == 0) {
                flowerType = FLOWER_TYPE_DANDELION;
            } else if (i % 5 == 0) {
                flowerType = FLOWER_TYPE_POPPY;
            } else if (i % 3 == 0) {
                flowerType = FLOWER_TYPE_FERN;
            } else {
                flowerType = FLOWER_TYPE_SHORT_GRASS;
            }
            points.add(new FlowerPoint(x, y, z, flowerType));
        }
        return points;
    }

    private static FieldTuning buildTuning(BowUpgradeData upgrades) {
        double sizeMultiplier = upgrades.sizeMultiplier();
        float frameMultiplier = (float) upgrades.damageMultiplier();
        int stringLevel = upgrades.stringLevel();
        RuneEtching rune = upgrades.runeEtching();

        float friendlyHeal = FRIENDLY_HEAL * frameMultiplier;
        float hostileDamage = HOSTILE_DAMAGE * frameMultiplier;
        float undeadBonusDamage = UNDEAD_BONUS_DAMAGE * frameMultiplier;
        boolean healFriendlies = true;
        boolean damageHostiles = true;
        boolean cleanseNegative = false;
        boolean cherryTreeVisual = false;
        double bountyLootChance = 0.0;
        int auraInterval = 20;

        if (rune == RuneEtching.PAIN) {
            healFriendlies = false;
            damageHostiles = true;
            auraInterval = 10;
        } else if (rune == RuneEtching.GRACE) {
            healFriendlies = true;
            damageHostiles = false;
            hostileDamage = 0.0F;
            undeadBonusDamage = 0.0F;
            cleanseNegative = true;
            cherryTreeVisual = true;
        } else if (rune == RuneEtching.BOUNTY) {
            bountyLootChance = 0.25;
        }

        double fieldRadius = FIELD_RADIUS * sizeMultiplier + stringLevel * STRING_FIELD_RADIUS_BONUS_PER_LEVEL;
        double attractionRadius = ATTRACTION_RADIUS * sizeMultiplier + stringLevel * STRING_ATTRACTION_RADIUS_BONUS_PER_LEVEL;
        double visualRadius = PATCH_VISUAL_RADIUS * sizeMultiplier + stringLevel * STRING_VISUAL_RADIUS_BONUS_PER_LEVEL;
        double radiusRatio = visualRadius / PATCH_VISUAL_RADIUS;
        int visualPoints = Math.min(
                MAX_VISUAL_POINTS,
                Math.max(
                        PATCH_VISUAL_POINTS,
                        (int) Math.round(PATCH_VISUAL_POINTS * radiusRatio * radiusRatio) + upgrades.stringLevel() * 10
                )
        );
        return new FieldTuning(
                fieldRadius,
                attractionRadius,
                visualRadius,
                friendlyHeal,
                hostileDamage,
                undeadBonusDamage,
                healFriendlies,
                damageHostiles,
                cleanseNegative,
                cherryTreeVisual,
                bountyLootChance,
                Math.max(5, auraInterval),
                visualPoints
        );
    }

    private static List<FlowerPoint> buildCherryTreeVisualPoints(ServerWorld world, Vec3d center) {
        List<FlowerPoint> points = new ArrayList<>();
        double baseY = findGroundTopY(world, center.x, center.z, center.y) + 0.03;

        // Trunk
        points.add(new FlowerPoint(center.x, baseY, center.z, FLOWER_TYPE_CHERRY_LOG));
        points.add(new FlowerPoint(center.x, baseY + 1.0, center.z, FLOWER_TYPE_CHERRY_LOG));
        points.add(new FlowerPoint(center.x, baseY + 2.0, center.z, FLOWER_TYPE_CHERRY_LOG));
        // Add leaf coverage at top trunk space so the trunk cap blends into canopy.
        points.add(new FlowerPoint(center.x, baseY + 2.0, center.z, FLOWER_TYPE_CHERRY_LEAVES));

        // Canopy ring
        for (int i = 0; i < 8; i++) {
            double angle = ((Math.PI * 2.0) / 8.0) * i;
            double x = center.x + Math.cos(angle) * 1.1;
            double z = center.z + Math.sin(angle) * 1.1;
            points.add(new FlowerPoint(x, baseY + 2.2, z, FLOWER_TYPE_CHERRY_LEAVES));
        }

        // Canopy top
        points.add(new FlowerPoint(center.x, baseY + 3.0, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x + 0.55, baseY + 2.9, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x - 0.55, baseY + 2.9, center.z, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x, baseY + 2.9, center.z + 0.55, FLOWER_TYPE_CHERRY_LEAVES));
        points.add(new FlowerPoint(center.x, baseY + 2.9, center.z - 0.55, FLOWER_TYPE_CHERRY_LEAVES));
        return points;
    }

    private static boolean dealAuraDamage(ServerWorld world, LivingEntity owner, LivingEntity entity, float damage) {
        float before = entity.getHealth();
        boolean damaged = CombatTargeting.applyDamage(world, owner, entity, damage, true);
        return damaged && before > 0.0F && !entity.isAlive();
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static boolean cleanseNegativeEffects(LivingEntity entity) {
        List<StatusEffectInstance> toRemove = new ArrayList<>();
        for (StatusEffectInstance instance : entity.getStatusEffects()) {
            if (!instance.getEffectType().value().isBeneficial()) {
                toRemove.add(instance);
            }
        }
        if (toRemove.isEmpty()) {
            return false;
        }
        for (StatusEffectInstance instance : toRemove) {
            entity.removeStatusEffect(instance.getEffectType());
        }
        return true;
    }

    private static void trySpawnBountyLoot(ServerWorld world, LivingEntity entity, LivingEntity owner, double chance) {
        if (!world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            return;
        }
        if (entity.getRandom().nextDouble() > chance) {
            return;
        }

        RegistryKey<LootTable> lootTableKey = entity.getLootTable();
        LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);
        DamageSource damageSource = createBountyDamageSource(world, owner);

        LootContextParameterSet.Builder builder = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.ORIGIN, entity.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
                .addOptional(LootContextParameters.ATTACKING_ENTITY, damageSource.getAttacker())
                .addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, damageSource.getSource());

        if (owner instanceof ServerPlayerEntity playerOwner) {
            builder = builder.add(LootContextParameters.LAST_DAMAGE_PLAYER, playerOwner).luck(playerOwner.getLuck());
        }

        LootContextParameterSet lootContext = builder.build(LootContextTypes.ENTITY);
        List<ItemStack> generatedLoot = lootTable.generateLoot(lootContext, entity.getLootTableSeed() ^ world.getRandom().nextLong());
        if (generatedLoot.isEmpty()) {
            return;
        }

        List<ItemStack> nonEmptyLoot = generatedLoot.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .toList();
        if (nonEmptyLoot.isEmpty()) {
            return;
        }

        ItemStack extraDrop = nonEmptyLoot.get(world.getRandom().nextInt(nonEmptyLoot.size())).copy();
        entity.dropStack(extraDrop);
    }

    private static DamageSource createBountyDamageSource(ServerWorld world, LivingEntity owner) {
        if (owner instanceof ServerPlayerEntity playerOwner) {
            return world.getDamageSources().playerAttack(playerOwner);
        }
        if (owner != null) {
            return world.getDamageSources().mobAttack(owner);
        }
        return world.getDamageSources().magic();
    }

    private static void growFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.spawnCursor >= field.pendingPoints.size()) {
            return;
        }

        int spawnCount = Math.min(GROWTH_POINTS_PER_TICK, field.pendingPoints.size() - field.spawnCursor);
        for (int i = 0; i < spawnCount; i++) {
            FlowerPoint point = field.pendingPoints.get(field.spawnCursor++);
            BlockState state = flowerTypeToBlockState(point.flowerType);
            UUID id = spawnFlowerVisual(world, field.displayIds, point.x, point.y, point.z, point.flowerType);
            if (id != null) {
                field.springVisuals.add(new SpringVisual(id, point.x, point.y, point.z, world.getTime()));
            }
            // Brief upward burst so the patch feels like it's emerging from the ground.
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), point.x, point.y - 0.08, point.z, 3, 0.08, 0.02, 0.08, 0.03);
            world.spawnParticles(ParticleTypes.COMPOSTER, point.x, point.y + 0.04, point.z, 2, 0.06, 0.01, 0.06, 0.0);
        }
    }

    private static void animateFieldVisuals(ServerWorld world, ActiveFlowerField field) {
        if (field.springVisuals.isEmpty()) {
            return;
        }

        field.springVisuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof VineFlowerVisualEntity display)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= SPRING_ANIM_TICKS) {
                display.setPos(visual.targetX(), visual.targetY(), visual.targetZ());
                display.setHeightScale(1.0F);
                return true;
            }

            float t = (float) age / (float) SPRING_ANIM_TICKS;
            double riseOffset = SPRING_START_OFFSET_Y * (1.0 - t) * (1.0 - t);
            double bounceOffset = Math.sin(t * Math.PI * 2.3) * 0.10 * (1.0 - t);
            double y = visual.targetY() + riseOffset + bounceOffset;
            display.setPos(visual.targetX(), y, visual.targetZ());
            display.setHeightScale(MathHelper.clamp(t * 1.05F, 0.0F, 1.0F));
            return false;
        });
    }

    private static UUID spawnFlowerVisual(ServerWorld world, List<UUID> ids, double x, double y, double z, int flowerType) {
        VineFlowerVisualEntity visual = new VineFlowerVisualEntity(world, x, y + SPRING_START_OFFSET_Y, z, flowerType);
        if (flowerType == FLOWER_TYPE_CHERRY_LOG) {
            visual.setYaw(0.0F);
        } else {
            visual.setYaw(world.random.nextFloat() * 360.0F);
        }
        visual.setHeightScale(0.0F);
        visual.addCommandTag(FIELD_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return null;
        }
        ids.add(visual.getUuid());
        return visual.getUuid();
    }

    private static BlockState flowerTypeToBlockState(int flowerType) {
        return switch (flowerType) {
            case FLOWER_TYPE_DANDELION -> Blocks.DANDELION.getDefaultState();
            case FLOWER_TYPE_POPPY -> Blocks.POPPY.getDefaultState();
            case FLOWER_TYPE_FERN -> Blocks.FERN.getDefaultState();
            case FLOWER_TYPE_CHERRY_LOG -> Blocks.CHERRY_LOG.getDefaultState();
            case FLOWER_TYPE_CHERRY_LEAVES -> Blocks.CHERRY_LEAVES.getDefaultState();
            default -> Blocks.SHORT_GRASS.getDefaultState();
        };
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }

        return centerY;
    }

    private static void removeField(ServerWorld world, ActiveFlowerField field) {
        if (field == null) {
            return;
        }

        for (UUID id : field.displayIds) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanFieldVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof VineFlowerVisualEntity && entity.getCommandTags().contains(FIELD_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveFlowerField {
        private final Vec3d center;
        private final long expiryTick;
        private final List<FlowerPoint> pendingPoints;
        private final UUID ownerId;
        private final FieldTuning tuning;
        private final List<UUID> displayIds = new ArrayList<>();
        private final List<SpringVisual> springVisuals = new ArrayList<>();
        private int spawnCursor;

        private ActiveFlowerField(Vec3d center, long expiryTick, List<FlowerPoint> pendingPoints, UUID ownerId, FieldTuning tuning) {
            this.center = center;
            this.expiryTick = expiryTick;
            this.pendingPoints = pendingPoints;
            this.ownerId = ownerId;
            this.tuning = tuning;
        }

        private Vec3d center() {
            return this.center;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private UUID ownerId() {
            return this.ownerId;
        }

        private FieldTuning tuning() {
            return this.tuning;
        }
    }

    private record FlowerPoint(double x, double y, double z, int flowerType) {
    }

    private record SpringVisual(UUID id, double targetX, double targetY, double targetZ, long spawnTick) {
    }

    private record FieldTuning(
            double fieldRadius,
            double attractionRadius,
            double visualRadius,
            float friendlyHeal,
            float hostileDamage,
            float undeadBonusDamage,
            boolean healFriendlies,
            boolean damageHostiles,
            boolean cleanseNegativeEffects,
            boolean cherryTreeVisual,
            double bountyLootChance,
            int auraIntervalTicks,
            int visualPoints
    ) {
    }
}
