package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data component registration holder (TC-M2-01 registration architecture).
 *
 * <p>Replaces the original's ad-hoc ItemStack NBT keys (wand charge, vis
 * storage, research hints, ...) with typed {@code DataComponentType}s. The
 * component id set is defined by the feature milestones that need them
 * (M4+); this holder exists so component registration is wired before any
 * item construction reads a component (NeoForge orders
 * {@code data_component_type} before items for exactly that reason).</p>
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ThaumcraftClassic.MOD_ID);

    /**
     * Essentia contents of a stack (TC-M4-01): replaces the original
     * {@code IEssentiaContainerItem} ItemStack NBT key "Aspects" with a typed
     * component. Persistent (the original wrote it to stack NBT) and synced
     * for the aspect-tinted rendering the original read client-side
     * (docs/aspects.md).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EssentiaContainer>> ESSENTIA_CONTAINER =
            DATA_COMPONENT_TYPES.register("essentia_container",
                    () -> DataComponentType.<EssentiaContainer>builder()
                            .persistent(EssentiaContainer.CODEC)
                            .networkSynchronized(EssentiaContainer.STREAM_CODEC)
                            .build());

    /**
     * Vis charge carried by a stack (TC-M4-02): replaces the original's
     * per-aspect centi-vis {@code NBTTagInt} keys written on wand/amulet
     * stacks by {@code ItemWandCasting.storeVis}/{@code getVis}. Capacity is
     * not stored — it derives from the rod component via
     * {@code WandVis.maxVis}; the rod/cap components arrive with TC-M6-01.
     * Synced to the client for the charge tooltip the original rendered from
     * the same NBT. Semantics: docs/architecture/vis.md.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VisPool>> VIS_POOL =
            DATA_COMPONENT_TYPES.register("vis_pool",
                    () -> DataComponentType.<VisPool>builder()
                            .persistent(VisPool.CODEC)
                            .networkSynchronized(VisPool.STREAM_CODEC)
                            .build());

    /**
     * Original damage value of a multi-meta item (TC-M6-04a convention,
     * first used by ItemResource; TC-M8-02 baubles carry their original metas
     * the same way). 1.21.1 has no damage-value variants, so the meta travels
     * on the stack; {@code new_id} stays one registered object per
     * content-mapping.csv row (docs/architecture/registration.md).
     *
     * <p>TC-M6-08-s widened the range from 0..63 to the original's full
     * damage-value domain: {@code ItemGolemCore}'s blank core is metadata
     * <b>100</b> (the {@code golem_core_blank} icon and the crucible row
     * {@code recipe.42.crucible}'s catalyst) and the original's "any metadata"
     * wildcard is <b>32767</b> ({@code ResearchManager.META_ANY}; the
     * ADVANCEDGOLEM research icon's {@code ItemGolemPlacer} meta). The shipped
     * data files already carry those values, so the narrower codec rejected
     * rows that used to gate out on an unregistered item.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_META =
            DATA_COMPONENT_TYPES.register("item_meta",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 32767))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /**
     * The aspect a jar label / filled jar locks its jar to (TC-M4-06-s): the
     * original wrote the label's tag string to the {@code AspectFilter} key on
     * the stack's NBT ({@code ItemJarFilled.getFilter},
     * {@code BlockJar.getDrops}); 1.21.1 gives it its own component so the
     * contents ({@link #ESSENTIA_CONTAINER}) and the label stay separate.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> JAR_FILTER =
            DATA_COMPONENT_TYPES.register("jar_filter",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /**
     * Dye colour of a hanging banner carried by its stack (TC-M4-03 fix): the
     * original wrote {@code banner.field_77990_d.func_74774_a("color", (byte)a)}
     * on the banner ItemStack inside {@code ConfigRecipes.initializeArcaneRecipes}
     * ({@code ConfigRecipes.java:750-752}, the {@code for (int a = 0; a < 16; a++)}
     * loop that registers {@code Banner_0..Banner_15}), and read it back at
     * render/placement time ({@code TileBanner.java:48}
     * {@code this.setColor(nbttagcompound.func_74771_c("color"))} and
     * {@code ItemBannerRenderer.java:39}). The port keeps that value as a typed
     * component so the recipe rows (data {@code recipe.112.arcane_shaped.<0..15>})
     * decode and the colour survives into the banner the player places.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BANNER_COLOR =
            DATA_COMPONENT_TYPES.register("banner_color",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 15))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /**
     * Runic hardening bonus (TC-M8-02): the original's {@code "RS.HARDEN"}
     * ItemStack NBT int read by {@code EventHandlerRunic.getFinalCharge}
     * ({@code common/lib/events/EventHandlerRunic.java:347-350}); written by
     * the Runic Augmentation flow (TC-M6-07), summed into runic shield
     * capacity by {@code RunicShielding.finalCharge}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RUNIC_HARDENING =
            DATA_COMPONENT_TYPES.register("runic_hardening",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    private ModDataComponents() {
    }
}
