package com.jonnyutah12.thaumcraftclassic.client.fx;

/**
 * The bolt geometry the {@code fxZap} / {@code fxWispZap} senders describe
 * (TC-M7-02-c-s): {@code PacketFXZap.onMessage} (PacketFXZap.java:44-62) and
 * {@code PacketFXWispZap.onMessage} (PacketFXWispZap.java:41-46) both end in
 * {@code Thaumcraft.proxy.bolt(world, source, target)}, i.e. one
 * {@code FXLightningBolt} built from the two entities' AABB centres with a
 * fresh long seed, 6 segments, a 0.5 diameter and an 8-tick lifetime, then
 * {@code setType(2)} / {@code setWidth(0.125F)} / {@code finalizeBolt()}.
 *
 * <p>A 1.7.10 bolt is a fractal <em>line strip</em> drawn by a world renderer
 * (defaultFractal/finalizeBolt generate the vertex chain), not a billboard
 * particle, so the plan travels as data and the beam/bolt lane
 * (TC-M7-02-b-s, t_fe9e9f12) owns the vertex geometry and its renderer. The
 * plan is pure data, so the reference's endpoint maths and bolt parameters are
 * testable on the game-test server; the strip itself is not a
 * {@code Level#addParticle} hand-off.</p>
 */
public record TcBoltPlan(double fromX, double fromY, double fromZ,
        double toX, double toY, double toZ,
        long seed, int segments, float diameter, int lifetime, int type, float width) {
}
