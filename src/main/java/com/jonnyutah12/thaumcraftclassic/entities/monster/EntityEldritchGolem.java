package com.jonnyutah12.thaumcraftclassic.entities.monster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * {@code thaumcraft:eldritchgolem} — original
 * {@code thaumcraft.common.entities.monster.boss.EntityEldritchGolem}
 * (content-mapping {@code Thaumcraft:EldritchGolem}, kind=entity,
 * mod_index 31, display name "{@code %s} Eldritch Construct"). TC-M6-08-s-c.
 *
 * <p><b>Pinned from source</b> ({@code EntityEldritchGolem.java}): the
 * original {@code extends EntityThaumcraftBoss implements IEldritchMob,
 * IRangedAttackMob}; the port's base is {@link Monster} (the 1.21.1 home of
 * the 1.7.10 hostile-mob base). Hitbox {@code func_70105_a(1.75F, 3.5F)}
 * ({@code :54}) — 1.75 x 3.5. The attribute block ({@code :104-109}) sets
 * exactly three figures: movement speed {@code 0.3}, attack damage
 * {@code 10.0} and max health {@code 250.0}; the follow range is left at the
 * base's shared default (16.0, which 1.21.1's {@code Monster} supplies).
 * Registration {@code registerModEntity(EntityEldritchGolem.class,
 * "EldritchGolem", id, instance, 64, 3, true)}
 * ({@code ConfigEntities.java:119}) — tracking 64, interval 3, velocity
 * synced; the spawn egg mapping is
 * {@code ItemSpawnerEgg.addMapping("EldritchGolem", 5592405, 4210752)}
 * ({@code :120}) — 0x555555 / 0x404040.</p>
 *
 * <p><b>Not ported (out of scope on this card)</b>: the boss body — the
 * 100-tick spawn timer, the ranged beam attack, the crab summons, the boss
 * health bar and the armour bonus {@code func_70658_aO() + 6}. The beam's
 * client FX pair already landed in this tree through the beams card as
 * {@code client/fx/TcGolemBeamParticle} + {@code network/payload/fx/
 * FXBeamPulseGolemBossPayload} (see docs/feature-tracking.md).</p>
 */
public class EntityEldritchGolem extends Monster {

    /** Original hitbox figures ({@code func_70105_a(1.75F, 3.5F)}). */
    public static final float WIDTH = 1.75F;
    public static final float HEIGHT = 3.5F;
    /** Original {@code registerModEntity(..., 64, 3, true)}. */
    public static final int TRACKING_RANGE = 64;
    public static final int UPDATE_INTERVAL = 3;
    /** Original spawn-egg colours ({@code ItemSpawnerEgg.addMapping}, 5592405 / 4210752). */
    public static final int EGG_PRIMARY = 0x555555;
    public static final int EGG_SECONDARY = 0x404040;

    public EntityEldritchGolem(final EntityType<? extends EntityEldritchGolem> type, final Level level) {
        super(type, level);
        // Original constructor: this.func_110163_bv() (enablePersistence).
        this.setPersistenceRequired();
    }

    /** Original {@code func_110147_ax()} ({@code EntityEldritchGolem.java:104-109}), all three figures. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MAX_HEALTH, 250.0);
    }
}
