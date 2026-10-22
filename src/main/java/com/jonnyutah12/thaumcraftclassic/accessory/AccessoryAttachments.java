package com.jonnyutah12.thaumcraftclassic.accessory;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Fallback provider's player attachment registration (TC-M8-01).
 *
 * <p>Deliberately NOT {@code copyOnDeath}: accessory persistence across death
 * is keepInventory-aware and owned by {@link AccessoryLifecycle} (drop at
 * death when keepInventory is off, copy on respawn when on) — a blanket
 * copy-on-death would duplicate stacks against the death drop. S2C sync is
 * automatic like {@code ModAttachments}' (join/respawn/dimension-change plus
 * every {@code setData}); the client mirror is display-only, the server is
 * authoritative. Persistence across logout/login rides in the vanilla player
 * save, same as the TC-M2-02 attachments.</p>
 */
public final class AccessoryAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ThaumcraftClassic.MOD_ID);

    public static final Supplier<AttachmentType<AccessoryData>> ACCESSORIES =
            ATTACHMENT_TYPES.register("accessories",
                    () -> AttachmentType.builder(AccessoryData::empty)
                            .serialize(AccessoryData.CODEC)
                            .sync(ByteBufCodecs.fromCodec(AccessoryData.CODEC))
                            .build());

    private AccessoryAttachments() {
    }
}
