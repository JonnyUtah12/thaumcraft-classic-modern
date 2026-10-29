package com.jonnyutah12.thaumcraftclassic.tiles;

import com.jonnyutah12.thaumcraftclassic.blocks.BlockDeviceBase;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.api.entities.IEldritchMob;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Eldritch obelisk (content-mapping.csv {@code thaumcraft:tileeldritchobelisk},
 * hosted on blockEldritch variant 1).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileEldritchObelisk}. Every 20
 * ticks it scans a 6-cell radius for living entities that implement
 * {@link IEldritchMob} and, on any that do not already carry slowness, applies
 * slowness and weakness for 40 ticks at amplifier 0 (the original's
 * "ambient" flag has no 1.21.1 constructor equivalent, so the port uses the
 * plain instance — a cosmetic particle/icon difference). The obelisk is the
 * Outer Lands' defence against its own monsters: the census is
 * {@code IEldritchMob}, never players.</p>
 *
 * <p>Deferred (TC-M7-02): the client-side half of the original tick draws a
 * wisp beam from the obelisk top to every eldritch mob in range, and the
 * 5-cell render bounding box (the obelisk model is 1×5) is render
 * territory.</p>
 */
public class TileEldritchObelisk extends BlockEntity implements BlockDeviceBase.TickingDeviceTile {

    private int counter = 0;

    public TileEldritchObelisk(final BlockPos pos, final BlockState state) {
        super(CatalogEldritchRealm.TILE_ELDRITCH_OBELISK.get(), pos, state);
    }

    public int counter() {
        return counter;
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (counter % EldritchRealmValues.OBELISK_TICK_PERIOD != 0) {
            counter++;
            return;
        }
        counter++;
        for (final LivingEntity entity : eldritchMobsInRange()) {
            if (entity.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    EldritchRealmValues.OBELISK_EFFECT_TICKS, EldritchRealmValues.OBELISK_EFFECT_AMPLIFIER));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    EldritchRealmValues.OBELISK_EFFECT_TICKS, EldritchRealmValues.OBELISK_EFFECT_AMPLIFIER));
        }
    }

    /** The original's {@code EntityUtils.getEntitiesInRange(..., EntityLivingBase.class, 6.0)} filtered to eldritch mobs. */
    public List<LivingEntity> eldritchMobsInRange() {
        final double r = EldritchRealmValues.OBELISK_RANGE;
        final AABB box = new AABB(worldPosition.getX() + 0.5 - r, worldPosition.getY() - r, worldPosition.getZ() + 0.5 - r,
                worldPosition.getX() + 0.5 + r, worldPosition.getY() + r, worldPosition.getZ() + 0.5 + r);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                TileEldritchObelisk::isEldritch).stream().toList();
    }

    private static boolean isEldritch(final Entity entity) {
        return entity instanceof IEldritchMob;
    }
}
