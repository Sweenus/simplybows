package net.sweenus.simplybows.util;

import dev.architectury.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.compat.opac.OpacCompat;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CombatTargeting {

    private static final Set<String> TARGET_WHITELIST = new HashSet<>();

    static {
        addTargetWhitelist("target_dummy");
        addTargetWhitelist("dummmmmmy:target_dummy");
    }

    private CombatTargeting() {
    }

    // Check if we should be able to hit the target
    public static boolean checkFriendlyFire(LivingEntity livingEntity, LivingEntity attackingEntity) {
        if (livingEntity == null || attackingEntity == null) {
            return false;
        }
        if (!checkEntityBlacklist(livingEntity, attackingEntity)) {
            return false;
        }
        if (livingEntity == attackingEntity) {
            return false;
        }
        if (isTargetWhitelisted(livingEntity)) {
            return true;
        }
        if (livingEntity instanceof VillagerEntity && !(attackingEntity instanceof HostileEntity)) {
            return false;
        }

        AbstractTeam attackerTeam = attackingEntity.getScoreboardTeam();
        AbstractTeam targetTeam = livingEntity.getScoreboardTeam();
        if (isOpacLoaded() && livingEntity instanceof PlayerEntity && attackingEntity instanceof PlayerEntity playerEntity) {
            return OpacCompat.checkOpacFriendlyFire(livingEntity, playerEntity);
        }
        if (attackerTeam != null && targetTeam != null && livingEntity.isTeammate(attackingEntity)) {
            return false;
        }

        if (livingEntity instanceof PlayerEntity playerEntity && attackingEntity instanceof PlayerEntity player) {
            if (playerEntity == attackingEntity) {
                return false;
            }
            return playerEntity.shouldDamagePlayer(player);
        }
        if (livingEntity instanceof Tameable tameable) {
            if (tameable.getOwner() != null) {
                if (tameable.getOwner() != attackingEntity
                        && tameable.getOwner() instanceof PlayerEntity ownerPlayer
                        && attackingEntity instanceof PlayerEntity playerEntity) {
                    if (isOpacLoaded()) {
                        return OpacCompat.checkOpacFriendlyFire(ownerPlayer, playerEntity);
                    }
                    return playerEntity.shouldDamagePlayer(ownerPlayer);
                }
                return tameable.getOwner() != attackingEntity;
            }
            return true;
        }
        return true;
    }

    public static boolean isFriendlyTo(LivingEntity livingEntity, LivingEntity otherEntity) {
        if (livingEntity == null || otherEntity == null) {
            return false;
        }
        return !checkFriendlyFire(livingEntity, otherEntity);
    }

    public static boolean isOffensiveTargetCandidate(LivingEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        return target instanceof HostileEntity
                || target instanceof PlayerEntity
                || isTargetWhitelisted(target);
    }

    public static boolean isOffensiveTargetCandidate(LivingEntity target, @Nullable LivingEntity attacker) {
        if (!isOffensiveTargetCandidate(target)) {
            return false;
        }
        return attacker == null || checkFriendlyFire(target, attacker);
    }

    public static boolean checkEntityBlacklist(LivingEntity livingEntity, LivingEntity attackingEntity) {
        if (livingEntity == null || attackingEntity == null) {
            return false;
        }
        return livingEntity.isAlive() && attackingEntity.isAlive() && !livingEntity.isRemoved() && !attackingEntity.isRemoved();
    }

    public static boolean applyDamage(ServerWorld world, @Nullable Entity attackingEntity, LivingEntity target, float amount, boolean ignoreIframe) {
        return applyDamage(world, attackingEntity, target, amount, ignoreIframe, true);
    }

    public static boolean applyDamage(ServerWorld world, @Nullable Entity attackingEntity, LivingEntity target, float amount, boolean ignoreIframe, boolean applyKnockback) {
        if (world == null || target == null || amount <= 0.0F || !target.isAlive()) {
            return false;
        }

        if (attackingEntity instanceof LivingEntity attackerLiving && !checkFriendlyFire(target, attackerLiving)) {
            return false;
        }

        if (ignoreIframe) {
            target.hurtTime = 0;
            target.timeUntilRegen = 0;
        }

        Vec3d velocityBeforeDamage = applyKnockback ? null : target.getVelocity();
        boolean damaged;
        if (attackingEntity instanceof PlayerEntity playerEntity) {
            damaged = target.damage(world.getDamageSources().playerAttack(playerEntity), amount);
        } else if (attackingEntity instanceof LivingEntity attackerLiving) {
            damaged = target.damage(world.getDamageSources().mobAttack(attackerLiving), amount);
        } else {
            damaged = target.damage(world.getDamageSources().magic(), amount);
        }

        if (ignoreIframe) {
            target.hurtTime = 0;
            target.timeUntilRegen = 0;
        }
        if (!applyKnockback && damaged && velocityBeforeDamage != null) {
            target.setVelocity(velocityBeforeDamage);
        }

        return damaged;
    }

    public static float applyHealing(@Nullable LivingEntity sourceEntity, LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || !target.isAlive()) {
            return 0.0F;
        }
        if (sourceEntity != null && !isFriendlyTo(target, sourceEntity)) {
            return 0.0F;
        }

        float before = target.getHealth();
        target.heal(amount);
        return Math.max(0.0F, target.getHealth() - before);
    }

    public static boolean isOpacLoaded() {
        return Platform.isModLoaded("openpartiesandclaims");
    }

    public static void addTargetWhitelist(String targetId) {
        String normalized = normalizeId(targetId);
        if (normalized != null) {
            TARGET_WHITELIST.add(normalized);
        }
    }

    public static boolean isTargetWhitelisted(LivingEntity target) {
        if (target == null) {
            return false;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(target.getType());
        if (id == null) {
            return false;
        }
        String fullId = normalizeId(id.toString());
        String path = normalizeId(id.getPath());
        return (fullId != null && TARGET_WHITELIST.contains(fullId))
                || (path != null && TARGET_WHITELIST.contains(path));
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
