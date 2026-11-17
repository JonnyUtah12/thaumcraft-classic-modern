package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import com.jonnyutah12.thaumcraftclassic.loot.FortunedUniformProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * SPINE-1: custom loot number providers, registered the house way
 * (DeferredRegister + one wiring line in {@link ModRegistration#init}).
 *
 * <p>{@code thaumcraft:fortuned_uniform} encodes the original
 * {@code 1 + rand.nextInt(span + fortune)} curve; the repo convention is
 * data-driven loot, and vanilla's number providers (uniform constants,
 * {@code ApplyBonusCount} formulas) cannot express fortune-inside-the-bound,
 * which is exactly the original formula, so this is the one registered
 * function the card allows.</p>
 */
public final class ModLootNumberProviders {

    public static final DeferredRegister<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPES =
            DeferredRegister.create(Registries.LOOT_NUMBER_PROVIDER_TYPE, ThaumcraftClassic.MOD_ID);

    /** {@link FortunedUniformProvider}: {@code base + rand.nextInt(span + fortune)}. */
    public static final DeferredHolder<LootNumberProviderType, LootNumberProviderType> FORTUNED_UNIFORM =
            LOOT_NUMBER_PROVIDER_TYPES.register("fortuned_uniform",
                    () -> new LootNumberProviderType(FortunedUniformProvider.CODEC));

    private ModLootNumberProviders() {
    }

    public static void register(final IEventBus modEventBus) {
        LOOT_NUMBER_PROVIDER_TYPES.register(modEventBus);
    }
}
