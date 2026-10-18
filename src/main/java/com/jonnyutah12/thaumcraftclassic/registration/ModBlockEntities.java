package com.jonnyutah12.thaumcraftclassic.registration;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block entity type registration holder (TC-M2-01 registration architecture).
 *
 * <p>The original registered 73 tile entities by name (kind=tile_entity in
 * docs/content-mapping.csv). In 1.21.1 block entity types are bound to their
 * blocks, not to standalone ids: the id contract for those rows is the owning
 * block's id, and the tile_entity rows are inventory records only
 * (docs/architecture/registration.md).</p>
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ThaumcraftClassic.MOD_ID);

    private ModBlockEntities() {
    }
}
