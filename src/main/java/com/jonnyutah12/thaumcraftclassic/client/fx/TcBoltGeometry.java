package com.jonnyutah12.thaumcraftclassic.client.fx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Port of the reference tree's {@code FXLightningBoltCommon}
 * (reference/decompiled/thaumcraft/thaumcraft/client/fx/bolt/FXLightningBoltCommon.java),
 * the fractal bolt geometry behind {@code FXLightningBolt} (TC-M7-02-b-s).
 *
 * <p>The class is pure Java on purpose: the original built its segments with a
 * plain {@link java.util.Random} seeded from the client world's
 * {@code rand.nextLong()} (ClientProxy.nodeBolt, ClientProxy.java:789-796),
 * so a bolt is fully reproducible from its seed. The port keeps that random
 * (not 1.21.1's {@code RandomSource}: the draw sequences differ) and every
 * draw in the original's order — the base constructor's
 * {@code nextInt(3)}, the duration constructor's {@code nextInt(duration)},
 * the fractal's perpendicular/rotate/offset draws, and the split-chance check
 * that Java's short-circuit skips for the first sub-segment. The
 * seed-parity tests pin the resulting segment counts and endpoints.</p>
 *
 * <p>The {@code World} parameter of every original constructor is unused by
 * this class (the decompiled body touches no world state), so the port drops
 * it. {@code calculateCollisionAndDiffs} is a misnomer of the original: it
 * prunes each split branch past its last active segment and computes the
 * join diffs; it performs no ray-tracing, so no world access is needed.</p>
 */
public final class TcBoltGeometry {

    /** One fractal point: a base point plus an offset (the original's BoltPoint). */
    public static final class BoltPoint {
        public final TcFxVector3 point;
        public final TcFxVector3 basepoint;
        public final TcFxVector3 offsetvec;

        BoltPoint(final TcFxVector3 basepoint, final TcFxVector3 offsetvec) {
            this.point = basepoint.copy().add(offsetvec);
            this.basepoint = basepoint;
            this.offsetvec = offsetvec;
        }
    }

    /** One bolt segment, chained into the polyline (the original's Segment). */
    public static final class Segment {
        public final BoltPoint startpoint;
        public final BoltPoint endpoint;
        public TcFxVector3 diff;
        public Segment prev;
        public Segment next;
        public TcFxVector3 nextdiff;
        public TcFxVector3 prevdiff;
        public float sinprev;
        public float sinnext;
        public float light;
        public int segmentno;
        public int splitno;

        Segment(final BoltPoint start, final BoltPoint end, final float light,
                final int segmentnumber, final int splitnumber) {
            this.startpoint = start;
            this.endpoint = end;
            this.light = light;
            this.segmentno = segmentnumber;
            this.splitno = splitnumber;
            this.calcDiff();
        }

        Segment(final TcFxVector3 start, final TcFxVector3 end) {
            this(new BoltPoint(start, new TcFxVector3(0.0, 0.0, 0.0)),
                    new BoltPoint(end, new TcFxVector3(0.0, 0.0, 0.0)),
                    1.0F, 0, 0);
        }

        void calcDiff() {
            this.diff = this.endpoint.point.copy().sub(this.startpoint.point);
        }

        void calcEndDiffs() {
            final TcFxVector3 thisdiffnorm = this.diff.copy().normalize();
            if (this.prev != null) {
                final TcFxVector3 prevdiffnorm = this.prev.diff.copy().normalize();
                this.prevdiff = thisdiffnorm.add(prevdiffnorm).normalize();
                this.sinprev = (float) Math.sin(
                        TcFxVector3.anglePreNorm(thisdiffnorm, prevdiffnorm.scale(-1.0F)) / 2.0F);
            } else {
                this.prevdiff = this.diff.copy().normalize();
                this.sinprev = 1.0F;
            }

            if (this.next != null) {
                final TcFxVector3 nextdiffnorm = this.next.diff.copy().normalize();
                this.nextdiff = thisdiffnorm.add(nextdiffnorm).normalize();
                this.sinnext = (float) Math.sin(
                        TcFxVector3.anglePreNorm(thisdiffnorm, nextdiffnorm.scale(-1.0F)) / 2.0F);
            } else {
                this.nextdiff = this.diff.copy().normalize();
                this.sinnext = 1.0F;
            }
        }
    }

    public final List<Segment> segments = new ArrayList<>();
    public final TcFxVector3 start;
    public final TcFxVector3 end;
    private final HashMap<Integer, Integer> splitparents = new HashMap<>();
    public float multiplier;
    public float length;
    public int numsegments0;
    public int increment;
    public int type = 0;
    private int numsplits;
    private boolean finalized;
    private final Random rand;
    public final long seed;
    public int particleAge;
    public int particleMaxAge;

    /** The base constructor's shape (FXLightningBoltCommon.java:38-51). */
    public TcBoltGeometry(final TcFxVector3 start, final TcFxVector3 end, final long seed) {
        this.start = start;
        this.end = end;
        this.seed = seed;
        this.rand = new Random(seed);
        this.numsegments0 = 1;
        this.increment = 1;
        this.length = this.end.copy().sub(this.start).length();
        // The base shape draws here; the duration constructors overwrite the
        // age below with a second draw. Both draws happen on the nodeBolt path.
        this.particleMaxAge = 3 + this.rand.nextInt(3) - 1;
        this.multiplier = 1.0F;
        this.particleAge = -((int) (this.length * 3.0F));
        this.segments.add(new Segment(this.start, this.end));
    }

    /**
     * The sender shape of {@code ClientProxy.nodeBolt} (ClientProxy.java:789-796):
     * {@code new FXLightningBolt(world, x, y, z, x2, y2, z2, seed, 10, 4.0F, 5)}
     * → duration 10, multiplier 4.0, speed (increment) 5.
     */
    public TcBoltGeometry(final TcFxVector3 start, final TcFxVector3 end, final long seed,
            final int duration, final float multi, final int speed) {
        this(start, end, seed);
        this.particleMaxAge = duration + this.rand.nextInt(duration) - duration / 2;
        this.multiplier = multi;
        this.increment = speed;
    }

    /** The original's seven-pass default fractal (FXLightningBoltCommon.java:135-143). */
    public void defaultFractal() {
        this.fractal(2, this.length * this.multiplier / 8.0F, 0.7F, 0.1F, 45.0F);
        this.fractal(2, this.length * this.multiplier / 12.0F, 0.5F, 0.1F, 50.0F);
        this.fractal(2, this.length * this.multiplier / 17.0F, 0.5F, 0.1F, 55.0F);
        this.fractal(2, this.length * this.multiplier / 23.0F, 0.5F, 0.1F, 60.0F);
        this.fractal(2, this.length * this.multiplier / 30.0F, 0.0F, 0.0F, 0.0F);
        this.fractal(2, this.length * this.multiplier / 34.0F, 0.0F, 0.0F, 0.0F);
        this.fractal(2, this.length * this.multiplier / 40.0F, 0.0F, 0.0F, 0.0F);
    }

    public void fractal(final int splits, final float amount, final float splitchance,
            final float splitlength, final float splitangle) {
        if (this.finalized) {
            return;
        }
        final List<Segment> oldsegments = this.segments;
        final List<Segment> news = new ArrayList<>();
        Segment prev = null;

        for (final Segment segment : oldsegments) {
            prev = segment.prev;
            final TcFxVector3 subsegment = segment.diff.copy().scale(1.0F / splits);
            final BoltPoint[] newpoints = new BoltPoint[splits + 1];
            final TcFxVector3 startpoint = segment.startpoint.point;
            newpoints[0] = segment.startpoint;
            newpoints[splits] = segment.endpoint;

            for (int i = 1; i < splits; i++) {
                final TcFxVector3 randoff = TcFxVector3.getPerpendicular(segment.diff)
                        .rotate(this.rand.nextFloat() * 360.0F, segment.diff);
                randoff.scale((this.rand.nextFloat() - 0.5F) * amount);
                final TcFxVector3 basepoint = startpoint.copy().add(subsegment.copy().scale(i));
                newpoints[i] = new BoltPoint(basepoint, randoff);
            }

            for (int i = 0; i < splits; i++) {
                final Segment next = new Segment(newpoints[i], newpoints[i + 1],
                        segment.light, segment.segmentno * splits + i, segment.splitno);
                next.prev = prev;
                if (prev != null) {
                    prev.next = next;
                }

                if (i != 0 && this.rand.nextFloat() < splitchance) {
                    final TcFxVector3 splitrot = TcFxVector3.xCrossProduct(next.diff)
                            .rotate(this.rand.nextFloat() * 360.0F, next.diff);
                    final TcFxVector3 diff = next.diff.copy()
                            .rotate((this.rand.nextFloat() * 0.66F + 0.33F) * splitangle, splitrot)
                            .scale(splitlength);
                    this.numsplits++;
                    this.splitparents.put(this.numsplits, next.splitno);
                    final Segment split = new Segment(newpoints[i],
                            new BoltPoint(newpoints[i + 1].basepoint,
                                    newpoints[i + 1].offsetvec.copy().add(diff)),
                            segment.light / 2.0F, next.segmentno, this.numsplits);
                    split.prev = prev;
                    news.add(split);
                }

                prev = next;
                news.add(next);
            }

            if (segment.next != null) {
                segment.next.prev = prev;
            }
        }

        this.segments.clear();
        this.segments.addAll(news);
        this.numsegments0 *= splits;
    }

    /**
     * The original's prune pass (FXLightningBoltCommon.java:145-176): for every
     * split branch only the chain up to its last active segment survives, and
     * each surviving segment's join diffs are computed. No world ray-trace.
     */
    private void calculateCollisionAndDiffs() {
        final HashMap<Integer, Integer> lastactivesegment = new HashMap<>();
        Collections.sort(this.segments, (o1, o2) -> {
            final int comp = Integer.compare(o1.splitno, o2.splitno);
            return comp == 0 ? Integer.compare(o1.segmentno, o2.segmentno) : comp;
        });
        int lastsplitcalc = 0;
        int lastactiveseg = 0;

        for (final Segment segment : this.segments) {
            if (segment.splitno > lastsplitcalc) {
                lastactivesegment.put(lastsplitcalc, lastactiveseg);
                lastsplitcalc = segment.splitno;
                lastactiveseg = lastactivesegment.get(this.splitparents.get(segment.splitno));
            }
            lastactiveseg = segment.segmentno;
        }
        lastactivesegment.put(lastsplitcalc, lastactiveseg);

        lastsplitcalc = 0;
        lastactiveseg = lastactivesegment.get(0);
        final Iterator<Segment> iterator = this.segments.iterator();
        while (iterator.hasNext()) {
            final Segment segment = iterator.next();
            if (lastsplitcalc != segment.splitno) {
                lastsplitcalc = segment.splitno;
                lastactiveseg = lastactivesegment.get(segment.splitno);
            }
            if (segment.segmentno > lastactiveseg) {
                iterator.remove();
            }
            segment.calcEndDiffs();
        }
    }

    public void finalizeBolt() {
        if (!this.finalized) {
            this.finalized = true;
            this.calculateCollisionAndDiffs();
            // The original renders the brightest segments first
            // (SegmentLightSorter: descending light, stable sort).
            this.segments.sort(Comparator.comparingDouble((Segment s) -> -s.light));
        }
    }

    public void onUpdate() {
        this.particleAge = this.particleAge + this.increment;
        if (this.particleAge > this.particleMaxAge) {
            this.particleAge = this.particleMaxAge;
        }
    }

    /**
     * The progressive reveal length of {@code FXLightningBolt.renderBolt}
     * (FXLightningBolt.java:112): integer-truncated
     * {@code (age + partial + (int)(length*3)) / (int)(length*3) * numsegments0}.
     */
    public int renderLength(final float partialTick) {
        return (int) ((this.particleAge + partialTick + (int) (this.length * 3.0F))
                / (int) (this.length * 3.0F) * this.numsegments0);
    }

    /**
     * The per-segment ribbon width of {@code renderBolt} (FXLightningBolt.java:118):
     * {@code width * (viewDistance / 5 + 1) * (1 + light) * 0.5}.
     */
    public static float segmentWidth(final float baseWidth, final float viewDistance, final float light) {
        return baseWidth * (viewDistance / 5.0F + 1.0F) * (1.0F + light) * 0.5F;
    }

    /**
     * The pass alphas of {@code renderBolt} (FXLightningBolt.java:99-103):
     * pass 0 fades as the bolt ages, pass 1 fades at half the rate. The final
     * vertex alpha multiplies by the segment's own light.
     */
    public static float passAlpha(final int pass, final int particleAge, final int particleMaxAge) {
        final float boltage = particleAge >= 0 ? (float) particleAge / particleMaxAge : 0.0F;
        return pass == 0 ? (1.0F - boltage) * 0.4F : 1.0F - boltage * 0.5F;
    }

    /**
     * The camera-facing expansion vector of {@code renderBolt}
     * (FXLightningBolt.java:96): {@code (sinyaw*-cospitch, -cossinpitch/cosyaw,
     * cosyaw*cospitch)}. The trig arguments reach the original from vanilla
     * {@code RenderGlobal.renderEffects}, which is not in the reference tree;
     * the port derives the same yaw/pitch cos-sin pair from the 1.21.1 camera
     * look vector (docs/BEHAVIORAL_DEVIATIONS.md, BD-16).
     */
    public static TcFxVector3 playerVec(final float sinYaw, final float cosYaw, final float cosPitch) {
        // The original's RenderGlobal arguments expand (y = yawRad, p = pitchRad)
        // to: cosyaw = sin(y), cospitch = -sin(y)cos(p), sinyaw = -cos(y),
        // cossinpitch = cos(y)cos(p) — see BD-16 for the basis derivation.
        final float cosyaw = sinYaw;
        final float cospitch = -sinYaw * cosPitch;
        final float sinyaw = -cosYaw;
        final float cossinpitch = cosYaw * cosPitch;
        return new TcFxVector3(sinyaw * -cospitch, -cossinpitch / cosyaw, cosyaw * cospitch);
    }

    /**
     * The yaw/pitch cos-sin pair of the RenderGlobal basis, read off a
     * normalized camera look vector {@code (lx, ly, lz)}:
     * {@code cosPitch = |(lx, lz)|}, {@code sinYaw = -lx / cosPitch},
     * {@code cosYaw = lz / cosPitch} (the 1.21.1 camera looks along
     * {@code (-sin(y)cos(p), -sin(p), cos(y)cos(p))}).
     */
    public static float lookCosPitch(final float lx, final float ly, final float lz) {
        return (float) Math.sqrt(lx * lx + lz * lz);
    }

    public static float lookSinYaw(final float lx, final float lz, final float cosPitch) {
        return -lx / cosPitch;
    }

    public static float lookCosYaw(final float lz, final float cosPitch) {
        return lz / cosPitch;
    }
}
