package com.jonnyutah12.thaumcraftclassic.entities.golems.ai;

import com.jonnyutah12.thaumcraftclassic.entities.golems.EntityGolemBase;
import com.jonnyutah12.thaumcraftclassic.entities.golems.GolemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The Filter core's "fetch the dropped item at home" goal — the original's
 * {@code thaumcraft.common.entities.ai.inventory.AIItemPickup}
 * ({@code reference/decompiled/thaumcraft/thaumcraft/common/entities/ai/inventory/AIItemPickup.java},
 * 129 lines).
 *
 * <p><b>Priority (the original's {@code setupGolem()}).</b> Installed at
 * priority {@code 2} for core {@code 2} (Filter, {@code EntityGolemBase.java:242}).</p>
 *
 * <h2>Pinned numbers</h2>
 * <ul>
 *   <li>mutex bits {@code 3} ({@code AIItemPickup.java:20}).</li>
 *   <li>Gate: {@code tickCount % Config.golemDelay > 0 ? false : findItem()}
 *       ({@code AIItemPickup.java:24}) → {@link GolemConfig#GOLEM_DELAY} = 5.</li>
 *   <li>The search box is the home block 1×1×1 expanded by
 *       {@code dmod = getRange()} on every axis
 *       ({@code AIItemPickup.java:30-43}); a candidate must be an
 *       {@code ItemEntity} younger than {@code 5} ticks
 *       ({@code age < 5}, {@code AIItemPickup.java:49}), wanted by the
 *       golem's inventory (empty inventory accepts anything,
 *       {@code AIItemPickup.java:51-52}), and either the hands are empty or the
 *       stack is stack-strict-equal to the carry and fits the carry space
 *       ({@code AIItemPickup.java:54-58}).</li>
 *   <li>The winner is the one nearest the <em>golem</em> whose distance to
 *       <em>home</em> stays within {@code dmod * dmod}
 *       ({@code AIItemPickup.java:59-68}) — squared.</li>
 *   <li>The budget is {@code count = 200} ({@code AIItemPickup.java:126});
 *       {@code canContinueToUse} burns it with {@code count-- > 0}
 *       ({@code AIItemPickup.java:77}).</li>
 *   <li>Look speed {@code 30, 30} ({@code AIItemPickup.java:87}); pickup radius
 *       {@code 2.0} ({@code AIItemPickup.java:89}); volume {@code 0.2} with the
 *       vanilla double-random pitch spread
 *       ({@code AIItemPickup.java:116-121}).</li>
 *   <li>The picked amount is the whole stack when it is smaller than
 *       {@code getCarrySpace()}, otherwise the carry space
 *       ({@code AIItemPickup.java:98-102}).</li>
 * </ul>
 *
 * <h2>Deviations</h2>
 * <ul>
 *   <li>DEVIATION: 1.7.10 compared {@code EntityItem.field_145804_b} (the
 *       item's <em>age</em>) against {@code 5}; 1.21.1 keeps the same semantic
 *       in {@link ItemEntity#getAge()}, so the gate reads the age. The
 *       companion field {@code delayBeforePickup} has no 1.7.10 equivalent in
 *       this goal — the goals that drop items ({@code AIHomeDrop},
 *       {@code AIEmptyDrop}) set the modern pickup delay instead of an age, so a
 *       stack they drop is <em>not</em> age-gated away here.
 *       // SEAM: give the drop goals an age setter (none exists in 21.1.259) and
 *       // the original's age gate lines up exactly.</li>
 *   <li>DEVIATION: the pickup sound is the modern
 *       {@link SoundEvents#ITEM_PICKUP} for 1.7.10's {@code "random.pop"}
 *       ({@code AIItemPickup.java:118}), played from the item entity as the
 *       original did, in the {@code NEUTRAL} category that 1.7.10's
 *       {@code World.playSound(Entity, ...)} overload used.</li>
 *   <li>DEVIATION: the original shrank the item's stack in place and let
 *       {@code EntityItem} self-destruct in its tick
 *       ({@code AIItemPickup.java:105}); 1.21.1's {@code ItemEntity.tick}
 *       discards an empty stack the same way, so the port mutates the stack and
 *       does not discard explicitly.</li>
 *   <li>DEVIATION: the original's {@code InventoryUtils.areItemStacksEqualStrict}
 *       ({@code AIItemPickup.java:56}) is 1.21.1's
 *       {@link ItemStack#isSameItemSameComponents} — the exact item + components
 *       comparison.</li>
 * </ul>
 */
public final class AIItemPickup extends GolemGoal {

    /** Original {@code targetEntity} ({@code AIItemPickup.java:15}). */
    private Entity targetEntity;

    /** Original {@code count} ({@code AIItemPickup.java:16}) — the run budget. */
    private int count = 0;

    public AIItemPickup(final EntityGolemBase golem) {
        super(golem);
        // Original setMutexBits(3) (AIItemPickup.java:20) — mutex bits MOVE|LOOK.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * Original {@code ground()} ({@code AIItemPickup.java:23-25}).
     */
    @Override
    public boolean canUse() {
        return this.golem.tickCount % GolemConfig.GOLEM_DELAY > 0 ? false : this.findItem();
    }

    /**
     * Original {@code findItem} ({@code AIItemPickup.java:27-74}) — the nearest
     * droppable item within the golem's range of home.
     */
    private boolean findItem() {
        double range = Double.MAX_VALUE;
        final float dmod = this.golem.getRange();
        final BlockPos home = this.golem.getRestrictCenter();
        final AABB box = new AABB(home.getX(), home.getY(), home.getZ(),
                home.getX() + 1, home.getY() + 1, home.getZ() + 1).inflate(dmod, dmod, dmod);
        final List<Entity> targets = this.level().getEntities(this.golem, box, target -> true);
        if (targets.isEmpty()) {
            return false;
        }
        for (final Entity entity : targets) {
            if (entity instanceof ItemEntity item
                    && item.getAge() < 5
                    && (this.golem.getInventory().allEmpty()
                    || this.golem.getInventory()
                            .getAmountNeededSmart(item.getItem(), this.golem.getUpgradeAmount(5) > 0) > 0)
                    && (this.golem.getCarried() == null
                    || (ItemStack.isSameItemSameComponents(this.golem.getCarried(), item.getItem())
                    && item.getItem().getCount() <= this.golem.getCarrySpace()))) {
                final double distance =
                        entity.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
                final double distanceToGolem = entity.distanceToSqr(this.golem.position());
                if (distanceToGolem < range && distance <= dmod * dmod) {
                    range = distanceToGolem;
                    this.targetEntity = entity;
                }
            }
        }
        return this.targetEntity != null;
    }

    /**
     * Original {@code hasExecute()} ({@code AIItemPickup.java:76-78}) — budget,
     * an unfinished path and a live target.
     */
    @Override
    public boolean canContinueToUse() {
        return this.count-- > 0
                && !this.golem.getNavigation().isDone()
                && this.targetEntity != null
                && !this.targetEntity.isRemoved();
    }

    /**
     * Original {@code startExecuting()} ({@code AIItemPickup.java:125-128}) —
     * startExecuting: budget, then path to the item at the golem's own speed.
     */
    @Override
    public void start() {
        this.count = 200;
        this.golem.getNavigation().moveTo(this.targetEntity, this.golem.getSpeed());
    }

    /**
     * Original {@code updateTask()} ({@code AIItemPickup.java:86-92}) — watch the
     * item and pick it up within {@code 2.0}.
     */
    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.targetEntity, 30.0F, 30.0F);
        final double dist = this.golem.distanceTo(this.targetEntity);
        if (dist <= 2.0) {
            this.pickUp();
        }
    }

    /**
     * Original {@code resetTask()} ({@code AIItemPickup.java:80-84}) —
     * resetTask: budget, target and path are all cleared.
     */
    @Override
    public void stop() {
        this.count = 0;
        this.targetEntity = null;
        this.golem.getNavigation().stop();
    }

    /**
     * 1.7.10's {@code EntityAITasks} ran {@code updateTask} every tick, so the
     * pinned 200-tick budget needs the same cadence here.
     */
    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    /**
     * Original {@code pickUp} ({@code AIItemPickup.java:94-123}) — moves the
     * stack (bounded by the carry space) from the item entity into the carry and
     * plays the pickup sound.
     */
    private void pickUp() {
        int amount = 0;
        if (this.targetEntity instanceof ItemEntity item) {
            final ItemStack stack = item.getItem().copy();
            if (item.getItem().getCount() < this.golem.getCarrySpace()) {
                amount = item.getItem().getCount();
            } else {
                amount = this.golem.getCarrySpace();
            }
            stack.setCount(amount);
            // DEVIATION: the original shrank the stack in place and let the
            // entity self-destruct (AIItemPickup.java:105); 1.21.1's
            // ItemEntity.tick discards an empty stack the same way.
            item.getItem().shrink(amount);
            if (this.golem.getCarried() == null) {
                this.golem.setCarried(stack);
            } else {
                this.golem.getCarried().setCount(this.golem.getCarried().getCount() + amount);
            }
        }
        if (amount != 0) {
            // The original's World.playSound(Entity, ...) overload played the
            // sound at the entity's own position (AIItemPickup.java:114-121);
            // 1.21.1's playSound(Player, Entity, ...) is that overload.
            this.level().playSound(null, this.targetEntity, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2F,
                    ((this.level().getRandom().nextFloat() - this.level().getRandom().nextFloat()) * 0.7F + 1.0F)
                            * 2.0F);
        }
    }
}
