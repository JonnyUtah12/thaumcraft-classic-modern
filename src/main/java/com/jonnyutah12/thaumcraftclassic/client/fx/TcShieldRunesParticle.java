package com.jonnyutah12.thaumcraftclassic.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The orbiting shield runes (TC-M7-02-c-s-f4) — the port of
 * {@code thaumcraft.client.fx.other.FXShieldRunes} (96 lines: constructor
 * 24-42, draw 44-82, tick 84-95).
 *
 * <p>The original is the runed-armour / shield flash: sent by
 * {@code PacketFXShield.onMessage} (PacketFXShield.java:38-79), it rides the
 * packet's source entity and aims a hemisphere of runes at the packet's
 * target.</p>
 *
 * <h2>Riding the entity</h2>
 * The original stores the entity as a live reference and dereferences it every
 * tick (FXShieldRunes.java:92-94). In 1.7.10 the reference stays resolvable
 * after the entity leaves the world, so the runes ride a corpse; the port
 * looks the id up in the client level, so a despawn removes the particle. That
 * despawn behaviour is the seam, not the rule: BD-46 states it and the
 * GameTest pins the observable half (a live entity keeps the mote following;
 * a removed one leaves no particle behind).</p>
 *
 * <h2>The orientation, and one thing the mined branch invented</h2>
 * The located draw rotates the fixed GL matrix by {@code 180 - yaw} about Y
 * then {@code -pitch} about X (FXShieldRunes.java:72-73) and scales by
 * {@code 0.4 * target.width} (line 74). The port composes the same two
 * rotations on top of the camera basis (BD-16 — the camera basis is composed,
 * never chased).</p>
 *
 * <p>The mined {@code wt/m7-fx-wisps} draft applied a per-tick
 * {@code roll += 5.5F} and cited {@code FXShieldRunes.java:115}. That file is
 * 96 lines long and has no roll line, so the spin has no located source and is
 * <em>not</em> shipped (D-017: name only what you can locate). The rune art's
 * apparent motion is the frame ramp, as the located code does it.</p>
 *
 * <p>Layer: the original's {@code glBlendFunc(770, 1)} (SRC_ALPHA, ONE) with
 * the {@code (220, 0)} lightmap pair (lines 48-49, 68-71) is 1.21.1's additive
 * {@link ParticleRenderType#PARTICLE_SHEET_LIT} at full block light and no sky
 * light; the mob form's 0.5 colour (line 63) rides as the particle tint.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TcShieldRunesParticle extends TextureSheetParticle {

    private final TcShieldOptions options;
    private final SpriteSet sprites;

    TcShieldRunesParticle(final ClientLevel level, final double x, final double y, final double z,
            final TcShieldOptions options, final SpriteSet sprites) {
        super(level, x, y, z);
        this.options = options;
        this.sprites = sprites;
        // particleMaxAge = 8 + rand(8 / 2), rolled plan-side (line 31).
        this.lifetime = options.lifetime();
        // glScaled(0.4 * target.width) (line 74); the port's quadSize is the
        // half-extent, so the billboard is as wide as the hemisphere the OBJ
        // drew at that scale. The constructor's particleScale = 1.0F (line 35)
        // is never read by this draw.
        this.quadSize = options.scale();
        this.gravity = FxShieldRules.GRAVITY;   // line 29
        this.hasPhysics = false;                // the noClip pair, lines 32-34
        this.setSize(FxShieldRules.AABB_SIZE, FxShieldRules.AABB_SIZE);   // line 33
        // glColor4f(b, b, b, alpha) (line 75): b is 0.5 on the mob branch
        // (line 63); the alpha is the draw's, computed per render below.
        final float brightness = FxShieldRules.brightness(options.mob());
        this.rCol = brightness;
        this.gCol = brightness;
        this.bCol = brightness;
        this.alpha = FxShieldRules.alpha(0.0F);
    }

    @Override
    public void tick() {
        // Particle.tick() carries the original's `age++ >= lifetime` gate and
        // the `prev = pos` snapshot (FXShieldRunes.java:85-90).
        super.tick();
        final Entity target = this.level.getEntity(this.options.sourceEntityId());
        if (target == null) {
            // The located code dereferences a raw reference; the id lookup is
            // the port's seam (class javadoc, BD-46).
            this.remove();
            return;
        }
        // FXShieldRunes.java:92-94: x, the AABB centre height, z.
        this.setPos(target.getX(),
                (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0,
                target.getZ());
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        // The fade, the frame and the alpha are all the draw's (lines 54, 60,
        // 75): fade = (age + partialTick) / lifetime, frame =
        // min(15, (int)(14F * fade) + 1) over hemis / ripple, alpha =
        // min(1, (1 - fade) * 3).
        final float fade = FxShieldRules.fade(this.age, partialTick, this.lifetime);
        this.setSprite(this.sprites.get(FxShieldRules.spriteIndex(FxShieldRules.frame(fade)),
                FxShieldRules.FRAME_CLAMP));
        this.alpha = FxShieldRules.alpha(fade);
        final Quaternionf rotation = new Quaternionf();
        getFacingCameraMode().setRotation(rotation, camera, partialTick);
        // glRotatef(180 - yaw, 0, 1, 0) then glRotatef(-pitch, 1, 0, 0)
        // (lines 72-73), composed on the camera basis; the camera yaw/pitch
        // ride in because the original drew in a camera-transformed matrix
        // stack (BD-16, restated for this family in BD-46).
        rotation.rotateY(FxShieldRules.toRadians(180.0F - this.options.yaw() + camera.getYRot()));
        rotation.rotateX(FxShieldRules.toRadians(-this.options.pitch() + camera.getXRot()));
        renderRotatedQuad(buffer, camera, rotation, partialTick);
    }

    @Override
    protected int getLightColor(final float partialTick) {
        // (220, 0) through OpenGlHelper (FXShieldRunes.java:68-71): block
        // light 220 -> the maximum, sky light 0.
        return 15 << 4;
    }

    @Override
    public ParticleRenderType getRenderType() {
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE) (FXShieldRunes.java:49).
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    /**
     * Provider wired from {@link TcParticleProviders} for both frame lists:
     * {@code shield_runes.json} (hemis1..hemis15) for the plain form and
     * {@code shield_runes_mob.json} (ripple1..ripple15) for the mob form. The
     * form is chosen by {@link TcShieldOptions#getType()}, which is what
     * resolves to this provider — the original switches frames and colour
     * inside one draw (FXShieldRunes.java:61-66), 1.21.1 switches them per
     * registered type.
     */
    @OnlyIn(Dist.CLIENT)
    public record Provider(SpriteSet sprites) implements ParticleProvider<TcShieldOptions> {

        @Override
        public Particle createParticle(final TcShieldOptions options, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            return new TcShieldRunesParticle(level, x, y, z, options, this.sprites);
        }
    }
}
