package com.jonnyutah12.thaumcraftclassic.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
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
 * velocity sync. The original also ships nothing to pick up
 * ({@code EntityArrow} with no {@code canBePickedUp} override), reproduced by
 * the empty pickup stack.</p>
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

    /** The original dart is never picked up ({@code EntityArrow} default). */
    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    /** Original spawn-shot constructor ({@code EntityDart(World, LivingEntity, LivingEntity, float, float)}). */
    public static EntityDart shotFrom(final EntityType<EntityDart> type, final LivingEntity shooter,
                                      final Level level) {
        final EntityDart dart = new EntityDart(type, level);
        dart.setOwner(shooter);
        return dart;
    }
}
