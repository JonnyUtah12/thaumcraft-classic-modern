package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * {@code thaumcraft:dart} — original
 * {@code thaumcraft.common.entities.projectile.EntityDart}
 * (content-mapping {@code Thaumcraft:Dart}, kind=entity, mod_index 8,
 * display name "Golem Dart"). TC-M6-08-s-c.
 *
 * <p><b>Pinned from source</b> ({@code EntityDart.java}): the original
 * {@code extends EntityArrow implements IProjectile, IEntityAdditionalSpawnData}
 * — 1.21.1's {@code EntityArrow} is {@link AbstractArrow}, the port's base.
 * The constructor sets {@code field_70155_l = 10.0} (the 1.7.10
 * {@code EntityArrow} field that became {@code AbstractArrow.setBaseDamage})
 * and the hitbox {@code func_70105_a(0.5F, 0.5F)}; the registration is
 * {@code registerModEntity(EntityDart.class, "Dart", id, instance, 64, 20, false)}
 * ({@code ConfigEntities.java:84}) — tracking 64, update interval 20, no
 * velocity sync. The original ships nothing to pick up ({@code EntityArrow}
 * with no {@code canBePickedUp} override). 1.21.1 splits that one 1.7.10 field
 * in two: the pickup <em>mode</em> is what gates pickup, and 1.21.1's default
 * {@code Pickup.DISALLOWED} ({@code AbstractArrow.java:62}) is the mode the
 * original's {@code canBePickedUp = 0} maps to, so the dart stays
 * never-picked-up. The pickup <em>stack</em> is not a pickup permission —
 * {@code AbstractArrow.addAdditionalSaveData} serializes it unconditionally
 * ({@code :544}) and {@code ItemStack.save} throws
 * {@code IllegalStateException: "Cannot encode empty ItemStack"}
 * ({@code ItemStack.java:409}) on an empty one, so an {@code EMPTY} stack here
 * crashes the CHUNK SAVE for any dart alive at save time. Measured on the pech
 * arrow built the same way (CI run 43282015367, landed as {@code 87e706cf});
 * the dart was unreachable only because the launcher path below is not ported.
 * The pickup stack is therefore one vanilla arrow, as on vanilla's own mob
 * path ({@code AbstractSkeleton.java:189} → the same ctor). No pickup
 * semantics change, so no BD row is added.</p>
 *
 * <p><b>Not ported (out of scope on this card)</b>: the launcher path
 * ({@code AIDartAttack}), the 5-particle smoke puff on the first client tick
 * and the {@code IEntityAdditionalSpawnData} aim broadcast. Those are golem
 * behaviour (TC-M6-08-s-b2) and FX respectively.</p>
 */
public class EntityDart extends AbstractArrow {

    /** Original {@code function field_70155_l / setBaseDamage} figure. */
    public static final double BASE_DAMAGE = 10.0;

    public EntityDart(final EntityType<? extends EntityDart> type, final Level level) {
        super(type, level);
        this.setBaseDamage(BASE_DAMAGE);
    }

    /**
     * The pickup stack, NOT a pickup permission: the dart is never picked up
     * because {@code pickup} stays at 1.21.1's default {@code Pickup.DISALLOWED}
     * (1.7.10's {@code canBePickedUp = 0}). It must not be {@code EMPTY}:
     * {@code AbstractArrow.addAdditionalSaveData} serializes it unconditionally
     * and {@code ItemStack.save} throws on an empty stack, which breaks the
     * chunk save. Probed by {@code dart_spawnsWithASerializablePickupStack}.
     */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    /** Original spawn-shot constructor ({@code EntityDart(World, LivingEntity, LivingEntity, float, float)}). */
    public static EntityDart shotFrom(final EntityType<EntityDart> type, final LivingEntity shooter,
                                      final Level level) {
        final EntityDart dart = new EntityDart(type, level);
        dart.setOwner(shooter);
        return dart;
    }
}
