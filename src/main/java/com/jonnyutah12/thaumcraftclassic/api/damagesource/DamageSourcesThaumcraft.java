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
 * datapack content, so the port ships {@code thaumcraft:taint}
 * (data/thaumcraft/damage_type/taint.json) and puts the armour flag on the
 * vanilla tag {@code #minecraft:bypasses_armor}
 * (data/minecraft/tags/damage_type/bypasses_armor.json) — the same mechanism
 * vanilla's own magic/indirect_magic sources use. 1.21.1 has <b>no</b> magic
 * damage-type tag (DamageTypeTags.java lists no {@code MAGIC}), so the
 * original's {@code setMagicDamage()} flag has no data equivalent; it is the
 * 1.7.10 {@code DamageSource#isMagicDamage()} bit and nothing in the shipped
 * code reads it (BD-19).</p>
 *
 * <p>Only the taint source is ported here; the original's other sources
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
