package com.jonnyutah12.thaumcraftclassic.blocks;

import com.jonnyutah12.thaumcraftclassic.api.damagesource.DamageSourcesThaumcraft;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogEldritchRealm;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchAltar;
import com.jonnyutah12.thaumcraftclassic.tiles.TileEldritchCrabSpawner;
import com.jonnyutah12.thaumcraftclassic.world.outerlands.EldritchRealmValues;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block Eldritch (content-mapping.csv {@code thaumcraft:blockeldritch}) — the
 * eleven-variant eldritch building block of the Outer Lands structures.
 *
 * <p>Original: {@code thaumcraft.common.blocks.BlockEldritch} — hardness 20,
 * blast resistance 1200, ticking, unbreakable in survival (the original's
 * {@code removedByDestroy} returns false for everything), with a per-variant
 * mining time and drop experience (the structures are the reward for the
 * boss rooms, and the eldritch blocks pay out for breaking them): altar 0-1500
 * from its charge, obelisk 1000, obelisk top 0 (it shatters), caps and urns
 * 500, the chest pays its stored loot, the spawner 0-3000 from its count.</p>
 *
 * <p>Registered as a single block with an 11-value {@code variant} property,
 * the house shape for the original's metadata families; the variant names, the
 * per-variant item and the localization live in
 * {@link BlockVariantNames#ELDRITCH}, and the tile entities attach per variant
 * in {@link CatalogEldritchRealm} exactly where the original attached them
 * (altar 0, obelisk 1, cap 3, lock 8, crab spawner 9).</p>
 *
 * <p>Deferred: the client-side sparkles on the altar (meta 0), the capstone
 * (3), the chest (7) and the urns (11), and the per-variant face icons are
 * TC-M7-01/02. The chest's container (the original's {@code TileEldritchChest}
 * inventory) is TC-M7-04; the variant is registered and pays its stored
 * experience exactly as the original does.</p>
 */
public class BlockEldritch extends BlockDeviceBase {

    /** The original's metadata range, verbatim (D-001). */
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0,
            EldritchRealmValues.ELDRITCH_VARIANT_COUNT - 1);

    /** The original's {@code MAX_XP}/{@code MAX_STACK_XP}. */
    public static final int MAX_XP = EldritchRealmValues.ELDRITCH_MAX_XP;
    public static final int MAX_STACK_XP = EldritchRealmValues.ELDRITCH_MAX_STACK_XP;

    // The trap (meta 10) per the original: state lives on the block instance
    // (the original's fields are on the block, with lastLocation as the guard),
    // as in the port's other stateful-block helpers.
    private long lastDamage = 0L;
    private BlockPos lastLocation = BlockPos.ZERO;

    public BlockEldritch() {
        // Original: hardness 20, blast resistance 1200, ticking. The light 7
        // the original gave the chest variant rides on its per-meta light value;
        // the light-emitting variants arrive with their blockstate JSONs.
        super(VARIANT, Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(EldritchRealmValues.ELDRITCH_HARDNESS, EldritchRealmValues.ELDRITCH_RESISTANCE)
                .requiresCorrectToolForDrops());
    }

    /** The {@code variant} property this block carries; the base cannot add it
     * (its field is unset while the superclass constructor runs) — house
     * pattern, same as {@link BlockStoneDevice}. */
    @Override
    protected void createBlockStateDefinition(
            final net.minecraft.world.level.block.state.StateDefinition.Builder<
                    net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    /**
     * The original's {@code getDestroyProgress}: the eldritch blocks are a
     * long grind (the caps and urns take 3 and the obelisk top is a moment),
     * and the trap (meta 10) is the vanilla time.
     */
    @Override
    public float getDestroyProgress(final BlockState state, final Player player,
                                    final BlockGetter level, final BlockPos pos) {
        final int variant = state.getValue(VARIANT);
        return switch (variant) {
            case EldritchRealmValues.ELDRITCH_OBELISK_TOP ->
                    player.getDestroySpeed(state) / 2.0F / 4000.0F;
            case EldritchRealmValues.ELDRITCH_CAP, EldritchRealmValues.ELDRITCH_CRUSTED ->
                    player.getDestroySpeed(state) / 2.0F / 3.0F;
            case EldritchRealmValues.ELDRITCH_DOORWAY ->
                    player.getDestroySpeed(state) / 2.0F / 150.0F;
            case EldritchRealmValues.ELDRITCH_GATE ->
                    player.getDestroySpeed(state) / 2.0F / 5.0F;
            default -> super.getDestroyProgress(state, player, level, pos);
        };
    }

    /**
     * The original's {@code getExpDrop}: the eldritch blocks pay out for being
     * broken — the altar by its charge (capped at 1500), the obelisk at 1000,
     * the top at nothing, caps/urns at 500, the chest at its stored loot, the
     * spawner by its count (capped at 3000). The trap and the pillar pay the
     * vanilla default.
     */
    @Override
    public int getExpDrop(final BlockState state, final net.minecraft.world.level.LevelAccessor level,
                          final BlockPos pos, final BlockEntity tile,
                          final net.minecraft.world.entity.Entity entity, final ItemStack tool) {
        final int variant = state.getValue(VARIANT);
        return switch (variant) {
            case EldritchRealmValues.ELDRITCH_ALTAR -> tile instanceof TileEldritchAltar altar
                    ? Mth.clamp(altar.counter() * 10, 0, MAX_STACK_XP) : 0;
            case EldritchRealmValues.ELDRITCH_OBELISK -> EldritchRealmValues.ELDRITCH_OBELISK_XP;
            case EldritchRealmValues.ELDRITCH_OBELISK_TOP -> EldritchRealmValues.ELDRITCH_OBELISK_TOP_XP;
            case EldritchRealmValues.ELDRITCH_CAP, EldritchRealmValues.ELDRITCH_CRUSTED ->
                    EldritchRealmValues.ELDRITCH_CAP_XP;
            // The gate/chest's stored experience travels in its container (the
            // original's TileEldritchChest read on the dropped stack); the
            // chest container is TC-M7-04, so the plain block drop pays 0 xp
            // until that card wires the stored amount.
            case EldritchRealmValues.ELDRITCH_GATE -> 0;
            case EldritchRealmValues.ELDRITCH_CRAB_SPAWNER -> tile instanceof TileEldritchCrabSpawner spawner
                    ? Mth.clamp(spawner.count * 10, 0, MAX_XP) : 0;
            default -> super.getExpDrop(state, level, pos, tile, entity, tool);
        };
    }

    /**
     * The original's {@code onEntityCollidedWithBlock} meta-10 branch: the trap
     * shreds whatever stands on it (1-4 damage scaled by armour, bypassing
     * armour via the tentacle damage type) and teleports the victim 0.5 cells
     * over; a 500-tick cooldown per trap, and it does nothing to a player
     * standing on the same block twice.
     */
    @Override
    public void entityInside(final BlockState state, final Level level, final BlockPos pos, final Entity entity) {
        if (state.getValue(VARIANT) != EldritchRealmValues.ELDRITCH_TRAP) {
            return;
        }
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        final BlockPos centre = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        if (pos.equals(centre) && pos.equals(lastLocation) && !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (level.getGameTime() - lastDamage < EldritchRealmValues.TRAP_COOLDOWN_TICKS) {
            return;
        }
        lastDamage = level.getGameTime();
        lastLocation = pos;
        int damage = 1 + level.getRandom().nextInt(4);
        if (living instanceof Player player) {
            damage = 1 + Mth.floor(player.getArmorValue() / 2.5F) + level.getRandom().nextInt(4);
        }
        living.hurt(DamageSourcesThaumcraft.source(level, DamageSourcesThaumcraft.TENTACLE), damage);
        entity.setDeltaMovement(entity.getDeltaMovement().x, 0.25F, 0.25F);
    }
}
