package com.jonnyutah12.thaumcraftclassic.player;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Player data attachment registration (TC-M2-02 persistence substrate).
 *
 * <p>Both attachments serialize with the lenient codecs on the records, are copied on
 * death ({@code copyOnDeath}, applied by NeoForge through {@code PlayerEvent.Clone}),
 * and sync server-to-client automatically: NeoForge sends the initial payload on join,
 * respawn and dimension change, and pushes updates whenever {@code setData} is called.
 * Persistence across logout/login is handled by the vanilla player save file — the
 * attachments ride inside its {@code neoforge:attachments} compound.</p>
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ThaumcraftClassic.MOD_ID);

    public static final Supplier<AttachmentType<PlayerKnowledgeData>> PLAYER_KNOWLEDGE =
            ATTACHMENT_TYPES.register("player_knowledge",
                    () -> AttachmentType.builder(PlayerKnowledgeData::empty)
                            .serialize(PlayerKnowledgeData.CODEC)
                            .copyOnDeath()
                            .sync(ByteBufCodecs.fromCodec(PlayerKnowledgeData.CODEC))
                            .build());

    public static final Supplier<AttachmentType<PlayerWarpData>> PLAYER_WARP =
            ATTACHMENT_TYPES.register("player_warp",
                    () -> AttachmentType.builder(PlayerWarpData::empty)
                            .serialize(PlayerWarpData.CODEC)
                            .copyOnDeath()
                            .sync(ByteBufCodecs.fromCodec(PlayerWarpData.CODEC))
                            .build());

    /**
     * The default value of {@link #BIOME_RECORD}, in a field whose declared type
     * is {@code Supplier<Map<String,String>>}: {@code AttachmentType} has both
     * {@code builder(Supplier<T>)} and {@code builder(Function<IAttachmentHolder,
     * T>)}, and an inline method reference is ambiguous between them, while a
     * lambda pins T to {@code HashMap} and then {@code serialize(Codec<Map>)}
     * does not fit.
     */
    private static final Supplier<java.util.Map<String, String>> BIOME_RECORD_FACTORY =
            java.util.HashMap::new;

    /**
     * TC-M5-04-s-b: the biome a chunk column held before taint converted it,
     * keyed by the chunk-local 4×4 biome footprint
     * ({@code "<cellX>_<cellZ>"}, {@code (coord >> 2) & 3}). Server-side only —
     * the client learns the biome through {@code BiomeChangePayload} — and
     * persisted with the chunk, so a converted region keeps its restore record
     * across reloads. The 1.21.1 stand-in for the biome array 1.7.10's chunk
     * primer kept next to the editable byte; see
     * {@code TaintBiomes.generatedBiomeAt} and BD-16 in
     * docs/architecture/taint.md.
     */
    public static final Supplier<AttachmentType<java.util.Map<String, String>>> BIOME_RECORD =
            ATTACHMENT_TYPES.register("taint_biome_record",
                    // Codec.unboundedMap is a Codec<Map<K,V>> (UnboundedMapCodec
                    // implements Codec<java.util.Map>), so the attachment type is
                    // the mutable Map.
                    () -> AttachmentType.builder(BIOME_RECORD_FACTORY)
                            .serialize(com.mojang.serialization.Codec.unboundedMap(
                                    com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.STRING))
                            .build());

    private ModAttachments() {
    }
}
