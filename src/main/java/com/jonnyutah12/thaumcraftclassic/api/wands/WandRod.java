package com.jonnyutah12.thaumcraftclassic.api.wands;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import net.minecraft.resources.ResourceLocation;

/**
 * A wand rod (TC-M6-01): port of {@code thaumcraft.api.wands.WandRod} /
 * {@code StaffRod} ({@code api/wands/WandRod.java}, {@code StaffRod.java}).
 *
 * <p>Original fields preserved 1:1: {@code tag}, {@code capacity},
 * {@code craftCost}, {@code glow} ({@link #glowing()}) and the built texture
 * path. {@code StaffRod} is the original's subclass whose constructor prefixes
 * the tag with {@code "_staff"}; the port carries that as {@link #staff()}
 * instead of a second class (the only behavioural difference the original
 * encoded by class was {@code instanceof StaffRod}, plus the runes flag).</p>
 *
 * <p>{@code getOnUpdate()}: the original installed
 * {@code WandRodPrimalOnUpdate} on six elemental rods + their staff cores
 * (fixed primal) and on the staff core of the primal (a random primal). The
 * port encodes both shapes as {@link #primalAspectTag()} (nullable, the fixed
 * primal) and {@link #primalTickAllPrimals()} (the no-argument constructor),
 * and {@code ItemWandCasting.inventoryTick} runs the tick body
 * ({@code WandRodPrimalOnUpdate.java}).</p>
 *
 * <p>The original's {@code ItemStack item} field becomes {@link #itemId()} +
 * {@link #meta()} (see {@link WandCap}); the item is {@code minecraft:stick}
 * for the plain wood rod, exactly as {@code ConfigItems} registered it.</p>
 */
public record WandRod(String tag,
                      int capacity,
                      int craftCost,
                      ResourceLocation itemId,
                      int meta,
                      boolean staff,
                      boolean glowing,
                      boolean runes,
                      String primalAspectTag,
                      boolean primalTickAllPrimals) {

    /** Original {@code WandRod.getResearch()}: {@code "ROD_" + tag}. */
    public String research() {
        return "ROD_" + this.tag;
    }

    /**
     * The tag with the {@code StaffRod} suffix stripped — the original's
     * display/research base tag ({@code func_77653_i} strips {@code "_staff"},
     * {@code ItemWandCasting.java:121-123}).
     */
    public String baseTag() {
        return this.staff ? this.tag.substring(0, this.tag.length() - "_staff".length()) : this.tag;
    }

    /**
     * Original {@code getTexture()}: a staff core keeps its base rod's texture
     * ({@code StaffRod}'s constructors overwrite the path with the
     * un-suffixed tag, {@code StaffRod.java:12,26}), repathed for 1.21.1
     * (see {@link WandCap#texture()}).
     */
    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID,
                "models/wand_rod_" + this.baseTag());
    }

    /** Original {@code getOnUpdate() != null}: whether this rod tops up vis on the inventory tick. */
    public boolean hasOnUpdate() {
        return this.primalAspectTag != null || this.primalTickAllPrimals;
    }

    /** The fixed primal an on-update rod tops up, or null (original {@code WandRodPrimalOnUpdate.aspect}). */
    public Aspect primalAspect() {
        return this.primalAspectTag == null ? null : Aspect.getAspect(this.primalAspectTag);
    }
}
