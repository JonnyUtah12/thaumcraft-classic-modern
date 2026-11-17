package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import com.jonnyutah12.thaumcraftclassic.taint.TaintBiomes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintVillager}
 * (TC-M6-06-s-b). Health 30.0 / attack 4.0 / speed 0.3 ({@code
 * func_110147_ax} lines 58-62: 30.0 line 60, 4.0 line 61, 0.3 line 62);
 * the two vanilla-villager ctor calls copied
 * verbatim — {@code func_75498_b(true)} (line 42, setCanPassDoors) and
 * {@code func_75491_a(true)} (line 43, setCanOpenDoors) — and NO swim
 * flag; the goal table is the eleven-row villager table including the
 * {@code EntityAIWatchClosest2} rows (probabilities 1.0 / 0.02).
 *
 * <p>Village refresh (the {@code func_70629_bd} body, decompiled lines
 * 85-96): every {@code 70 + nextInt(50)} ticks the original asks
 * {@code Village.getVillageWithin(this, 32, ...)} and pins the mob's
 * movement restriction to {@code 0.6 * village radius}. 1.21.1 removed the
 * village entity; the port maps the rule onto the bed POI net (the
 * documented deviation): scan radius 32, restriction = the scanned radius
 * scaled by 0.6, falling back to the vanilla null-village branch
 * (center = current position, radius 16) when no bed POI is in range.</p>
 */
public class EntityTaintVillager extends EntityTaintedAnimalBase {

    private int villageRefreshDivider = 70;

    public EntityTaintVillager(final EntityType<? extends EntityTaintVillager> type, final Level level) {
        super(type, level);
        // ctor lines 42-43: func_75498_b(true) (setCanPassDoors) and
        // func_75491_a(true) (setCanOpenDoors) — the vanilla villager pair,
        // copied verbatim; the original sets NO swim flag. 1.21.1 moved both
        // door flags onto GroundPathNavigation.
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanPassDoors(true);
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintedAnimalAiLogic.VILLAGER.health())
                .add(Attributes.ATTACK_DAMAGE, TaintedAnimalAiLogic.VILLAGER.attack())
                .add(Attributes.MOVEMENT_SPEED, TaintedAnimalAiLogic.VILLAGER.movementSpeed());
    }

    @Override
    protected void customServerAiStep() {
        // func_70629_bd (village refresh, body lines 85-96): divider 70 + nextInt(50)
        if (this.tickCount > this.villageRefreshDivider) {
            this.villageRefreshDivider = this.tickCount
                    + TaintedAnimalAiLogic.villageRefreshDivider(this.getRandom().nextInt(50));
            refreshVillageRestriction();
        }
        super.customServerAiStep();
    }

    private void refreshVillageRestriction() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        final BlockPos center = this.blockPosition();
        int count = 0;
        double sumX = 0.0D;
        double sumZ = 0.0D;
        // 1.21.1 has no Village entity; the nearest surviving analogue of the
        // original's village net is the bed POI set (PoiTypes.HOME is the bed
        // POI in 1.21.1), scanned over the same 32-block radius.
        try (var records = level.getPoiManager().getInRange(
                holder -> holder.is(PoiTypes.HOME), center,
                TaintedAnimalAiLogic.VILLAGE_SCAN_RADIUS, PoiManager.Occupancy.ANY)) {
            for (final PoiRecord record : records.toList()) {
                count++;
                sumX += record.getPos().getX();
                sumZ += record.getPos().getZ();
            }
        }
        if (count == 0) {
            this.restrictTo(center, 16);
            return;
        }
        // the original: setRestriction(villageCenter, (int)(radius * 0.6F))
        final int radius = Math.max(1, (int) (TaintedAnimalAiLogic.VILLAGE_SCAN_RADIUS
                * TaintedAnimalAiLogic.VILLAGE_RESTRICTION_FACTOR));
        final BlockPos villageCenter = new BlockPos(
                (int) (sumX / count), center.getY(), (int) (sumZ / count));
        this.restrictTo(villageCenter, radius);
    }

    @Override
    protected List<GoalSpec> goalTable() {
        return TaintedAnimalAiLogic.villagerGoals();
    }

    @Override
    protected List<GoalSpec> targetTable() {
        return TaintedAnimalAiLogic.villagerTargetGoals();
    }

    // The original permutes the slots: func_70639_aQ (hurt) lines 139-140
    // "mob.villager.idle" (= 1.21.1 VILLAGER_AMBIENT), func_70621_aR
    // (death) lines 143-144 "mob.villager.hit" (= 1.21.1 VILLAGER_HURT),
    // func_70673_aS (living) lines 147-148 "mob.villager.death"
    // (= 1.21.1 VILLAGER_DEATH).
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_DEATH; // "mob.villager.death" (147-148)
    }

    @Override
    protected SoundEvent getHurtSound(final net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.VILLAGER_AMBIENT; // "mob.villager.idle" (139-140)
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_HURT; // "mob.villager.hit" (143-144)
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch (lines 151-152): 0.7. */
    @Override
    public float getVoicePitch() {
        return TaintedAnimalAiLogic.villagerSoundPitch();
    }
}
