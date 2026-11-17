package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.entities.monster.TaintedAnimalAiLogic.GoalSpec;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintSheep}
 * (TC-M6-06-s-b). Box 0.9x1.3 ({@code func_70105_a}, ctor line 34), health
 * 20.0 / attack 3.0 / speed 0.25 ({@code func_110147_ax} lines 50-52),
 * armour 1 ({@code func_70658_aO} lines 63-64); NO {@code field_70728_aV}
 * is set, so no {@code xpReward} is invented. The goal table adds the
 * {@code AIConvertGrass} row (priority 2, ctor line 37) — the sheep is the
 * taint spreader's animal arm. Shearing: the original carries vanilla's
 * {@code getSheared}/{@code setSheared} (data byte slot 16, bit 16) and the
 * {@code "Sheared"} NBT tag, but NEVER shorn — it extends {@code EntityMob},
 * so the vanilla {@code ItemShears} path ({@code instanceof EntitySheep})
 * never fires, and its {@code func_70085_c} interact hook (lines 131-133)
 * is super-only. The port keeps that state dormant (no shear trigger, as in
 * the original).
 */
public class EntityTaintSheep extends EntityTaintedAnimalBase {

    private static final EntityDataAccessor<Byte> DATA_SHEARED =
            SynchedEntityData.defineId(EntityTaintSheep.class, EntityDataSerializers.BYTE);

    public EntityTaintSheep(final EntityType<? extends EntityTaintSheep> type, final Level level) {
        super(type, level);
        // ctor line 34: func_70105_a(0.9F, 1.3F); no field_70728_aV is set by
        // the original, so no xpReward is invented here
        // func_75491_a(true) (line 35): the taint sheep opens doors
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, TaintedAnimalAiLogic.SHEEP.health())
                .add(Attributes.ATTACK_DAMAGE, TaintedAnimalAiLogic.SHEEP.attack())
                .add(Attributes.MOVEMENT_SPEED, TaintedAnimalAiLogic.SHEEP.movementSpeed())
                .add(Attributes.ARMOR, TaintedAnimalAiLogic.SHEEP.armor());
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHEARED, (byte) 0);
    }

    public boolean isSheared() {
        return (this.entityData.get(DATA_SHEARED) & 16) != 0;
    }

    public void setSheared(final boolean sheared) {
        final byte b = this.entityData.get(DATA_SHEARED);
        this.entityData.set(DATA_SHEARED, (byte) (sheared ? b | 16 : b & -17));
    }

    @Override
    public void addAdditionalSaveData(final CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Sheared", this.isSheared());
    }

    @Override
    public void readAdditionalSaveData(final CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSheared(tag.getBoolean("Sheared"));
    }

    @Override
    protected List<GoalSpec> goalTable() {
        return TaintedAnimalAiLogic.sheepGoals();
    }

    @Override
    protected List<GoalSpec> targetTable() {
        return TaintedAnimalAiLogic.sheepTargetGoals();
    }

    // The original returns the SAME string "mob.sheep.say" in all three
    // slots — func_70639_aQ (hurt) lines 145-146, func_70673_aS (living)
    // lines 149-150, func_70621_aR (death) lines 153-154: the sheep has no
    // hurt/death voice, the ambient plays everywhere.
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHEEP_AMBIENT; // "mob.sheep.say" (149-150)
    }

    @Override
    protected SoundEvent getHurtSound(final net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.SHEEP_AMBIENT; // "mob.sheep.say" (145-146)
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_AMBIENT; // "mob.sheep.say" (153-154)
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch (lines 161-162): 0.7. */
    @Override
    public float getVoicePitch() {
        return TaintedAnimalAiLogic.sheepSoundPitch();
    }
}
