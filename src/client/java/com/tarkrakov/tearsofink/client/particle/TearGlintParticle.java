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

/** Холодная искорка Слёзницы: светится сама (не зависит от освещения), медленно поднимается и гаснет. */
public class TearGlintParticle extends TextureSheetParticle {
	protected TearGlintParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.pickSprite(sprites);
		this.quadSize = Mth.nextFloat(this.random, 0.05f, 0.1f);
		this.lifetime = Mth.nextInt(this.random, 40, 90);
		this.gravity = 0f;
		this.hasPhysics = false;
		this.xd = (this.random.nextDouble() - 0.5) * 0.01;
		this.yd = 0.008 + this.random.nextDouble() * 0.01;
		this.zd = (this.random.nextDouble() - 0.5) * 0.01;
		this.alpha = 0f;
	}

	@Override
	public void tick() {
		super.tick();
		float t = (float) this.age / this.lifetime;
		float fade = t < 0.2f ? t / 0.2f : (1f - t) / 0.8f;
		// лёгкое мерцание
		this.alpha = Mth.clamp(fade, 0f, 1f) * (0.75f + 0.25f * Mth.sin(this.age * 0.6f));
	}

	@Override
	protected int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
									   double xd, double yd, double zd) {
			return new TearGlintParticle(level, x, y, z, this.sprites);
		}
	}
}
