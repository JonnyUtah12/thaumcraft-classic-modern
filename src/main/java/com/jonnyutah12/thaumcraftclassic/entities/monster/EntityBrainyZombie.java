package com.jonnyutah12.thaumcraftclassic.entities.monster;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * The Angry Zombie — {@code thaumcraft:brainyzombie} (content-mapping
 * {@code Thaumcraft:BrainyZombie}, mod_index 19).
 *
 * <p>Original: {@code thaumcraft.common.entities.monster.EntityBrainyZombie}, 54
 * lines, {@code extends EntityZombie}. Its whole surface, read from the source:
 * two attribute overrides and the reinforcement attribute pinned at 0.0
 * ({@code :16-18}), one extra target goal ({@code :23}), an armour-total
 * override (+3 capped at 20, {@code :34-41}) and a rewritten death-drop body
 * ({@code :43-53}). {@code func_70097_a} ({@code :26-28}) and
 * {@code func_70074_a} ({@code :30-32}) are <b>pure {@code super}
 * passthroughs</b> — they change nothing, so this class does not re-declare the
 * 1.21.1 equivalents ({@code hurt(DamageSource, float)},
 * {@code doHurtTarget(Entity)}); re-declaring them would add code the original
 * never had.</p>
 *
 * <h2>Shape mapping</h2>
 * <ul>
 *   <li>{@code func_110147_ax} ({@code :14-19}) — 1.21.1 builds attributes from
 *       a supplier instead of mutating live instances, so the three writes
 *       become {@link #createAttributes()} entries, shipped through
 *       {@code EntityAttributeCreationEvent} in {@code CatalogEntities}.</li>
 *   <li>{@code field_110186_bp} at 0.0 ({@code :18}) — stable_11 leaves this
 *       SRG name unmapped (it is not in
 *       {@code reference/mcp-mappings/fields_1.7.10_mcp_stable_11.csv}, which is
 *       why the decompile shows the raw name). It is the zombie parent's
 *       reinforcement attribute: 1.21.1's {@code Attributes.SPAWN_REINFORCEMENTS_CHANCE}
 *       is registered as {@code "zombie.spawn_reinforcements"}, default 0.0,
 *       range 0.0..1.0 ({@code Attributes.java:89-91}). It is written and
 *       asserted, not inferred: 1.21.1's {@code Zombie} consumes it in its
 *       reinforcement branch, so pinning the base to 0.0 is the effect the
 *       original asked for.</li>
 *   <li>{@code func_70658_aO()} ({@code :34-41}) — 1.21.1 has no armour-total
 *       hook; worn armour rides on the {@code ARMOR} attribute, so the +3 over
 *       the vanilla zombie's own base of 2.0 ({@code Zombie.java:74}) ships as
 *       the attribute base through the original's own cap.</li>
 *   <li>{@code field_70715_bh.func_75776_a(1, new EntityAIHurtByTarget(this,
 *       false))} ({@code :23}) — {@code field_70715_bh} = targetTasks (fields
 *       CSV), and the 1.7.10 second argument is the "not passive" flag: it
 *       counter-attacks and alerts nobody. That is 1.21.1's plain
 *       {@code HurtByTargetGoal(this)} — the parent's own variant alerts
 *       ZombifiedPiglins ({@code Zombie.java:113}), so the parent's target slot
 *       1 is replaced, not stacked.</li>
 *   <li>{@code func_70628_a(boolean, int)} ({@code :43-53}) —
 *       {@code dropFewItems(recentlyHit, looting)} →
 *       {@link #dropCustomDeathLoot(ServerLevel, DamageSource, boolean)}. The
 *       original calls no super, so neither does this. {@code func_145779_a(Item,
 *       int)} = {@code dropItem} → 1.21.1's {@code spawnAtLocation(ItemLike,
 *       int)}; {@code func_70099_a(ItemStack, float)} = {@code entityDropItem}
 *       → {@code spawnAtLocation(ItemStack, float)} (the float is the throw
 *       velocity). The rolls use the WORLD's RNG
 *       ({@code field_70170_p.field_73012_v}, fields CSV) — here
 *       {@code ServerLevel#getRandom()} — as the original's do.</li>
 *   <li>The goal set: {@code reference/decompiled} carries no 1.7.10 vanilla
 *       {@code EntityZombie}, so the parent's goal slots are not readable. What
 *       1.21.1's own {@code Zombie} installs ({@code Zombie.java:103-117}) is
 *       the same list plus the features that post-date 1.7.10, so this class
 *       installs the parent's shape — melee, wander, watch-closest, look-idle
 *       and the player / villager / iron-golem target trio — at 1.21.1's slots,
 *       and installs NONE of the post-1.7.10 goals: no
 *       {@code MoveThroughVillageGoal} (village patrol, {@code :111}), no
 *       turtle-egg attack ({@code :103}), no turtle target ({@code :117}), no
 *       alerting hurt goal ({@code :113}), and door breaking is refused through
 *       {@link #supportsBreakDoorGoal()} ({@code :155}). Every kept and removed
 *       goal is recorded in the content-mapping row's notes.</li>
 *   <li>Registration args {@code (64, 3, true)}
 *       ({@code ConfigEntities.java:95}) — {@code clientTrackingRange(4)}
 *       (1.21.1 counts chunks: 64 blocks = 4) and {@code updateInterval(3)};
 *       1.21.1 always sends velocity updates for a mob built this way, which is
 *       the original's {@code true}.</li>
 *   <li>The box: the class never resizes itself (no {@code func_70105_a} in the
 *       54 lines), so it is the vanilla zombie's 0.6 x 1.95 with eye height
 *       1.74 ({@code EntityType.java:770-771}).</li>
 * </ul>
 *
 * <p>Every figure lives in {@link BrainyZombieAiLogic} and is pinned world-free
 * by {@code BrainyZombieAiLogicTest}; the live registry id, the hitbox, the
 * attributes, the goal set, the target acquisition and the drop rolls are
 * proven in the world by {@code ZombieMobGameTest}.</p>
 */
public class EntityBrainyZombie extends Zombie {

    /** {@code :51} — the brain the original throws.
     *  {@code ConfigItems.itemZombieBrain} = {@code thaumcraft:itemzombiebrain},
     *  whose content-mapping row ({@code docs/content-mapping.csv} line 230,
     *  {@code Thaumcraft:ItemZombieBrain}) is still {@code not_started}: the
     *  item is not registered by this port yet. The branch is therefore wired
     *  behind a live registry check — it lights up the day the item lands, and
     *  until then the mob drops exactly what the port can show. */
    public static final ResourceLocation BRAIN_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "itemzombiebrain");

    public EntityBrainyZombie(final EntityType<? extends Zombie> type, final Level level) {
        super(type, level);
    }

    /** The original's registration args are {@code (64, 3, true)} —
     *  {@code ConfigEntities.java:95}. */
    public static EntityType<EntityBrainyZombie> buildType(final MobCategory category) {
        return EntityType.Builder.of(EntityBrainyZombie::new, category)
                .sized(BrainyZombieAiLogic.BRAINY_WIDTH, BrainyZombieAiLogic.BRAINY_HEIGHT) // :770 (the class never resizes)
                .eyeHeight(1.74F)                                                           // :771
                .clientTrackingRange(BrainyZombieAiLogic.TRACK_RANGE_CHUNKS)                // 64 blocks (:95)
                .updateInterval(BrainyZombieAiLogic.UPDATE_INTERVAL)                        // 3 (:95)
                .build("brainyzombie");
    }

    /** {@code func_110147_ax} ({@code :14-19}) plus the armour override
     *  ({@code :34-41}). */
    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, BrainyZombieAiLogic.BRAINY_MAX_HEALTH)               // :16
                .add(Attributes.ATTACK_DAMAGE, BrainyZombieAiLogic.BRAINY_ATTACK_DAMAGE)         // :17
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, BrainyZombieAiLogic.BRAINY_REINFORCEMENTS) // :18
                .add(Attributes.ARMOR, BrainyZombieAiLogic.armorBase());                          // :34-41 over Zombie.java:74
    }

    /** The parent's goal shape at 1.21.1's slots, plus the original's own target
     *  goal — and none of the goals 1.7.10 never had. See the class javadoc. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));                 // Zombie.java:110
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));           // :112
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));         // :104
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));                         // :105
        // :23 — target slot 1, EntityAIHurtByTarget(this, false): counter-attack,
        // alert nobody. This replaces the 1.21.1 parent's alerting variant (:113).
        this.targetSelector.addGoal(BrainyZombieAiLogic.BRAINY_HURT_TARGET_SLOT,
                new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));            // :114
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false)); // :115
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));          // :116
    }

    /** 1.7.10 zombies do not break doors: 1.21.1 installs its door goal from
     *  {@code Zombie#setCanBreakDoors} (Natural difficulty and HARD, on a
     *  1-in-10 finalizeSpawn roll), and this is the seam that refuses it
     *  ({@code Zombie.java:155-157}). */
    @Override
    protected boolean supportsBreakDoorGoal() {
        return false;
    }

    /** {@code :43-53} — 3 world-RNG coin flips drop 1 rotten flesh each, then
     *  the looting-sensitive brain gate. The original calls no super, so this
     *  does not. */
    @Override
    protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source,
            final boolean causedByPlayer) {
        final int looting = lootingLevel(source);                       // :43 int i
        for (int a = 0; a < BrainyZombieAiLogic.BRAINY_FLESH_ROLLS; a++) {   // :44
            if (BrainyZombieAiLogic.dropsFlesh(level.getRandom().nextBoolean())) {  // :45 (world RNG)
                // :46 func_145779_a(rotten_flesh, 1) = dropItem(count, Y offset
                // 0.0). The count goes in the STACK: 1.21.1's
                // spawnAtLocation(ItemLike, int) passes its int as the OFFSET
                // (Entity.java:1853-1855), not the count.
                this.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH,
                        BrainyZombieAiLogic.BRAINY_FLESH_STACK), BrainyZombieAiLogic.FLESH_DROP_OFFSET_Y);
            }
        }
        // :50-52 — the gate fires on the world RNG, and the thrown item is the
        // zombie brain the port has not registered yet (see BRAIN_ITEM_ID).
        if (BrainyZombieAiLogic.dropsBrain(level.getRandom().nextInt(
                BrainyZombieAiLogic.BRAIN_ROLL_MODULO), looting) && brainItemRegistered()) {
            this.spawnAtLocation(new ItemStack(BuiltInRegistries.ITEM.get(BRAIN_ITEM_ID)),
                    BrainyZombieAiLogic.BRAINY_BRAIN_OFFSET_Y);
        }
    }

    /** The looting level of the kill: 1.7.10 hands {@code dropFewItems} the
     *  number directly; 1.21.1 derives it from the killer's weapon, so the
     *  killer is resolved from the damage source (a non-kill
     *  source has none, which is looting 0, as in the original's
     *  {@code i = 0} calls). */
    protected static int lootingLevel(final DamageSource source) {
        final Entity killer = source.getEntity();
        if (killer instanceof LivingEntity living) {
            // 21.1.259 shape: Enchantments.LOOTING is a ResourceKey and the
            // level helper wants a Holder, and minecraft:enchantment is a
            // datapack registry — resolve through the level's registry access,
            // the pattern TileArcaneBore.java:216 already uses in this repo.
            final Holder<Enchantment> looting = living.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.LOOTING);
            return EnchantmentHelper.getEnchantmentLevel(looting, living);
        }
        return 0;
    }

    /** {@code :50-52} ships as data behind a live check: the brain item is a
     *  not_started content-mapping row, so the branch must not fabricate a
     *  drop of an unregistered item. */
    protected static boolean brainItemRegistered() {
        return BuiltInRegistries.ITEM.containsKey(BRAIN_ITEM_ID);
    }
}
