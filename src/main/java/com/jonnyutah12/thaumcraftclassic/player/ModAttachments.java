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

    private ModAttachments() {
    }
}
