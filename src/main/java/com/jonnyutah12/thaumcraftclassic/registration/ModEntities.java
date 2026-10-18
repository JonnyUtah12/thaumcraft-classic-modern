package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity type registration holder (TC-M2-01 registration architecture).
 *
 * <p>Every kind=entity row in docs/content-mapping.csv registers here with its
 * canonical id (the {@code new_id} column is the id contract,
 * docs/architecture/registration.md). Spawn placements (kind=mob_spawn) are
 * configured separately via {@code RegisterSpawnPlacementsEvent}.</p>
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    private ModEntities() {
    }
}
