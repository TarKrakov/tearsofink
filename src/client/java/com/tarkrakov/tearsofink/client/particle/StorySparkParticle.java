package com.tarkrakov.tearsofink.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Personal navigation trail: hovering, slowly fading sparks. Independent art for both vows. */
public final class StorySparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private StorySparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                               SpriteSet sprites, boolean soul) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = Mth.clamp(xd, -0.025, 0.025);
        this.yd = Mth.clamp(yd, -0.015, 0.015) + 0.004;
        this.zd = Mth.clamp(zd, -0.025, 0.025);
        this.lifetime = 48 + random.nextInt(17);
        this.quadSize = 0.075f + random.nextFloat() * 0.05f;
        this.gravity = 0;
        this.friction = 0.94f;
        this.hasPhysics = false;
        if (soul) {
            setColor(1f, 0.75f + random.nextFloat() * 0.2f, 0.15f + random.nextFloat() * 0.15f);
        } else {
            float v = 0.14f + random.nextFloat() * 0.12f;
            setColor(v, v * 0.65f, v * 1.4f);
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        // Keep the bearing visible for most of its life, then fade smoothly.
        float progress = (float) age / lifetime;
        this.alpha = 1f - Mth.clamp((progress - 0.72f) / 0.28f, 0f, 1f);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, boolean soul) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new StorySparkParticle(level, x, y, z, xd, yd, zd, sprites, soul);
        }
    }
}
