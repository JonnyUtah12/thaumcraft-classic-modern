package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.api.entities.ITaintedMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Port of {@code thaumcraft.common.entities.monster.EntityTaintSpider}
 * (TC-M6-06-s-b). The original extends {@code EntitySpider} and keeps the
 * vanilla goal table (its constructor touches no AI rows), so the port
 * extends 1.21.1's {@code Spider}: hitbox 0.4x0.3, eye height 0.1 (the
 * 1.7.10 {@code func_70105_a(0.4F, 0.3F)} ctor line, eye height 0.1F from
 * the {@code func_70053_R} override), health 5.0, attack 2.0, xp 2 ({@code
 * func_110147_ax} + ctor lines).
 */
public class EntityTaintSpider extends Spider implements ITaintedMob {

    /** {@code field_70728_aV = 2} (ctor line 18): a REAL xp override. */
    private static final int TAINT_SPIDER_XP = 2;

    public EntityTaintSpider(final EntityType<? extends EntityTaintSpider> type, final Level level) {
        super(type, level);
        this.xpReward = TAINT_SPIDER_XP; // ctor line 18
    }

    /**
     * {@code func_110147_ax} (lines 21-25): {@code super} first (the vanilla
     * spider set), then max health 5.0 and attack 2.0 — the movement speed
     * 0.3 and follow range 16 stay the vanilla spider values.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    /** func_70647_i (getSoundPitch) → 1.21.1's getVoicePitch (lines 27-29): 0.7. */
    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    /** The original's {@code field_70728_aV} (ctor line 18), for the probes. */
    public int taintSpiderXp() {
        return TAINT_SPIDER_XP;
    }
}
