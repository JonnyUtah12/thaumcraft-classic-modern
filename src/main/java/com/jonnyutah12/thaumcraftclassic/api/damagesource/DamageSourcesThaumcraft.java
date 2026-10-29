package com.jonnyutah12.thaumcraftclassic.api.damagesource;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Thaumcraft damage sources (original
 * {@code thaumcraft.api.damagesource.DamageSourceThaumcraft}; the data side is
 * {@code data/thaumcraft/damage_type/}, TC-M6-04f-s).
 *
 * <p>The original registered its sources as {@code DamageSource} subclasses
 * with flags: {@code taint} = {@code setDamageBypassesArmor() +
 * setMagicDamage()}, {@code tentacle} and {@code swarm} = plain, plus the two
 * static helpers {@code causeTentacleDamage} / {@code causeSwarmDamage} that
 * attach a mob as the killer. In 1.21.1 a damage type is datapack content
 * ({@code data/<ns>/damage_type/<id>.json}); 21.1.259's {@code DamageEffects}
 * has no magic profile (removed in 1.21), so the armour-ignoring bypass of
 * taint/dissolve is applied at the emitter (the Liquid Death contact damage
 * and the taint emitters) while the shipped types keep the hurt profile. The
 * {@code message_id} is {@code thaumcraft.<name>}: 1.21.1 builds the death
 * message key as {@code death.attack.<message_id>}, which lands on the
 * original's {@code death.attack.taint[.player]}-style strings under the mod
 * namespace. Every source keeps the original's food-exhaustion multiplier
 * (1.21.1 {@code exhaustion} 0.1).</p>
 */
public final class DamageSourcesThaumcraft {

    /** Original {@code DamageSourceThaumcraft.taint}. */
    public static final ResourceKey<DamageType> TAINT = key("taint");

    /** Original {@code DamageSourceThaumcraft.dissolve} (Liquid Death). */
    public static final ResourceKey<DamageType> DISSOLVE = key("dissolve");

    /** Original {@code DamageSourceThaumcraft.tentacle} (taintacles, TC-M6-06). */
    public static final ResourceKey<DamageType> TENTACLE = key("tentacle");

    /** Original {@code DamageSourceThaumcraft.swarm} (swarm mobs, TC-M6-06). */
    public static final ResourceKey<DamageType> SWARM = key("swarm");

    /** The original {@code DamageSourceThaumcraft.taint}. */
    public static DamageSource taint(final Level level) {
        return source(level, TAINT);
    }

    /** The original {@code DamageSourceThaumcraft.dissolve}. */
    public static DamageSource dissolve(final Level level) {
        return source(level, DISSOLVE);
    }

    /** Original {@code DamageSourceThaumcraft.causeTentacleDamage(EntityLivingBase)}. */
    public static DamageSource causeTentacleDamage(final Level level, final Entity killer) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TENTACLE), killer);
    }

    /** Original {@code DamageSourceThaumcraft.causeSwarmDamage(EntityLivingBase)}. */
    public static DamageSource causeSwarmDamage(final Level level, final Entity killer) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(SWARM), killer);
    }

    /** A bare source of one of this mod's damage types. */
    public static DamageSource source(final Level level, final ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key));
    }

    private static ResourceKey<DamageType> key(final String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, path));
    }

    private DamageSourcesThaumcraft() {
    }
}
