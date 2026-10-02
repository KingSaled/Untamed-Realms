package com.untamedrealms.magic.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * How spells look and sound. Server side: particles go out with {@code sendParticles}, so everyone
 * nearby sees the same thing. Every element pairs our own glowing particle with one vanilla accent
 * (smoke for fire, snowflakes for frost, end-rod glints for holy light...).
 */
public final class SpellFx {
    private SpellFx() {}

    /** Roughly where the casting hand is: in front of the eyes, a little right and down. */
    public static Vec3 hand(ServerPlayer caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 1e-4) right = new Vec3(1, 0, 0);
        return caster.getEyePosition().add(look.scale(0.6)).add(right.scale(0.35)).add(0, -0.3, 0);
    }

    private static ParticleOptions accent(String element) {
        return switch (element) {
            case "fire" -> ParticleTypes.SMOKE;
            case "frost" -> ParticleTypes.SNOWFLAKE;
            case "shock" -> ParticleTypes.ELECTRIC_SPARK;
            case "holy" -> ParticleTypes.END_ROD;
            case "heal" -> ParticleTypes.HAPPY_VILLAGER;
            case "shadow" -> ParticleTypes.SQUID_INK;
            default -> ParticleTypes.ENCHANT;
        };
    }

    /** One particle with a velocity (sendParticles with count 0 treats the offsets as a direction). */
    private static void moving(ServerLevel level, ParticleOptions p, Vec3 at, Vec3 velocity) {
        level.sendParticles(p, at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
    }

    // ------------------------------------------------------------------ casting

    /** The flash at the hand and the cast sound, for every spell. */
    public static void cast(ServerPlayer caster, String element, String kind) {
        ServerLevel level = caster.serverLevel();
        Vec3 h = hand(caster);
        level.sendParticles(MagicFx.particle(element), h.x, h.y, h.z, 10, 0.08, 0.08, 0.08, 0.02);
        level.playSound(null, caster.getX(), caster.getY() + 1, caster.getZ(), MagicFx.castSound(element, kind), SoundSource.PLAYERS,
                1.0f, 0.9f + caster.getRandom().nextFloat() * 0.2f);
    }

    /** A dense stream along the look direction (Flames, Sparks): particles fly outwards, spreading. */
    public static void cone(ServerPlayer caster, String element, double range) {
        ServerLevel level = caster.serverLevel();
        RandomSource r = caster.getRandom();
        Vec3 h = hand(caster);
        Vec3 look = caster.getLookAngle();
        ParticleOptions main = MagicFx.particle(element);
        for (int i = 0; i < 40; i++) {
            Vec3 dir = look.add((r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35, (r.nextDouble() - 0.5) * 0.35).normalize();
            double speed = range / 14.0 * (0.6 + r.nextDouble() * 0.6);
            moving(level, main, h, dir.scale(speed));
            if (i % 4 == 0) moving(level, accent(element), h, dir.scale(speed * 0.8));
        }
    }

    /**
     * An electric arc from {@code from} to {@code to}: a jagged line of sparks that zig-zags
     * around the straight path, with branches.
     */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to, RandomSource r) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        int nodes = Math.max(3, (int) (len * 2.5));
        Vec3 side1 = d.cross(new Vec3(0, 1, 0)).normalize();
        if (side1.lengthSqr() < 1e-4) side1 = new Vec3(1, 0, 0);
        Vec3 side2 = d.cross(side1).normalize();
        ParticleOptions spark = MagicFx.particle("shock");
        Vec3 prev = from;
        for (int i = 1; i <= nodes; i++) {
            double u = (double) i / nodes;
            double amp = i == nodes ? 0 : 0.35 * Math.sin(Math.PI * u) + 0.1;
            Vec3 node = from.add(d.scale(u)).add(side1.scale((r.nextDouble() - 0.5) * 2 * amp)).add(side2.scale((r.nextDouble() - 0.5) * 2 * amp));
            // fill the segment so the bolt reads as one line
            for (int k = 0; k < 4; k++) {
                Vec3 p = prev.lerp(node, k / 4.0);
                level.sendParticles(spark, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            if (r.nextFloat() < 0.18 && i < nodes) {
                Vec3 branchEnd = node.add(side1.scale((r.nextDouble() - 0.5) * 1.6)).add(side2.scale((r.nextDouble() - 0.5) * 1.6)).add(d.normalize().scale(0.6));
                for (int k = 1; k <= 4; k++) {
                    Vec3 p = node.lerp(branchEnd, k / 4.0);
                    level.sendParticles(spark, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            prev = node;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 8, 0.2, 0.2, 0.2, 0.15);
    }

    /** Burst where a spell lands: a puff of the element's particle, its accent, and the impact sound. */
    public static void impact(ServerLevel level, Vec3 at, String element, RandomSource r) {
        ParticleOptions main = MagicFx.particle(element);
        for (int i = 0; i < 24; i++) {
            Vec3 dir = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6 + 0.2, r.nextGaussian()).normalize();
            moving(level, main, at, dir.scale(0.12 + r.nextDouble() * 0.12));
        }
        level.sendParticles(accent(element), at.x, at.y, at.z, 8, 0.25, 0.25, 0.25, 0.04);
        if ("fire".equals(element)) level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0);
        level.playSound(null, at.x, at.y, at.z, MagicFx.impactSound(element), SoundSource.PLAYERS, 1.0f, 0.9f + r.nextFloat() * 0.2f);
    }

    /** Self spells: two strands spiralling up around the caster. */
    public static void spiral(ServerPlayer caster, String element) {
        ServerLevel level = caster.serverLevel();
        ParticleOptions main = MagicFx.particle(element);
        for (int i = 0; i < 36; i++) {
            double u = i / 36.0;
            for (int strand = 0; strand < 2; strand++) {
                double a = u * Mth.TWO_PI * 2 + strand * Math.PI;
                double x = caster.getX() + Math.cos(a) * 0.8, z = caster.getZ() + Math.sin(a) * 0.8, y = caster.getY() + u * 2.2;
                level.sendParticles(main, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(accent(element), caster.getX(), caster.getY() + 1, caster.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
    }

    /** Area spells: a ring that races outwards along the ground. */
    public static void ring(ServerPlayer caster, String element, float radius) {
        ServerLevel level = caster.serverLevel();
        ParticleOptions main = MagicFx.particle(element);
        Vec3 c = caster.position().add(0, 0.2, 0);
        int n = 64;
        for (int i = 0; i < n; i++) {
            double a = i * Mth.TWO_PI / n;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            moving(level, main, c, dir.scale(radius / 12.0));
            if (i % 4 == 0) moving(level, accent(element), c.add(0, 0.4, 0), dir.scale(radius / 16.0));
        }
    }

    /** Summons and bound weapons: a column of light where the conjured thing appears. */
    public static void pillar(ServerLevel level, Vec3 at, String element) {
        ParticleOptions main = MagicFx.particle(element);
        for (int i = 0; i < 30; i++) {
            double y = at.y + i * 0.08;
            level.sendParticles(main, at.x, y, at.z, 2, 0.25, 0.02, 0.25, 0.01);
        }
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 0.8, at.z, 40, 0.4, 0.6, 0.4, 0.4);
    }

    /** The trail of a flying spell: a dense glowing core plus a few accents. */
    public static void trail(ServerLevel level, Entity bolt, String element, RandomSource r) {
        Vec3 v = bolt.getDeltaMovement();
        ParticleOptions main = MagicFx.particle(element);
        for (int i = 0; i < 4; i++) {
            double t = i / 4.0;
            level.sendParticles(main, bolt.getX() - v.x * t, bolt.getY() + 0.2 - v.y * t, bolt.getZ() - v.z * t, 2, 0.04, 0.04, 0.04, 0.005);
        }
        if (r.nextFloat() < 0.5) level.sendParticles(accent(element), bolt.getX(), bolt.getY() + 0.2, bolt.getZ(), 1, 0.05, 0.05, 0.05, 0.01);
    }
}
