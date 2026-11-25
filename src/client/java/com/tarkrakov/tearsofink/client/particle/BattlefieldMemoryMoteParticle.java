package com.tarkrakov.tearsofink.client.particle;

import com.tarkrakov.tearsofink.client.battlefield.BattlefieldMemoryMotes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Small white memories rising out of the old battlefield soil. Reuses the two
 * already-authored Tear Glint images; never a new artwork placeholder.
 * Client visuals only, no entity, server tick, or actual light-level changes.
 */
public final class BattlefieldMemoryMoteParticle extends TextureSheetParticle {
    private final float phase;
    private final float brightness;

    private BattlefieldMemoryMoteParticle(ClientLevel level, double x, double y, double z,
                                         double xSpeed, double ySpeed, double zSpeed,
                                         SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites); // two author sprites for varied silhouettes
        phase = random.nextFloat() * Mth.TWO_PI;
        quadSize = Mth.nextFloat(random, .080f, .19f);
        lifetime = Mth.nextInt(random, 150, 230);
        brightness = Mth.nextFloat(random, .58f, .9f);
        // Not a shower from overhead: an immediate upward departure from soil,
        // then a faint sideways wobble, breaking up before reaching the sky.
        xd = xSpeed;
        yd = Math.max(.040d, ySpeed);
        zd = zSpeed;
        gravity = 0f;
        hasPhysics = false;
        friction = .995f;
        float white = Mth.nextFloat(random, .87f, 1f);
        setColor(white, white, Math.min(1f, white + .035f));
        alpha = 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float ageRatio = (float) age / lifetime;
        xd += Math.sin(phase + age * .10f) * .00016d;
        zd += Math.cos(phase + age * .083f) * .00016d;
        yd = Math.min(.095d, yd + .00011d);
        // A soft entrance, short suspension and gradual dissolution, plus
        // field-edge attenuation if the particle drifts across the boundary.
        float fade = Math.min(1f, ageRatio / .12f)
                * Mth.clamp((1f - ageRatio) / .48f, 0f, 1f);
        float field = BattlefieldMemoryMotes.opennessAt(level, x, z);
        alpha = brightness * fade * field;
        if (field <= .001f) remove();
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new BattlefieldMemoryMoteParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
