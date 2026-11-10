package com.jonnyutah12.thaumcraftclassic.casting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.aspect.AspectList;
import com.jonnyutah12.thaumcraftclassic.items.foci.ItemFocus;
import com.jonnyutah12.thaumcraftclassic.items.wands.ItemWandCasting;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import com.jonnyutah12.thaumcraftclassic.blocks.VariantStateItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The focus effect performers (TC-M6-03-p): the handlers
 * {@link SpellEffects#perform} dispatches to, one per effect key, registered
 * by {@link #registerAll()} beside the behavior tables so each focus becomes
 * playable the moment its content exists.
 *
 * <p>Every handler follows the card-wide rule for content that has not
 * landed: it refuses <em>before any state change</em> and returns false, and
 * {@link WandCasting} refunds the cast — an unlanded projectile, warded
 * block, hole block, swapper queue, the cast charges nothing.</p>
 *
 * <ul>
 *   <li><b>The seven projectiles</b> ({@code thaumcraft:ember},
 *       {@code explosiveorb}, {@code shockorb}, {@code frostshard},
 *       {@code pechblast}, {@code primalorb}, {@code firebat}) are TC-M6-06's
 *       entity rows. A registered {@link EntityType} spawns; a class that
 *       implements {@link FocusProjectile} launches with the cast's full
 *       context, the fallback aims from the caster's eyes at the spec's
 *       speed. Count is the original's loop ({@code 2 + potency} embers, one
 *       orb, one bat).</li>
 *   <li><b>Ward</b> — the warding focus ({@code ItemFocusWarding.java:86-154}):
 *       pointing at an owned warded block restores every block it covers —
 *       the original's restore branch has no {@code consumeAllVis} at all, so
 *       the handler performs it and reports the no-charge status and the seam
 *       refunds; pointing at plain solid, non-block-entity ground turns it to
 *       {@code thaumcraft:blockwarded}, capturing block, light and owner on
 *       the tile, and pays the cast cost per warded block as the original
 *       loop did (the seam paid one; the handler pays the rest).</li>
 *   <li><b>Swap</b> — the equal-trade focus ({@code ItemFocusTrade.java:95-165}):
 *       sneak-pick stores the pointed block on the focus (the original's
 *       {@code "picked"}, free); the swap enqueues onto
 *       {@link SwapperBridge} with the original's guards (air, unbreakable
 *       and picked-equals-target skip) and the original's
 *       {@code 3 + enlarge} lifespan. The cast itself pays nothing in the
 *       original — the queue charges per swap — so a successful enqueue
 *       reports the no-charge status.</li>
 *   <li><b>Hole</b> — the portable hole ({@code ItemFocusPortableHole.java:82-151}):
 *       opens {@code distance + 1} blocks for {@code duration} ticks through
 *       {@link HoleCarrier}. The body pre-charged {@code distance} copies of
 *       the cast cost with the base the seam paid; a refused hole refunds the
 *       whole tax too.</li>
 *   <li><b>Excavate</b> — the dig mutation ({@code ItemFocusExcavation.java:109-227}):
 *       every channeled tick pays (the original's tick charge is the body's
 *       first statement, {@code :111}) and accumulates the spec's speed (the
 *       original's soft material set runs on the block's sound type; obsidian
 *       breaks at triple) against the pointed block's hardness, tracked as
 *       {@link DigState} on the wand. A tick that points at a NEW block only
 *       records it: the original zeroes the progress there and adds no speed.
 *       At hardness the block breaks — and pays a SECOND time, the original's
 *       completion test ({@code :205}) — with fortune {@code treasure}, silk
 *       from {@code silktouch}, its drops, its XP and the break event, and
 *       each of up to {@code enlarge} matching neighbours breaks on its own
 *       paid cost, the original's randomised order and per-neighbour
 *       dry-run-then-pay included.</li>
 * </ul>
 *
 * <p>Area sizes: {@code Ward}/{@code Swap} carry the original's
 * {@code getMaxAreaSize} bound, not a GUI selection — the selection surface
 * is TC-M7-04, and with no selection the original walked area 1, so the
 * handlers act on the single pointed block. The original's flood walks
 * ({@code checkNeighbours}, {@code CollectBlocks}) are ported here as
 * {@link #flood} for the GUI to drive; unit-tested and wired behind an
 * extent.</p>
 */
public final class EffectPerformers {

    /** The warded block's id (the original's {@code GameRegistry.findBlock}). */
    public static final ResourceLocation WARDED_BLOCK =
            ResourceLocation.parse("thaumcraft:blockwarded");
    /** The portable hole block's id. */
    public static final ResourceLocation HOLE_BLOCK =
            ResourceLocation.parse("thaumcraft:blockhole");

    /** The keys this card registers — the ten non-native effects. */
    public static final List<String> KEYS = List.of(
            SpellEffect.Projectile.EMBER,
            SpellEffect.Projectile.EXPLOSIVE_ORB,
            SpellEffect.Projectile.SHOCK_ORB,
            SpellEffect.Projectile.FROST_SHARD,
            SpellEffect.Projectile.PECH_BLAST,
            SpellEffect.Projectile.PRIMAL_ORB,
            SpellEffect.Projectile.FIRE_BAT,
            SpellEffect.Ward.KEY,
            SpellEffect.Swap.KEY,
            SpellEffect.Hole.KEY,
            SpellEffect.Excavate.KEY);

    /**
     * Registers every performer. Idempotent and re-runnable: a probe (or a
     * partner card's cleanup) that unregisters a key restores the real
     * handler with one call.
     */
    public static void registerAll() {
        SpellEffects.register(SpellEffect.Projectile.EMBER, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.EXPLOSIVE_ORB, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.SHOCK_ORB, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.FROST_SHARD, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.PECH_BLAST, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.PRIMAL_ORB, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Projectile.FIRE_BAT, EffectPerformers::performProjectile);
        SpellEffects.register(SpellEffect.Ward.KEY, EffectPerformers::performWard);
        SpellEffects.register(SpellEffect.Swap.KEY, EffectPerformers::performSwap);
        SpellEffects.register(SpellEffect.Hole.KEY, EffectPerformers::performHole);
        SpellEffects.register(SpellEffect.Excavate.KEY, EffectPerformers::performExcavate);
    }

    // ---------------------------------------------------------------- projectiles

    /**
     * Spawns the spec's projectile {@code count} times:
     * {@code world.spawnEntityInWorld(new EntityEmber(world, p, scatter))} and
     * its six siblings, every parameter already carried on the spec.
     */
    private static boolean performProjectile(final CastContext context, final SpellEffect effect) {
        if (!(effect instanceof SpellEffect.Projectile projectile)) {
            return false;
        }
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final ResourceLocation entityId = ResourceLocation.parse(projectile.entityId());
        final EntityType<?> type =
                BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)
                        ? BuiltInRegistries.ENTITY_TYPE.get(entityId)
                        : null;
        if (type == null) {
            // TC-M6-06 has not landed this entity: refuse, refund, charge nothing.
            return false;
        }
        for (int i = 0; i < Math.max(1, projectile.count()); i++) {
            final Entity entity = type.create(level);
            if (entity == null) {
                return false;
            }
            if (entity instanceof FocusProjectile shot) {
                shot.launch(context, projectile);
            } else {
                launchFallback(level, entity, context, projectile);
            }
            level.addFreshEntity(entity);
        }
        return true;
    }

    /**
     * Best-effort launch for an entity that predates {@link FocusProjectile}:
     * spawn at the caster's eyes aimed along their view at the spec's speed
     * ({@code EntityFrostShard}'s {@code 1.0F}/{@code 8.0F} and the projectile
     * constructors' eye-relative start).
     */
    private static void launchFallback(final ServerLevel level, final Entity entity,
                                       final CastContext context, final SpellEffect.Projectile spec) {
        final Vec3 eye = context.player().getEyePosition();
        final Vec3 look = context.player().getLookAngle();
        entity.setPos(eye.x + look.x * 0.5, eye.y + look.y * 0.5, eye.z + look.z * 0.5);
        final double speed = spec.speed() > 0.0 ? spec.speed() : 1.5;
        entity.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
    }

    // ---------------------------------------------------------------------- ward

    /**
     * The warding focus: restore over an owned warded block (free), ward plain
     * solid ground paying per block
     * ({@code ItemFocusWarding.java:95-127}: the loop's per-block
     * {@code consumeAllVis(viscost, …, true)}).
     */
    private static boolean performWard(final CastContext context, final SpellEffect effect) {
        if (!(effect instanceof SpellEffect.Ward ward)) {
            return false;
        }
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final Block warded = BuiltInRegistries.BLOCK.containsKey(WARDED_BLOCK)
                ? BuiltInRegistries.BLOCK.get(WARDED_BLOCK) : null;
        if (warded == null) {
            // The warding machinery has not landed: refuse, refund, change nothing.
            return false;
        }
        final BlockHitResult hit = CastTargets.pointedBlock(context.level(), context.player());
        if (hit == null) {
            return false;
        }
        final BlockPos centre = hit.getBlockPos();
        final Direction side = hit.getDirection();
        // The GUI area selection is TC-M7-04; with no selection the original walked area 1.
        final int extent = 0;
        final int owner = ownerHash(context.player());
        if (isWarded(level, centre, warded)) {
            return unward(context, level, warded, owner,
                    flood(centre, side, extent, pos -> isOwnedWarded(level, pos, warded, owner)));
        }
        final List<BlockPos> blocks = flood(centre, side, extent,
                pos -> plainWardable(level, pos, warded));
        if (blocks.isEmpty()) {
            return false;
        }
        int paid = 0;
        for (final BlockPos pos : blocks) {
            // The seam paid the first block; the loop pays each further one,
            // and the original's loop breaks the moment the wand cannot pay.
            if (paid > 0 && !context.wand().consumeAllVis(
                    context.wandStack(), context.player(), context.cost(), true, false)) {
                break;
            }
            wardBlock(level, pos, warded, owner);
            paid++;
        }
        return paid > 0;
    }

    /**
     * Restore every covered warded block ({@code ItemFocusWarding.java:135-145}).
     * The original's restore path charged NOTHING — no {@code consumeAllVis}
     * call anywhere in that branch — so the handler performs it and reports the
     * no-charge status (false), which is the seam's refund. The ward-mode
     * branch above is the one that pays, once per warded block.
     */
    private static boolean unward(final CastContext context, final ServerLevel level,
                                  final Block warded, final int owner, final List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return false;
        }
        for (final BlockPos pos : blocks) {
            if (level.getBlockEntity(pos) instanceof WardedSurface surface) {
                final BlockState captured = surface.captured();
                if (captured != null) {
                    level.setBlock(pos, captured, 3);
                }
            }
        }
        return false;
    }

    /**
     * One warded block: capture block, light and owner, then place
     * {@code blockWarded} — the original's
     * {@code tw.block = bi; tw.blockMd = md; tw.light = …; tw.owner = …;
     * world.setBlock(x, y, z, Blocks.field_150350_m …)} ({@code :98-107}).
     * The port stores the captured {@link BlockState} on the tile, so the
     * metadata proxy is unnecessary; the {@code variant} property, when the
     * block defines one, carries the light level the original rendered.
     */
    private static void wardBlock(final ServerLevel level, final BlockPos pos,
                                  final Block warded, final int owner) {
        final BlockState captured = level.getBlockState(pos);
        final int light = captured.getLightEmission(level, pos);
        BlockState state = warded.defaultBlockState();
        final IntegerProperty variant = (IntegerProperty)
                warded.getStateDefinition().getProperty(VariantStateItems.VARIANT_PROPERTY);
        if (variant != null) {
            state = state.setValue(variant, net.minecraft.util.Mth.clamp(light,
                    java.util.Collections.min(variant.getPossibleValues()),
                    java.util.Collections.max(variant.getPossibleValues())));
        }
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof WardedSurface surface) {
            surface.setCaptured(captured, light, owner);
        }
    }

    /** The original's warded-block + owner test ({@code ItemFocusWarding.java:117-121}). */
    private static boolean isOwnedWarded(final ServerLevel level, final BlockPos pos,
                                         final Block warded, final int owner) {
        return isWarded(level, pos, warded)
                && level.getBlockEntity(pos) instanceof WardedSurface surface
                && surface.ownerHash() == owner;
    }

    private static boolean isWarded(final ServerLevel level, final BlockPos pos, final Block warded) {
        return level.getBlockState(pos).is(warded);
    }

    /**
     * The ward-mode accept test ({@code ItemFocusWarding.java:245-253}): a
     * solid, non-air block with no tile and not already warded.
     */
    private static boolean plainWardable(final ServerLevel level, final BlockPos pos, final Block warded) {
        final BlockState state = level.getBlockState(pos);
        return !state.is(warded) && !state.isAir() && !state.hasBlockEntity() && state.isSolid();
    }

    /** The original's {@code player.getCommandSenderName().hashCode()}. */
    private static int ownerHash(final Player player) {
        return player.getGameProfile().getName().hashCode();
    }

    // ---------------------------------------------------------------------- swap

    /**
     * The equal-trade focus: sneak-pick stores the pointed block on the focus
     * ({@code ItemFocusTrade.java:93-95}, free), and the swap hands the
     * pointed position to {@link SwapperBridge} ({@code :141,189}), which
     * charges per swapped block as the original's queue did.
     */
    private static boolean performSwap(final CastContext context, final SpellEffect effect) {
        if (!(effect instanceof SpellEffect.Swap swap)) {
            return false;
        }
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final Player player = context.player();
        final ItemStack focus = context.focusStack();
        if (player.isShiftKeyDown()) {
            // The original gated the pick on the sneak input flag (1.7.10
            // EntityPlayer.isSneaking(), ItemFocusTrade.java:93-95). The modern
            // isCrouching() is pose-derived — it only follows the flag once the
            // entity ticks — so the flag is both the closer port and the one an
            // unticked test player can set.
            final BlockHitResult hit = CastTargets.pointedBlock(level, player);
            if (hit == null) {
                return false;
            }
            final BlockState source = level.getBlockState(hit.getBlockPos());
            if (source.isAir() || source.getDestroySpeed(level, hit.getBlockPos()) < 0.0F) {
                return false;
            }
            ItemFocus.setPickedBlock(focus, pickedStackFor(source));
            // The original wrote the pick onto the focus stack its wand stored;
            // the port rebuilds that stack from the tag on every cast, so the
            // pick is snapshotted on the wand beside the upgrades (TC-M6-03-p).
            ItemWandCasting.setFocusPicked(context.wandStack(), ItemFocus.getPickedBlock(focus));
            // The pick is the original's zero-cost path — performed, not charged.
            return false;
        }
        final ItemStack picked = ItemFocus.getPickedBlock(focus);
        if (picked.isEmpty()) {
            return false;
        }
        final SwapperBridge bridge = SwapperBridge.get();
        if (bridge == null) {
            // The equal-trade queue has not landed: refuse, refund, change nothing.
            return false;
        }
        final BlockHitResult hit = CastTargets.pointedBlock(level, player);
        if (hit == null) {
            return false;
        }
        final BlockPos centre = hit.getBlockPos();
        final BlockState source = level.getBlockState(centre);
        if (source.isAir() || source.getDestroySpeed(level, centre) < 0.0F
                || picked.getItem() == source.getBlock().asItem()) {
            // The queue's own guards (ServerTickEventsFML.java:180-190) skip these.
            return false;
        }
        final int extent = 0; // the GUI selection (TC-M7-04) drives flood's extent
        final List<BlockPos> targets = flood(centre, hit.getDirection(), extent,
                pos -> sameBlockItem(level, pos, picked));
        final int lifespan = swap.architect() ? 0 : swap.radius();
        for (final BlockPos pos : targets) {
            bridge.queue(level, pos, level.getBlockState(pos), picked,
                    lifespan, player, player.getInventory().selected);
        }
        // The cast is the original's zero-cost path: the swapper charged per
        // swap, so the seam's prepayment is returned.
        return false;
    }

    /**
     * The stack for a picked block — the original's
     * {@code BlockUtils.createStackedBlock(bi, md)}
     * ({@code ItemFocusTrade.java:94,101}); the port's pick-block factory is
     * the block item with its {@code variant} recorded as the
     * {@code minecraft:block_state} component ({@code blocks/VariantStateItems}).
     */
    static ItemStack pickedStackFor(final BlockState state) {
        final ItemStack stack = new ItemStack(state.getBlock().asItem());
        VariantStateItems.withVariant(stack, state);
        return stack;
    }

    private static boolean sameBlockItem(final ServerLevel level, final BlockPos pos,
                                         final ItemStack picked) {
        final BlockState state = level.getBlockState(pos);
        return !state.isAir() && picked.getItem() == state.getBlock().asItem();
    }

    // ---------------------------------------------------------------------- hole

    /**
     * The portable hole: open {@code distance + 1} blocks along the pointed
     * face through {@link HoleCarrier}
     * ({@code TileHole.createHole(…, (byte)(distance + 1), duration)}).
     */
    private static boolean performHole(final CastContext context, final SpellEffect effect) {
        if (!(effect instanceof SpellEffect.Hole hole)) {
            return false;
        }
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final Block blockHole = BuiltInRegistries.BLOCK.containsKey(HOLE_BLOCK)
                ? BuiltInRegistries.BLOCK.get(HOLE_BLOCK) : null;
        if (!(blockHole instanceof HoleCarrier carrier)) {
            // The portable-hole machinery has not landed: refund the distance
            // tax the body charged and the seam's base, change nothing.
            refundDistanceTax(context, hole);
            return false;
        }
        final BlockHitResult hit = CastTargets.pointedBlock(context.level(), context.player());
        if (hit == null) {
            refundDistanceTax(context, hole);
            return false;
        }
        final boolean opened = carrier.openHole(level, hit.getBlockPos(),
                Direction.from3DDataValue(hole.side()), hole.distance() + 1, hole.duration());
        if (!opened) {
            // The original's guards refuse: refund the whole cast, its tax included.
            refundDistanceTax(context, hole);
        }
        return opened;
    }

    /**
     * The body charged {@code baseCost × distance} on top of the seam's base
     * ({@code ItemFocusPortableHole.java:135-137}); when no hole opens, that
     * tax comes back — the seam itself refunds only its own base. The tax was
     * deducted as the cap-modified total, so the refund is the effective
     * cost times the distance (whole-centi truncation can leave at most a
     * centi-per-block of the refund behind, in the player's favour).
     */
    private static void refundDistanceTax(final CastContext context, final SpellEffect.Hole hole) {
        if (hole.distance() > 0) {
            WandCasting.refund(context.wand(), context.wandStack(),
                    multiplied(context.cost(), hole.distance()));
        }
    }

    // ------------------------------------------------------------------ excavate

    /**
     * The dig mutation, one paid tick at a time
     * ({@code ItemFocusExcavation.onUsingFocusTick}, lines 109-227). The
     * original charged EVERY channeled tick — {@code consumeAllVis(stack, p,
     * getVisCost(stack), false, false)} is the tick body's first statement
     * ({@code :111}), and the wand stops the use when it cannot pay — so the
     * seam's tick payment stands for every tick this handler runs. A tick that
     * completes a dig pays the cost a SECOND time, because the original's
     * completion test is {@code bc >= hardness && wand.consumeAllVis(..., true,
     * false)} ({@code :205}): no payment, no break, progress keeps
     * accumulating. Each broken neighbour pays its own cost, the original's
     * dry-run-then-break-then-pay triple ({@code :207-212}).
     */
    private static boolean performExcavate(final CastContext context, final SpellEffect effect) {
        if (!(effect instanceof SpellEffect.Excavate spec)) {
            return false;
        }
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        final Player player = context.player();
        final ItemStack wand = context.wandStack();
        final DigState stored = wand.get(ModDataComponents.WAND_DIG_STATE.get());
        final BlockHitResult hit = CastTargets.pointedBlock(level, player);
        final BlockPos pos = hit == null ? null : hit.getBlockPos();
        final BlockState state = pos == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
        final float hardness = pos == null ? -1.0F : state.getDestroySpeed(level, pos);
        // The original's gate: a block hit the player can edit, with a real
        // hardness. 1.21.1 has no per-position canPlayerEdit for a wand cast,
        // so the creative/spectator split is the gate that survives.
        if (pos == null || state.isAir() || hardness < 0.0F || player.isSpectator()) {
            // No target: the tick was still paid (the charge runs before this
            // test), the dig coordinates go to MAX and bc to 0 (:220-225).
            if (stored != null) {
                wand.remove(ModDataComponents.WAND_DIG_STATE.get());
            }
            return true;
        }
        if (stored == null || !stored.isAt(pos)) {
            // The original's new-target branch (:214-219): remember the block,
            // zero the progress, and add nothing on this tick.
            wand.set(ModDataComponents.WAND_DIG_STATE.get(), new DigState(pos, 0.0F));
            return true;
        }
        final float bc = stored.progress() + digSpeed(state, spec, level, pos);
        if (bc < hardness) {
            wand.set(ModDataComponents.WAND_DIG_STATE.get(), new DigState(pos, bc));
            return true; // the tick's own payment stands
        }
        if (!context.wand().consumeAllVis(wand, player, context.baseCost(), true, false)) {
            // The original's completion test failed on the payment: no break,
            // progress keeps accumulating (:205, :213).
            wand.set(ModDataComponents.WAND_DIG_STATE.get(), new DigState(pos, bc));
            return true;
        }
        wand.remove(ModDataComponents.WAND_DIG_STATE.get());
        final boolean dug = digBlock(level, player, pos, state, spec);
        if (dug) {
            // The neighbours' loop (ItemFocusExcavation.java:207-212): up to
            // enlarge matching neighbours, each on its own dry-run-then-pay.
            for (int i = 0; i < spec.enlarge(); i++) {
                final BlockPos neighbour = matchingNeighbour(level, pos, state);
                if (neighbour == null) {
                    continue;
                }
                if (context.wand().consumeAllVis(
                        context.wandStack(), player, context.baseCost(), false, false)
                        && digBlock(level, player, neighbour, level.getBlockState(neighbour), spec)) {
                    context.wand().consumeAllVis(
                            context.wandStack(), player, context.baseCost(), true, false);
                }
            }
        }
        return true;
    }

    /**
     * Progress per tick: the spec's speed, the soft-material set on the
     * original's block materials ({@code rock}/{@code grass}/{@code ground}/
     * {@code sand} — proxied by the block's sound type) and the original's
     * obsidian triple ({@code :184-186}).
     */
    static float digSpeed(final BlockState state, final SpellEffect.Excavate spec,
                          final ServerLevel level, final BlockPos pos) {
        float speed = isSoftMaterial(state) ? spec.softSpeed() : spec.speed();
        if (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) {
            speed *= 3.0F;
        }
        return speed;
    }

    /**
     * The original's soft set — {@code Material.field_151576_e} (rock),
     * {@code field_151577_b} (grass), {@code field_151578_c} (ground),
     * {@code field_151595_p} (sand) ({@code ItemFocusExcavation.java:179-183});
     * 1.21.1 has no {@code Material}, so the break-sound group is the proxy:
     * {@code STONE} (rock), {@code GRASS}, {@code GRAVEL} (the {@code ground}
     * family — dirt's break sound is gravel), {@code SAND}.
     */
    static boolean isSoftMaterial(final BlockState state) {
        final SoundType sound = state.getSoundType();
        return sound == SoundType.STONE || sound == SoundType.GRASS
                || sound == SoundType.GRAVEL || sound == SoundType.SAND;
    }

    /**
     * Break one block with the cast's drops ({@code ItemFocusExcavation
     * .java:190-222}): a plain diamond pick carries fortune
     * {@code treasure} and the silk touch, the block's drops pop, its XP
     * pops, and the break removes it without a second drop.
     */
    private static boolean digBlock(final ServerLevel level, final Player player,
                                    final BlockPos pos, final BlockState state,
                                    final SpellEffect.Excavate spec) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (spec.treasure() > 0) {
            tool.enchant(enchantment(level, Enchantments.FORTUNE), spec.treasure());
        }
        // Silk is the vanilla mechanism: the tool carries the enchantment and
        // the block's loot tables apply it (what the port's BlockLoot does).
        if (spec.silktouch()) {
            tool.enchant(enchantment(level, Enchantments.SILK_TOUCH), 1);
        }
        final List<ItemStack> drops =
                Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
        for (final ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
        final int xp = state.getBlock().getExpDrop(
                state, level, pos, level.getBlockEntity(pos), player, tool);
        if (xp > 0) {
            state.getBlock().popExperience(level, pos, xp);
        }
        level.destroyBlock(pos, false, player);
        level.levelEvent(2001, pos, Block.getId(state));
        return true;
    }

    /**
     * The original's neighbour pick ({@code ItemFocusExcavation.java:156-166}):
     * shuffle the six sides on the world random, take the first neighbour of
     * the same block (the original's block + metadata match — the state is
     * the metadata).
     */
    private static BlockPos matchingNeighbour(final ServerLevel level, final BlockPos pos,
                                              final BlockState state) {
        final List<Direction> directions = new ArrayList<>(java.util.Arrays.asList(Direction.values()));
        for (int i = directions.size() - 1; i > 0; i--) { // the original's world.rand shuffle
            final int j = level.random.nextInt(i + 1);
            final Direction tmp = directions.get(i);
            directions.set(i, directions.get(j));
            directions.set(j, tmp);
        }
        for (final Direction direction : directions) {
            final BlockPos next = pos.relative(direction);
            if (level.getBlockState(next).getBlock() == state.getBlock()) {
                return next;
            }
        }
        return null;
    }

    private static Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment(
            final ServerLevel level, final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    // ----------------------------------------------------------------- geometry

    /**
     * The original's flood walk over the pointed area
     * ({@code ItemFocusWarding.checkNeighbours}, {@code ItemFocusTrade
     * .CollectBlocks}): the centre, then the six neighbours, each accepted
     * position's neighbours in turn, every position bounded by the GUI-area
     * limit for the pointed face. {@code extent} is that limit
     * (GUI area 1 → extent 0 → the centre alone).
     *
     * <p>The original recursed depth-first in a fixed order (the centre's six
     * sides, then each in the same order); the port walks breadth-first —
     * the collected set is identical, and the order only decides which
     * blocks a mid-loop payment cut leaves warded, documented as a
     * deviation.</p>
     */
    public static List<BlockPos> flood(final BlockPos centre, final Direction side,
                                       final int extent, final Predicate<BlockPos> accept) {
        final List<BlockPos> out = new ArrayList<>();
        final Set<BlockPos> seen = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(centre);
        while (!queue.isEmpty()) {
            final BlockPos pos = queue.poll();
            if (!seen.add(pos) || !withinLimit(centre, pos, side, extent)) {
                continue;
            }
            if (accept.test(pos)) {
                out.add(pos);
                for (final Direction direction : Direction.values()) {
                    queue.add(pos.relative(direction));
                }
            }
        }
        return out;
    }

    /**
     * The area limit on the pointed face — the original's per-side
     * {@code checkNeighbours} calls: up/down pass the sizes straight,
     * north/south swap the x/z limits, west/east swap the y/z limits
     * ({@code ItemFocusWarding.java:103-110, 122-127, 148-151}).
     */
    public static boolean withinLimit(final BlockPos centre, final BlockPos pos,
                                      final Direction side, final int extent) {
        final int dx = Math.abs(pos.getX() - centre.getX());
        final int dy = Math.abs(pos.getY() - centre.getY());
        final int dz = Math.abs(pos.getZ() - centre.getZ());
        return switch (side) {
            case DOWN, UP -> dx <= extent && dy <= extent && dz <= extent;
            case NORTH, SOUTH -> dz <= extent && dy <= extent && dx <= extent;
            case WEST, EAST -> dx <= extent && dz <= extent && dy <= extent;
        };
    }

    /** One aspect price times {@code factor} (as {@code FocusSpells}' private copy). */
    private static AspectList multiplied(final AspectList cost, final int factor) {
        final AspectList out = new AspectList();
        for (final Aspect aspect : cost.getAspects()) {
            out.add(aspect, cost.getAmount(aspect) * factor);
        }
        return out;
    }

    private EffectPerformers() {
    }
}