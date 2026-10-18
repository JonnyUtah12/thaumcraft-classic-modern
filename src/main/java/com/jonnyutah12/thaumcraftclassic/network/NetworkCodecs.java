package com.jonnyutah12.thaumcraftclassic.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * Shared stream codecs for value types that have no vanilla STREAM_CODEC.
 */
public final class NetworkCodecs {

    /** Vec3 as three float components (the original FX packets used float fields). */
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.of(
            (buf, vec) -> {
                buf.writeFloat((float) vec.x());
                buf.writeFloat((float) vec.y());
                buf.writeFloat((float) vec.z());
            },
            buf -> new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));

    private NetworkCodecs() {
    }
}
