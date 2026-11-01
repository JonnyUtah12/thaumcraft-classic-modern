package com.jonnyutah12.thaumcraftclassic.client.entity;

import com.jonnyutah12.thaumcraftclassic.entities.monster.EntityEldritchGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * The eldritch golem renderer — Thaumcraft 4.2.3.5
 * {@code thaumcraft.client.renderers.entity.RenderEldritchGolem}, transcribed
 * from {@code reference/decompiled/thaumcraft/thaumcraft/client/renderers/entity/RenderEldritchGolem.java}
 * (47 lines). Card TC-M6-08-s-c2.
 *
 * <p><b>What the original does, in its own order.</b> It is a 47-line
 * {@code RenderLiving} subclass and does exactly four things:</p>
 * <ol>
 *   <li>one texture — {@code skin} = {@code thaumcraft:textures/models/eldritch_golem.png}
 *       ({@code :18}), returned by {@code func_110775_a} ({@code :25-27});</li>
 *   <li>a shadow radius handed in by the registration site —
 *       {@code new RenderEldritchGolem(new ModelEldritchGolem(), 0.5F)}
 *       ({@code ClientProxy.java:457}) — so 0.5 is the shipped figure, not a
 *       figure this class picks;</li>
 *   <li>{@code func_77041_b} = {@code preRenderCallback} ({@code :29-32}): the
 *       boss-bar feed {@code BossStatus.setBossStatus(golem, false)} and
 *       {@code GL11.glScalef(2.15F, 2.15F, 2.15F)};</li>
 *   <li>{@code doRenderLiving} ({@code :34-42}): turn on
 *       {@code GL_BLEND} with {@code glBlendFunc(GL_SRC_ALPHA,
 *       GL_ONE_MINUS_SRC_ALPHA)} and {@code glAlphaFunc(GL_GREATER, 1/255)},
 *       render the model at {@code par4 - golem.field_70129_M}, then turn
 *       blending off and restore {@code glAlphaFunc(516, 0.1F)}.</li>
 * </ol>
 *
 * <p><b>1.21.1 mapping, and the three deltas.</b> 1.7.10's {@code RenderLiving}
 * is 1.21.1's {@link LivingEntityRenderer} one-for-one, and this renderer draws
 * no extra geometry — the model is all it emits. The GL state of the
 * {@code doRenderLiving} wrapper is per-draw state in 1.21.1, so it becomes the
 * renderer's {@link #getRenderType}: {@link RenderType#entityTranslucent} is the
 * platform's "blend the alpha, discard the near-transparent texels" entity type,
 * and {@link #scale} is the twin of {@code preRenderCallback} (both are the
 * hook the platform calls on the model's pose before it draws). Named deltas,
 * recorded as BEHAVIORAL_DEVIATIONS BD-25:</p>
 * <ul>
 *   <li>{@code BossStatus.setBossStatus(golem, false)} at {@code :30} is the
 *       1.7.10 boss-health-bar feed, done from inside the renderer. 1.21.1 keeps
 *       the boss bar on the entity side (a {@code ServerBossEvent} published by
 *       the entity, rendered by the GUI), so the renderer ports no equivalent
 *       call; wiring that event is the entity behaviour card t_03c5d088.</li>
 *   <li>{@code glAlphaFunc(GL_GREATER, 1/255)} ({@code :36}) has no 1.21.1
 *       equivalent: the {@code position_tex} shader discards texels under
 *       {@code a < 0.1}, which is the restored figure of the source's own tail
 *       ({@code :41}). The softest texel edge of the sheet is clipped, and only
 *       a real client can show by how much.</li>
 *   <li>the y passed to the model is {@code par4 - golem.field_70129_M}
 *       ({@code :38}, MCP {@code renderYOffset}). 1.21.1 has no
 *       {@code renderYOffset}: its dispatcher hands the renderer a pose already
 *       sitting at the entity. Provable that nothing is lost for THIS entity:
 *       the reference tree never writes that field
 *       ({@code grep -rn field_70129_M reference/decompiled/thaumcraft} is
 *       empty), so the term the source subtracts is always 0.</li>
 * </ul>
 *
 * <p><b>Why it is a plain living renderer here.</b> The reference's class
 * extends {@code RenderLiving} directly ({@code :16}) — it is <em>not</em> a
 * {@code RenderGolemBase} subclass, so it never inherits that class's core-icon,
 * upgrade-strip, decoration, damage or carried-item passes. That matches the
 * entity: {@code EntityEldritchGolem extends EntityThaumcraftBoss}
 * ({@code common/entities/monster/boss/EntityEldritchGolem.java:35}) which
 * extends {@code EntityMob} ({@code same dir/EntityThaumcraftBoss.java:25}), so
 * it is no {@code EntityGolemBase} at all — and the port registers it the same
 * way, as a Monster (parent card t_d0a98369; its behaviour is t_03c5d088).
 * {@code LivingEntityRenderer<EntityEldritchGolem, ModelEldritchGolem>} is
 * therefore the faithful shape, with no deviation to record for the inheritance.
 * The boss-bar row of BD-25 is the only renderer-side delta.</p>
 *
 * <p>Textures are referenced by path only (D-004): {@code eldritch_golem.png}
 * is All Rights Reserved and stays in the local-build overlay.</p>
 */
public class RenderEldritchGolem extends LivingEntityRenderer<EntityEldritchGolem, ModelEldritchGolem> {

    /** {@code RenderEldritchGolem.java:18} — the {@code skin} field, {@code textures/models/eldritch_golem.png}. */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/models/eldritch_golem.png");

    /**
     * The shadow radius the registration site hands the constructor —
     * {@code new RenderEldritchGolem(new ModelEldritchGolem(), 0.5F)} at
     * {@code ClientProxy.java:457}.
     */
    public static final float SHADOW_RADIUS = 0.5F;

    /** {@code RenderEldritchGolem.java:31} — {@code glScalef(2.15F, 2.15F, 2.15F)}. */
    public static final float PRE_RENDER_SCALE = 2.15F;

    public RenderEldritchGolem(final EntityRendererProvider.Context context) {
        // ClientProxy.java:457 — the model and the 0.5F shadow radius.
        super(context, new ModelEldritchGolem(), SHADOW_RADIUS);
    }

    /** {@code func_110775_a} ({@code :25-27}) — the {@code skin} field, the entity is ignored. */
    @Override
    public ResourceLocation getTextureLocation(final EntityEldritchGolem golem) {
        return TEXTURE;
    }

    /**
     * {@code func_77041_b} / {@code preRenderCallback} ({@code :29-32}): the
     * 2.15 scale, on the pose the platform then draws the model with. The
     * boss-bar feed of {@code :30} is the entity side in 1.21.1 (BD-25).
     */
    @Override
    protected void scale(final EntityEldritchGolem golem, final PoseStack pose, final float partialTick) {
        pose.scale(PRE_RENDER_SCALE, PRE_RENDER_SCALE, PRE_RENDER_SCALE); // :31
    }

    /**
     * The GL state of {@code doRenderLiving} ({@code :35-37}):
     * {@code glEnable(GL_BLEND) + glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)}
     * and {@code glAlphaFunc(GL_GREATER, 1/255)} become the entity translucent
     * render type, which is 1.21.1's blended + alpha-tested entity draw. The
     * source restores the alpha test to 0.1 on the way out ({@code :41}), which
     * is the threshold this render type uses (BD-25).
     */
    @Override
    protected RenderType getRenderType(final EntityEldritchGolem golem, final boolean showPurpleShadows,
            final boolean hasGlint, final boolean isSleeping) {
        return RenderType.entityTranslucent(TEXTURE);
    }

    /**
     * {@code render} ({@code :34-42}) is the platform's own chain: the
     * {@link #scale} hook, then the model through {@link #getRenderType}. No
     * extra pass is emitted, because the reference emits none — see the class
     * javadoc for the four things the original does.
     */
    @Override
    public void render(final EntityEldritchGolem golem, final float entityYaw, final float partialTick,
            final PoseStack pose, final MultiBufferSource buffers, final int packedLight) {
        super.render(golem, entityYaw, partialTick, pose, buffers, packedLight);
    }
}
