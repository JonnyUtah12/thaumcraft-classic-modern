package com.jonnyutah12.thaumcraftclassic.api.damagesource;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Thaumcraft damage sources (TC-M6-05; original
 * {@code thaumcraft.api.damagesource.DamageSourceThaumcraft}).
 *
 * <p>The original taint source is {@code new DamageSourceThaumcraft("taint")
 * .setDamageBypassesArmor().setMagicDamage()}. In 1.21.1 damage types are
 * datapack content: the equivalent is the magic-effect damage type
 * {@code thaumcraft:taint} (data/thaumcraft/damage_type/taint.json), which
 * bypasses armor and counts as magic damage exactly like the original flags.
 * Only the taint source is ported here; the original's other sources
 * (primal, focus, …) belong to the milestones that use them.</p>
 */
public final class DamageSourcesThaumcraft {

    public static final ResourceKey<DamageType> TAINT = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "taint"));

    /** The original {@code DamageSourceThaumcraft.taint}. */
    public static DamageSource taint(final Level level) {
        return new DamageSource(level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(TAINT));
    }

    private DamageSourcesThaumcraft() {
    }
}
