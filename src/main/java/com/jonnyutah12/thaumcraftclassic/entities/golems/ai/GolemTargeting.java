package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

/**
 * The golem's own target filter — the port of {@code EntityGolemBase.isValidTarget}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/golems/EntityGolemBase.java:1206-1258}).
 *
 * <p>{@code // SEAM: isValidTarget lives on EntityGolemBase in the original} — the
 * runtime class in this tree ({@code entities/golems/EntityGolemBase.java}) has no
 * such method and the port may not edit it (card rule 1), so the verbatim port of
 * that method lives here and the combat goals call it. The parent can move it onto
 * the entity later without touching a goal body.</p>
 *
 * <h2>The numbers and gates this pins (original line by line)</h2>
 * <ul>
 *   <li>alive check — {@code target.func_70089_S()} (line 1207) → {@code isAlive()};</li>
 *   <li>the owner is never a target: {@code target.getCommandUsername().equals(getOwnerName())}
 *       (line 1209) → {@code Player.getGameProfile().getName()};</li>
 *   <li>the home-range gate: {@code !this.func_110176_b(floor(x), floor(y), floor(z))}
 *       (lines 1210-1214) → {@link EntityGolemBase#isWithinRange(BlockPos)} (the port's
 *       own {@code home.distanceSq(pos) < range²});</li>
 *   <li>core 9 (Butcher, lines 1216-1226): animals only, not hostiles, not tamed,
 *       not a golem, and not an animal in love ({@code func_70631_g_()});</li>
 *   <li>every other core (lines 1227-1252): creepers need
 *       {@code canAttackCreepers() && getUpgradeAmount(4) > 0}; hostiles need only
 *       {@code canAttackHostiles()}; animals and players need
 *       {@code canAttackX() && getUpgradeAmount(4) > 0} — the type-4 ("Effect")
 *       upgrade is what lets a Guard golem hit anything beyond hostiles.</li>
 * </ul>
 */
final class GolemTargeting {

    /** Original {@code getUpgradeAmount(4)} — the upgrade that unlocks the non-hostile targets. */
    private static final int EFFECT_UPGRADE = 4;

    private GolemTargeting() {
    }

    /**
     * The original's {@code isValidTarget(Entity)} ({@code EntityGolemBase.java:1206}).
     * The original's parameter is an {@code Entity}; every branch of it keys off
     * {@code EntityLivingBase} subtypes, so a non-living entity can never pass and the
     * port narrows the parameter to {@code LivingEntity} (a {@code null} target fails,
     * exactly as the original's {@code func_70089_S()} check failed for {@code null}).
     */
    static boolean isValidTarget(final EntityGolemBase golem, final LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target instanceof Player player && player.getGameProfile().getName().equals(golem.getOwnerName())) {
            return false;
        }
        if (!golem.isWithinRange(target.blockPosition())) {
            return false;
        }

        if (golem.getCore() == GolemTargeting.BUTCHER_CORE) {
            // Original lines 1216-1226: the Butcher eats animals, and nothing else.
            if (isAnimal(target) && !(target instanceof Monster) && !isTamed(target) && !(target instanceof AbstractGolem)) {
                // Original lines 1222-1224: an animal in love is left alone.
                // DEVIATION: isInLove() lives on Animal in 1.21.1, not on the AgeableMob root
                // the 1.7.10 check ran against, so a non-Animal AgeableMob has no love state to test.
                return target instanceof Animal animal ? !animal.isInLove() : true;
            }
            return false;
        }

        // Original line 1228: a creeper is only a target with the Effect upgrade and the
        // creeper toggle — the golem must not suicide on a swelling creeper by default.
        if (golem.canAttackCreepers() && golem.getUpgradeAmount(EFFECT_UPGRADE) > 0 && target instanceof Creeper) {
            return true;
        }
        // Original lines 1232-1234: (EntityMob || IMob) && !EntityCreeper.
        if (golem.canAttackHostiles() && isHostile(target) && !(target instanceof Creeper)) {
            return true;
        }
        // Original lines 1236-1242: animals need the toggle AND the Effect upgrade.
        if (golem.canAttackAnimals() && golem.getUpgradeAmount(EFFECT_UPGRADE) > 0
                && isAnimal(target) && !(target instanceof Monster) && !isTamed(target)
                && !(target instanceof AbstractGolem)) {
            return true;
        }
        // Original lines 1245-1247: players need the toggle AND the Effect upgrade.
        return golem.canAttackPlayers() && golem.getUpgradeAmount(EFFECT_UPGRADE) > 0 && target instanceof Player;
    }

    /** The Butcher core id ({@code GolemCoreTask.BUTCHER}, original {@code case 9}). */
    private static final int BUTCHER_CORE = 9;

    /**
     * Original {@code target instanceof EntityAnimal || target instanceof IAnimals}
     * (lines 1218, 1238-1239). {@code // DEVIATION: IAnimals has no 1.21.1 interface}
     * — the closest equivalent is {@code AgeableMob}, the 1.21.1 root of every animal
     * (it covers {@code Animal}, the breeding {@code AgeableMob} types and the water
     * animals), so
     * the handful of 1.7.10 {@code IAnimals} implementations outside the animal tree
     * (a few hostile hybrids) are no longer read as animals here.
     */
    private static boolean isAnimal(final LivingEntity target) {
        // DEVIATION: IAnimals has no 1.21.1 interface; AgeableMob is the animal root, so
        // 1.7.10 IAnimals implementors outside the animal tree are no longer read as animals.
        return target instanceof AgeableMob;
    }

    /**
     * Original {@code target instanceof EntityMob || target instanceof IMob}
     * (line 1232). {@code // DEVIATION: IMob has no 1.21.1 interface} — {@code Monster}
     * is its 1.21.1 replacement (the hostile-mob root), so the small set of 1.7.10
     * {@code IMob} implementors outside the {@code Monster} tree (bee, polar bear, …)
     * is no longer a Guard target.
     */
    private static boolean isHostile(final LivingEntity target) {
        // DEVIATION: IMob has no 1.21.1 interface; Monster is the hostile-mob root, so the
        // 1.7.10 IMob implementors outside that tree (bee, polar bear, ...) are not targets.
        return target instanceof Monster;
    }

    /** Original {@code target instanceof EntityTameable && ((EntityTameable)target).func_70909_n()} (lines 1219, 1239). */
    private static boolean isTamed(final LivingEntity target) {
        return target instanceof TamableAnimal tameable && tameable.isTame();
    }
}
