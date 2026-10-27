package com.jonnyutah12.thaumcraftclassic.tiles;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Ownership base tile (content-mapping.csv {@code thaumcraft:tileowned}).
 *
 * <p>Original: {@code thaumcraft.common.tiles.TileOwned} — stores the placing
 * player's name plus an access list ("0"+name = guest access, "1"+name =
 * owner-equivalent). Used by the arcane pressure plate and bore (this card)
 * and by the warding door (TC-M6-04d); the block's {@code setPlacedBy}
 * stamps the owner, mirroring the original's onBlockPlacedBy.</p>
 */
public class TileOwned extends BlockEntity {

    public String owner = "";
    public List<String> accessList = new ArrayList<>();
    public boolean safeToRemove = false;

    public TileOwned(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** Original {@code canUpdate()=false}: owned tiles never tick themselves. */
    public void serverTick() {
        // no-op, matching the original
    }

    public boolean hasAccess(final String playerName) {
        return owner.equals(playerName)
                || accessList.contains("0" + playerName)
                || accessList.contains("1" + playerName);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("owner", owner);
        final ListTag access = new ListTag();
        for (final String name : accessList) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("name", name);
            access.add(entry);
        }
        tag.put("access", access);
    }

    @Override
    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.getString("owner");
        accessList = new ArrayList<>();
        for (final Tag entry : tag.getList("access", Tag.TAG_COMPOUND)) {
            accessList.add(((CompoundTag) entry).getString("name"));
        }
    }
}
