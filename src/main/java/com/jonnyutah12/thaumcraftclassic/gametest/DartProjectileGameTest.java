package com.jonnyutah12.thaumcraftclassic.gametest;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.entities.projectile.EntityDart;
import com.jonnyutah12.thaumcraftclassic.registration.catalog.CatalogGolems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * World probes for {@code thaumcraft:dart} (TC-M6-08-s-c; card t_3eb79917),
 * content-mapping {@code Thaumcraft:Dart} (mod_index 8, "Golem Dart").
 *
 * <p>The one figure this class pins is the <b>save contract</b> of an arrow
 * built through the {@code (EntityType, Level)} ctor. 1.7.10's
 * {@code EntityArrow.canBePickedUp} is ONE field; 1.21.1 splits it into the
 * pickup <em>mode</em> ({@code AbstractArrow.Pickup}, default
 * {@code DISALLOWED} at {@code AbstractArrow.java:62}) which gates pickup, and
 * the pickup <em>stack</em> ({@code AbstractArrow.java:71}) which
 * {@code addAdditionalSaveData} serializes UNCONDITIONALLY
 * ({@code AbstractArrow.java:544}). {@code ItemStack.save} throws
 * {@code IllegalStateException: "Cannot encode empty ItemStack"}
 * ({@code ItemStack.java:409}) on an empty stack, so an {@code EMPTY} pickup
 * stack crashes the CHUNK SAVE for any dart alive at save time — the exact
 * stack CI measured on the pech arrow built the same way
 * (run 43282015367: {@code ServerLevel.save -> … -> AbstractArrow.addAdditionalSaveData -> ItemStack.save},
 * landed as {@code 87e706cf}). The dart was unreachable only because its
 * launcher ({@code AIDartAttack}) is not ported; this probe is the leg that
 * keeps it save-safe when that card lands.</p>
 */
@GameTestHolder(ThaumcraftClassic.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DartProjectileGameTest {

    /**
     * A fresh dart is save-safe and not pickable:
     * <ul>
     *   <li>{@code saveWithoutId(new CompoundTag())} must not throw — that is
     *       the chunk-save path, and it is what the CI crash proved;</li>
     *   <li>the tag must carry the {@code item} entry and it must parse back to
     *       one vanilla arrow (vanilla's own mob path uses the same stack,
     *       {@code AbstractSkeleton.java:189});</li>
     *   <li>the pickup MODE stays {@code DISALLOWED} — the original's
     *       {@code canBePickedUp = 0}, "the original dart is never picked up".
     *       A non-empty stack therefore changes no pickup semantics.</li>
     * </ul>
     * The dart is discarded before {@code succeed()}: the probe leaves nothing
     * alive for the shutdown save to serialize, so the probe cannot itself
     * become the timing-dependent failure this defect class produces.
     */
    @GameTest(template = "empty")
    public static void dart_spawnsWithASerializablePickupStack(final GameTestHelper helper) {
        final EntityDart dart = new EntityDart(CatalogGolems.DART.get(), helper.getLevel());
        dart.setPos(helper.absoluteVec(new Vec3(0.5, 1.0, 0.5)));                  // position FIRST
        helper.getLevel().addFreshEntity(dart);
        helper.assertTrue(dart.isAlive(), "the dart never entered the world");
        // 1.7.10 EntityArrow.canBePickedUp = 0 -> 1.21.1 Pickup.DISALLOWED: nothing is pickable.
        // `pickup` is a public field on 1.21.1's AbstractArrow (AbstractArrow.java:62), and
        // addAdditionalSaveData writes its ordinal (`compound.putByte("pickup", ...)`), so the mode is
        // pinned both live and in the serialized form.
        final AbstractArrow.Pickup mode = dart.pickup;
        helper.assertTrue(mode == AbstractArrow.Pickup.DISALLOWED,
                "the dart's pickup mode is " + mode + ", not DISALLOWED — "
                        + "the original dart is never picked up (canBePickedUp = 0)");
        // The chunk-save seam: addAdditionalSaveData serializes the pickup stack unconditionally.
        final CompoundTag tag = new CompoundTag();
        try {
            dart.saveWithoutId(tag);
        } catch (RuntimeException e) {
            // Minecraft wraps the save failure in a ReportedException ("Saving entity NBT"), so name
            // the ROOT cause too — the phrase that identifies this defect class is the leaf's.
            final StringBuilder chain = new StringBuilder(e.getClass().getSimpleName());
            chain.append(": ").append(e.getMessage());
            for (Throwable c = e.getCause(); c != null && c != e; c = c.getCause()) {
                chain.append(" <- ").append(c.getClass().getSimpleName()).append(": ").append(c.getMessage());
            }
            helper.fail("a fresh dart cannot be serialized — the chunk save that reaches it throws "
                    + chain);
        }
        helper.assertTrue(tag.contains("item"), "the dart's NBT carries no `item` pickup entry");
        helper.assertTrue(tag.getByte("pickup") == (byte) AbstractArrow.Pickup.DISALLOWED.ordinal(),
                "the serialized pickup mode is " + tag.getByte("pickup")
                        + ", not DISALLOWED's ordinal — the dart must stay not-pickable");
        final ItemStack parsed =
                ItemStack.parseOptional(helper.getLevel().registryAccess(), tag.getCompound("item"));
        helper.assertTrue(parsed.is(Items.ARROW),
                "the dart's saved pickup stack is not one vanilla arrow (parsed " + parsed + ")");
        helper.assertTrue(dart.getPickupItemStackOrigin().is(Items.ARROW),
                "the live pickup stack is not one vanilla arrow (getDefaultPickupItem contract)");
        dart.discard();
        helper.succeed();
    }
}
