package com.jonnyutah12.thaumcraftclassic.api.damagesource;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * Thaumcraft damage sources (TC-M6-06-s-e).
 *
 * <p>The 1.7.10 original lives at
 * {@code reference/decompiled/thaumcraft/thaumcraft/api/damagesource/DamageSourceThaumcraft.java}
 * and hands out pre-built {@code DamageSource} constants. 1.21.1 builds a source
 * from a {@link DamageType} registry entry plus a causer, so the constant here is
 * a {@link ResourceKey} into {@code data/thaumcraft/damage_type/<path>.json} —
 * the shipped row is {@code data/thaumcraft/damage_type/tentacle.json}
 * ({@code message_id "tentacle"}, {@code scaling when_caused_by_living_non_player},
 * {@code exhaustion 0.1}; the scaling name is the NeoForge 21.1 spelling, see the
 * 6172ac7 fix note on the parent card).
 *
 * <p>Only the tentacle source is in this slice: it is the one the taintacle family
 * deals ({@code EntityTaintacle.java:182} — {@code DamageSourceThaumcraft.causeTentacleDamage(this)}).
 * The remaining 1.7.10 sources in that class (demon, wand, boom, …) keep their own
 * catalog rows and are NOT declared here so a later card cannot be accused of
 * shadowing an id it has not implemented.
 */
public final class DamageSourceThaumcraft {

    /** {@code thaumcraft:tentacle} — the taintacle's melee damage type. */
    public static final ResourceKey<DamageType> TENTACLE =
            ResourceKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "tentacle"));

    private DamageSourceThaumcraft() {
    }

    /**
     * 1.7.10 {@code DamageSourceThaumcraft.causeTentacleDamage(Entity)}.
     *
     * @param causer the taintacle that landed the hit
     */
    public static DamageSource causeTentacleDamage(final Entity causer) {
        return new DamageSource(
                causer.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TENTACLE),
                causer,
                causer);
    }
}
