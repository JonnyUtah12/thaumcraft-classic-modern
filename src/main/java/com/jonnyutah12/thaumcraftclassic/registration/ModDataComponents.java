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
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_META =
            DATA_COMPONENT_TYPES.register("item_meta",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 63))
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
