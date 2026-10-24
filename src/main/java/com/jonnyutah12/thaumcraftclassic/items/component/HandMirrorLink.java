package com.jonnyutah12.thaumcraftclassic.items.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/**
 * The hand mirror's mirror link (content-mapping.csv {@code thaumcraft:handmirror}).
 *
 * <p>Original: the {@code linkX}/{@code linkY}/{@code linkZ}/{@code linkDim}
 * ints plus the {@code dimname} string the original wrote onto the item NBT in
 * {@code ItemHandMirror.onItemUseFirst}. Field-for-field the same data: the
 * linked mirror's position, the dimension it lives in, and the display string
 * the original stored for the tooltip. The original stored the dimension as a
 * 1.7 numeric id and the provider's display name (e.g. "Overworld"); 1.21.1
 * has no numeric dimension ids, so the dimension travels as its {@link
 * ResourceKey} and the display string is the dimension's id path — the tooltip
 * shows the same information, honestly labelled (deviation recorded on the CSV
 * row).</p>
 */
public record HandMirrorLink(BlockPos pos, ResourceKey<Level> dimension, String dimName) {

    public static final Codec<HandMirrorLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(HandMirrorLink::pos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(HandMirrorLink::dimension),
            Codec.STRING.fieldOf("dim_name").forGetter(HandMirrorLink::dimName))
            .apply(instance, HandMirrorLink::new));

    /** The link display string the original stored in {@code dimname}. */
    public static String dimNameFor(ResourceKey<Level> dimension) {
        final ResourceLocation id = dimension.location();
        return id.getPath();
    }
}
