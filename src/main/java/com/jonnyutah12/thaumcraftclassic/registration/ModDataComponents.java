package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.research.ResearchNoteData;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.mojang.serialization.Codec;
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
     * Original ItemStack damage-value metadata for the multi-meta item
     * families ItemResource / ItemShard / ItemNugget (TC-M6-04a). 1.21.1
     * items have no damage-value variants, so the original meta travels on
     * the stack in this component; the family items default it to 0
     * ({@code Item.Properties#component}). Per-meta display names and
     * variant-level ore-dictionary membership key off this value — see the
     * oredict section of docs/architecture/registration.md. Multi-meta
     * BLOCK items instead use the vanilla {@code minecraft:block_state}
     * component (VariantAwareBlockItem).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_META =
            DATA_COMPONENT_TYPES.register("item_meta",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 63))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /**
     * Essentia carried by a stack (TC-M6-04a): replaces the original's
     * {@code "Aspects"} ItemStack NBT subcompound (IEssentiaContainerItem).
     * Synced to the client for the aspect-tinted item colour the original
     * read from the same NBT.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EssentiaContainer>> ESSENTIA_CONTAINER =
            DATA_COMPONENT_TYPES.register("essentia_container",
                    () -> DataComponentType.<EssentiaContainer>builder()
                            .persistent(EssentiaContainer.CODEC)
                            .networkSynchronized(EssentiaContainer.STREAM_CODEC)
                            .build());

    /**
     * Research note state carried by a research-notes stack (TC-M3-04):
     * replaces the original's {@code key}/{@code color}/{@code complete}/
     * {@code copies}/{@code hexgrid} ItemStack NBT written by
     * {@code ResearchManager.writeNote}. The codec reproduces the original
     * tag layout exactly (see {@code ResearchNoteData}); the component is
     * network-synced because the research-table GUI renders the grid from it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResearchNoteData>> RESEARCH_NOTE =
            DATA_COMPONENT_TYPES.register("research_note",
                    () -> DataComponentType.<ResearchNoteData>builder()
                            .persistent(ResearchNoteData.CODEC)
                            .networkSynchronized(ResearchNoteData.STREAM_CODEC)
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

    private ModDataComponents() {
    }
}
