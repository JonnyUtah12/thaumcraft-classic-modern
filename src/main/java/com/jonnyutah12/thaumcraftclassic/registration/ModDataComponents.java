package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.EssentiaContainer;
import com.jonnyutah12.thaumcraftclassic.foci.FocusPouchContents;
import com.jonnyutah12.thaumcraftclassic.foci.FocusUpgradeList;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
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
     * <p>The range widened from {@code 0..63} to {@code 0..1023} in TC-M6-01:
     * the wand rod item's original damage values reach 100 (the primal staff
     * core, {@code ItemWandRod.func_150895_a}) and a wand's damage value is
     * {@code capCraftCost × rodCraftCost} (up to 9 × 32, ×1.5 for a sceptre,
     * {@code ArcaneWandRecipe}/{@code ArcaneSceptreRecipe}). Widening a
     * persistent codec's accepted range is additive: every value the previous
     * bound accepted still encodes and decodes identically.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_META =
            DATA_COMPONENT_TYPES.register("item_meta",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.intRange(0, 1023))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build());

    /**
     * The rod a wand stack is built from (TC-M6-01): the original's
     * {@code "rod"} ItemStack NBT key written by
     * {@code ItemWandCasting.setRod} ({@code common/items/wands/
     * ItemWandCasting.java:499-500}, a {@code NBTTagString} of the rod tag).
     * An absent component resolves to the wood rod, exactly as the original's
     * {@code getRod} did.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WAND_ROD =
            DATA_COMPONENT_TYPES.register("wand_rod",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /**
     * The cap a wand stack is built from (TC-M6-01): the original's
     * {@code "cap"} NBT key ({@code ItemWandCasting.setCap}, line 522-524).
     * An absent component resolves to the iron cap, as in the original's
     * {@code getCap}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WAND_CAP =
            DATA_COMPONENT_TYPES.register("wand_cap",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /**
     * The sceptre marker (TC-M6-01): the original wrote a
     * {@code NBTTagByte} under the {@code "sceptre"} key on the wand stack
     * ({@code ArcaneSceptreRecipe.getCraftingResult},
     * {@code ItemWandCasting.java:101-102}) and {@code isSceptre} tested the
     * key's <em>presence</em> ({@code func_74764_b("sceptre")}, line 490-491) —
     * so a present-but-false value still reads as a sceptre, and the port
     * preserves that by always writing {@code TRUE} and testing presence.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WAND_SCEPTRE =
            DATA_COMPONENT_TYPES.register("wand_sceptre",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build());

    /**
     * The focus a wand stack carries (TC-M6-02): the original wrote the whole
     * focus {@code ItemStack} into the wand's NBT under the {@code "focus"} key
     * ({@code ItemWandCasting.setFocus}, {@code common/items/wands/
     * ItemWandCasting.java:471-477}) and read it back with {@code getFocusItem}
     * (lines 462-469).
     *
     * <p>The port stores the focus's <em>tag</em> — the focus's registered
     * identity — exactly as {@link #WAND_ROD}/{@link #WAND_CAP} store their
     * component tags, and rebuilds the stack on demand. A data component cannot
     * hold an {@code ItemStack}: vanilla validates that a component value
     * implements {@code equals}/{@code hashCode} and {@code ItemStack} does not
     * ("Data components must implement equals and hashCode", the reason vanilla
     * wraps stacks in {@code ItemContainerContents}). Focus <em>upgrades</em>
     * are the only per-stack state a focus carries, and they are TC-M6-03-s, so
     * the tag is the whole of what this slice needs. An absent component is the
     * original's {@code null} focus.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> WAND_FOCUS =
            DATA_COMPONENT_TYPES.register("wand_focus",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /**
     * The contents of a focus pouch (TC-M6-02): the original's
     * {@code "Inventory"} {@code NBTTagList} of {@code {Slot: byte, ...item}}
     * compounds ({@code ItemFocusPouch.getInventory}/{@code setInventory},
     * {@code common/items/wands/ItemFocusPouch.java:63-93}), kept as a
     * positional bounded list so slot indices survive the save path.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FocusPouchContents>> FOCUS_POUCH_CONTENTS =
            DATA_COMPONENT_TYPES.register("focus_pouch_contents",
                    () -> DataComponentType.<FocusPouchContents>builder()
                            .persistent(FocusPouchContents.CODEC)
                            .networkSynchronized(FocusPouchContents.STREAM_CODEC)
                            .build());

    /**
     * The upgrades applied to a focus stack (TC-M6-03-s): the original's
     * {@code "upgrade"} {@code NBTTagList} of {@code {id: short}} compounds
     * ({@code ItemFocusBasic.getAppliedUpgrades}/{@code setFocusUpgradeTagList},
     * {@code reference/decompiled/…/api/wands/ItemFocusBasic.java:129-207}), kept
     * as the original's fixed five rank slots.
     *
     * <p>A component does not have to be persistent to be useful, but this one is
     * the focus's only per-stack state and the original persisted it, so it is
     * persisted and synced (the client renders the upgrade tooltip).</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FocusUpgradeList>> FOCUS_UPGRADES =
            DATA_COMPONENT_TYPES.register("focus_upgrades",
                    () -> DataComponentType.<FocusUpgradeList>builder()
                            .persistent(FocusUpgradeList.CODEC)
                            .networkSynchronized(FocusUpgradeList.STREAM_CODEC)
                            .build());

    /**
     * The applied upgrades of the focus a wand carries (TC-M6-03-s).
     *
     * <p>The original's wand stored the focus <em>stack</em> under its
     * {@code "focus"} NBT key ({@code ItemWandCasting.setFocus},
     * {@code ItemWandCasting.java:471-477}), so the focus's upgrade tag list
     * travelled with it. The port's {@link #WAND_FOCUS} stores the focus's tag
     * (a component cannot hold an {@code ItemStack}), and rebuilds the stack on
     * demand — which would drop the upgrades. This component carries them across
     * the rebuild, so a fitted focus keeps its upgrades exactly as the original's
     * stored stack did.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FocusUpgradeList>> WAND_FOCUS_UPGRADES =
            DATA_COMPONENT_TYPES.register("wand_focus_upgrades",
                    () -> DataComponentType.<FocusUpgradeList>builder()
                            .persistent(FocusUpgradeList.CODEC)
                            .networkSynchronized(FocusUpgradeList.STREAM_CODEC)
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
