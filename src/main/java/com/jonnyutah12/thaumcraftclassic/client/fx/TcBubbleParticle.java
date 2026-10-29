package com.jonnyutah12.thaumcraftclassic.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ported crucible bubble particle (TC-M7-02-s).
 *
 * <p>One instance is one {@code FXBubble}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/particles/FXBubble.java),
 * whose {@code onUpdate} (lines 48-66) fully replaces the vanilla base tick:
 * every tick it adds {@code bubblespeed} to Y, wobbles X/Z by
 * {@code (rand - rand) * 0.01} while rising, integrates the position, drags
 * every axis by 0.85, counts its life down, and swaps to the pop frame for the
 * last two ticks. All five of those numbers are in the tree, so this class
 * reproduces them directly; 1.21.1's {@code Particle.tick()} has the same
 * shape (integrate, then multiply by {@code friction}), so {@code rise} and
 * {@code wobble} ride on top of it and {@code friction = 0.85}.</p>
 *
 * <p>The original also culls the bubble by a render-distance test
 * (FxBubble.java:26-34, {@code visibleDistance}), which the platform does for
 * every particle in 1.21.1; that cull is not ported (BD-15).</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcBubbleParticle extends TextureSheetParticle {

    /** The original's FXBubble per-tick velocity drag (FxBubble.java:61-63). */
    static final float DRAG = 0.85F;

    /** The original's FXBubble X/Z wobble (FxBubble.java:53-56). */
    static final float WOBBLE = 0.01F;

    private final double rise;
    private final int popTicks;
    private final TextureAtlasSprite bodySprite;
    private final TextureAtlasSprite popSprite;

    TcBubbleParticle(final ClientLevel level, final double x, final double y, final double z,
            final double vx, final double vy, final double vz,
            final TcBubbleOptions options, final SpriteSet sprites) {
        // 4-argument form: the original's FXBubble motion is exactly the handed
        // jitter, so the 7-argument form's velocity re-normalisation is unwanted.
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.rise = options.rise();
        this.popTicks = TcBubbleOptions.POP_TICKS;
        this.lifetime = options.lifetime();
        // FXBubble render: half-extent = 0.1F * particleScale (FxBubble.java:32).
        this.quadSize = 0.1F * options.scale();
        this.rCol = TcFxPlans.red(options.color());
        this.gCol = TcFxPlans.green(options.color());
        this.bCol = TcFxPlans.blue(options.color());
        this.hasPhysics = false;   // the original's noClip (FxBubble.java:20)
        this.gravity = 0.0F;
        this.friction = DRAG;
        // FXBubble indexes two frames by hand (body=0, pop=1), not by age.
        // SpriteSet only exposes (age, lifetime) and random access, so an
        // age/lifetime pair that clamps to the ends of the list is how this
        // port asks for the first and the last frame deterministically.
        this.bodySprite = sprites.get(0, 1);
        this.popSprite = sprites.get(2, 1);
        this.setSprite(this.bodySprite);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        // FXBubble.onUpdate: the rise is added before the platform's friction,
        // matching the original's order (motionY += bubblespeed, then *= 0.85).
        this.yd += this.rise;
        if (this.rise > 0.0) {
            this.xd += (this.random.nextFloat() - this.random.nextFloat()) * WOBBLE;
            this.zd += (this.random.nextFloat() - this.random.nextFloat()) * WOBBLE;
        }
        // FxBubble.java:63-65: the pop frame takes over for the last two ticks.
        this.setSprite(this.lifetime - this.age <= this.popTicks ? this.popSprite : this.bodySprite);
    }

    /** The original draws its FX sheet fullbright (tessellator setBrightness(240)). */
    @Override
    protected int getLightColor(final float partialTick) {
        return 15 << 4 | 15 << 20;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // The bubble layer blends SRC_ALPHA + ONE_MINUS_SRC_ALPHA
        // (ParticleEngine.java:60-66, glBlendFunc(770, 771)) — the alpha sheet.
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider wired from {@link TcParticleProviders} for the JSON frame list. */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcBubbleOptions> {

        @Override
        public Particle createParticle(final TcBubbleOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcBubbleParticle(level, x, y, z, vx, vy, vz, options, this.sprites);
        }
    }
}
