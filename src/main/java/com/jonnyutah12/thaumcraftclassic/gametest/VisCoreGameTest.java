package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.aspect.Aspect;
import com.jonnyutah12.thaumcraftclassic.registration.ModDataComponents;
import com.jonnyutah12.thaumcraftclassic.registration.TestProbeContent;
import com.jonnyutah12.thaumcraftclassic.wands.VisPool;
import com.jonnyutah12.thaumcraftclassic.wands.WandVis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TC-M4-02 acceptance: the wand vis pool (data component {@code
 * thaumcraft:vis_pool}) survives the real item-stack save path.
 *
 * <p>{@link #visChargeWithdrawRoundTripPersistsAcrossSave}: charge a stack
 * (deposit semantics of the original {@code ItemWandCasting.addVis}), withdraw
 * (its {@code consumeVis}), then round-trip the stack through
 * {@code ItemStack.save} → compressed NBT on disk → {@code parseOptional} —
 * the exact path inventories and player saves use — and assert the centi-vis
 * state is intact. Capacity is never stored: it is re-derived from the rod
 * capacity (TC-M0-06 §2.2 greatwood = 50 → 5000 centi-vis), matching the
 * original's read-time {@code getMaxVis}.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VisCoreGameTest {

    private VisCoreGameTest() {
    }

    @GameTest(template = "empty")
    public static void visChargeWithdrawRoundTripPersistsAcrossSave(final GameTestHelper helper) {
        final Aspect aer = Aspect.getAspect("aer");
        final Aspect terra = Aspect.getAspect("terra");
        helper.assertTrue(aer != null && terra != null, "primal aspects not registered");

        // Greatwood rod capacity 50 → 5000 centi-vis per aspect (docs/balance-values.md §2.2-2.3).
        final int max = WandVis.maxVis(50, false);
        helper.assertTrue(max == 5000, "capacity math wrong: expected 5000, got " + max);

        final ItemStack stack = new ItemStack(TestProbeContent.PROBE_ITEM.get());

        // Charge: 30 real vis of aer (3000c) — under capacity, no leftover.
        final WandVis.AddResult charged = WandVis.addVis(VisPool.EMPTY, aer, 30, max);
        helper.assertTrue(charged.leftover() == 0, "unexpected leftover on under-capacity deposit");
        helper.assertTrue(WandVis.getVis(charged.pool(), aer) == 3000, "charge not applied");

        // Over-capacity deposit must clamp and report the leftover (original addVis rule).
        final WandVis.AddResult over = WandVis.addVis(charged.pool(), aer, 40, max);
        helper.assertTrue(WandVis.getVis(over.pool(), aer) == 5000, "over-capacity deposit not clamped");
        helper.assertTrue(over.leftover() == 20, "over-capacity leftover wrong: " + over.leftover());

        // Withdraw: 12.5 real vis (1250c) of aer, plus a terra deposit to carry.
        final VisPool afterWithdraw = WandVis.consumeVis(over.pool(), aer, 1250);
        helper.assertTrue(afterWithdraw != null, "withdraw of covered amount refused");
        final VisPool withTerra = WandVis.addRealVis(afterWithdraw, terra, 500, max).pool();
        helper.assertTrue(WandVis.getVis(withTerra, aer) == 3750, "withdraw math wrong");

        WandVis.setPool(stack, withTerra);
        helper.assertTrue(WandVis.poolOf(stack).equals(withTerra), "component not on stack");

        // Save path: ItemStack.save (full form incl. components) → compressed NBT
        // file → read back → parseOptional. Private temp dir, as in
        // PersistenceGameTest: run/world is shared and not a reliable fixture.
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = (CompoundTag) stack.save(registries);
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("tc-vis-pool");
            final CompoundTag root = new CompoundTag();
            root.put("stack", saved);
            NbtUtils.addCurrentDataVersion(root);
            final Path file = tempDir.resolve("stack.dat");
            NbtIo.writeCompressed(root, file);

            final CompoundTag reloadedRoot = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            final ItemStack reloaded = ItemStack.parseOptional(registries, reloadedRoot.getCompound("stack"));
            if (reloaded.isEmpty()) {
                helper.fail("item stack did not reload from disk");
                return;
            }

            final VisPool reloadedPool = WandVis.poolOf(reloaded);
            helper.assertTrue(reloadedPool.getVis(aer) == 3750,
                    "aer charge not persisted: " + reloadedPool.getVis(aer));
            helper.assertTrue(reloadedPool.getVis(terra) == 500,
                    "terra charge not persisted: " + reloadedPool.getVis(terra));

            // Capacity is derived, not stored: re-deriving must still clamp correctly.
            final WandVis.AddResult refilled = WandVis.addVis(reloadedPool, aer, 100, WandVis.maxVis(50, false));
            helper.assertTrue(WandVis.getVis(refilled.pool(), aer) == 5000 && refilled.leftover() == 87,
                    "derived capacity broken after reload");
        } catch (final IOException e) {
            helper.fail("temp save round-trip failed: " + e);
        } finally {
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir.resolve("stack.dat"));
                    Files.deleteIfExists(tempDir);
                } catch (final IOException ignored) {
                    // best-effort cleanup
                }
            }
        }

        helper.succeed();
    }
}
