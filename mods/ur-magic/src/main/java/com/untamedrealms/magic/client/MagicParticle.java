package com.untamedrealms.magic.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * One class for every spell particle; the element's {@link Motion} decides how it moves. Rendered
 * full-bright so spells glow at night and in caves, animated through its frames over its life.
 */
public class MagicParticle extends TextureSheetParticle {
    public enum Motion {
        /** Electric: very short, jittering. */
        SPARK(3, 6, 0.14f, 0f, 0.6f, true),
        /** Fire: rises, shrinks. */
        EMBER(10, 20, 0.16f, -0.012f, 0.92f, false),
        /** Frost: drifts down slowly, spins. */
        FROST(18, 32, 0.13f, 0.006f, 0.95f, false),
        /** Holy light: floats up, twinkles. */
        HOLY(14, 24, 0.15f, -0.006f, 0.9f, false),
        /** Healing: drifts up gently. */
        HEAL(16, 26, 0.12f, -0.01f, 0.92f, false),
        /** Shadow: lingers and spreads. */
        SHADOW(16, 28, 0.2f, -0.002f, 0.88f, false),
        /** Arcane: hangs in the air. */
        ARCANE(12, 20, 0.14f, 0f, 0.86f, false);

        final int minLife, maxLife;
        final float size, gravity, friction;
        final boolean jitter;

        Motion(int minLife, int maxLife, float size, float gravity, float friction, boolean jitter) {
            this.minLife = minLife;
            this.maxLife = maxLife;
            this.size = size;
            this.gravity = gravity;
            this.friction = friction;
            this.jitter = jitter;
        }
    }

    private final SpriteSet sprites;
    private final Motion motion;
    private final float baseSize;
    private final float spin;

    protected MagicParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, Motion motion) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.motion = motion;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = motion.minLife + random.nextInt(motion.maxLife - motion.minLife + 1);
        this.gravity = motion.gravity;
        this.friction = motion.friction;
        this.hasPhysics = false;
        this.baseSize = motion.size * (0.75f + random.nextFloat() * 0.5f);
        this.quadSize = baseSize;
        this.spin = motion == Motion.FROST ? (random.nextFloat() - 0.5f) * 0.3f : 0f;
        this.roll = random.nextFloat() * Mth.TWO_PI * (motion == Motion.FROST ? 1 : 0);
        this.oRoll = roll;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float life = (float) age / lifetime;
        oRoll = roll;
        roll += spin;
        if (motion.jitter) {
            xd += (random.nextDouble() - 0.5) * 0.08;
            yd += (random.nextDouble() - 0.5) * 0.08;
            zd += (random.nextDouble() - 0.5) * 0.08;
        }
        quadSize = switch (motion) {
            case SHADOW -> baseSize * (1f + life);
            case HOLY -> baseSize * (0.7f + 0.3f * Mth.sin(age * 1.3f));
            default -> baseSize * (1f - life * 0.5f);
        };
        alpha = life > 0.7f ? 1f - (life - 0.7f) / 0.3f : 1f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return motion == Motion.SHADOW ? super.getLightColor(partialTick) : 0xF000F0;
    }

    public record Provider(SpriteSet sprites, Motion motion) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new MagicParticle(level, x, y, z, vx, vy, vz, sprites, motion);
        }
    }
}
