package com.jonnyutah12.thaumcraftclassic.network.payload;

import com.jonnyutah12.thaumcraftclassic.ThaumcraftClassic;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C payload — the port's replacement for the open half of FML 1.7.10's
 * {@code FMLPacketGuiOpen} (BOOK-1 / t_1e6b18cc, content-mapping row
 * {@code Thaumcraft:GUI_THAUMONOMICON} → {@code thaumcraft:gui_thaumonomicon},
 * original {@code gui_id = 12}).
 *
 * <p>Why this payload exists (docs/BEHAVIORAL_DEVIATIONS.md BD-22): the
 * original's book is not a container. {@code ItemThaumonomicon.func_77659_a}
 * ends with {@code player.openGui(Thaumcraft.instance, 12, world, 0, 0, 0)}
 * ({@code ItemThaumonomicon.java:79}), and the {@code GuiResearchBrowser} the
 * client builds for id 12 extends {@code GuiScreen}
 * ({@code GuiResearchBrowser.java:41}) — zero slots, no {@code Container},
 * no {@code ContainerData}. 1.21.1's server-initiated open is
 * {@code Player#openMenu(IMenuProvider)}, which requires a real
 * {@code AbstractContainerMenu}: there is no containerless open, so a faithful
 * port cannot use the menu route without inventing a container the original
 * never had (a 0-slot phantom menu: a live menu id, the platform's slot/drag
 * machinery, and a client that refuses the open when no screen is registered
 * for the type). This payload is the smallest 1.21.1 seam that keeps the
 * browser containerless, exactly as the original wrote it.</p>
 *
 * <p><b>Payload state.</b> One field: the research category the book should
 * open on. The original's browser keeps its own category across reopens in a
 * static field ({@code GuiResearchBrowser.java:63} {@code selectedCategory},
 * recomputed on close at {@code :116-121}), so an empty category — what
 * {@code ItemThaumonomicon} sends, matching the original's all-zero
 * {@code openGui} x/y/z arguments — means "the last category the player was
 * reading, else the first category of the graph". The screen resolves that, and
 * the category is the only state that rides this payload.</p>
 *
 * @param category the category key to open on; empty for the browser's own
 *                 last-viewed/first-category default
 */
public record OpenThaumonomiconPayload(String category) implements CustomPacketPayload {

    public static final String EMPTY_CATEGORY = "";

    public static final Type<OpenThaumonomiconPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ThaumcraftClassic.MOD_ID, "open_thaumonomicon"));

    public static final StreamCodec<ByteBuf, OpenThaumonomiconPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenThaumonomiconPayload::category,
            OpenThaumonomiconPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
