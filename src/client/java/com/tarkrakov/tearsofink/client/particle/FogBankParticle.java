package com.tarkrakov.tearsofink.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Клуб тумана: большой, полупрозрачный, медленно дрейфует, плавно появляется и тает. */
public class FogBankParticle extends TextureSheetParticle {
	// ===== Настройки =====
	/** Максимальная непрозрачность клуба (0..1). */
	public static final float MAX_ALPHA = 0.25f;
	/** Размер клуба (половина стороны, в блоках). */
	public static final float MIN_SIZE = 2.5f, MAX_SIZE = 5.0f;
	/** Время жизни в тиках (20 тиков = 1 секунда). */
	public static final int MIN_LIFETIME = 200, MAX_LIFETIME = 360;

	protected FogBankParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.pickSprite(sprites);
		this.quadSize = Mth.nextFloat(this.random, MIN_SIZE, MAX_SIZE);
		this.lifetime = Mth.nextInt(this.random, MIN_LIFETIME, MAX_LIFETIME);
		this.gravity = 0f;
		this.hasPhysics = false;
		this.xd = (this.random.nextDouble() - 0.5) * 0.02;
		this.yd = 0.0;
		this.zd = (this.random.nextDouble() - 0.5) * 0.02;
		this.roll = this.oRoll = this.random.nextFloat() * Mth.TWO_PI;
		this.alpha = 0f;
	}

	@Override
	public void tick() {
		super.tick();
		this.xd *= 1.0; // без затухания скорости — плавный дрейф
		this.zd *= 1.0;
		float t = (float) this.age / this.lifetime;
		float fade = t < 0.2f ? t / 0.2f : (t > 0.7f ? (1f - t) / 0.3f : 1f);
		this.alpha = MAX_ALPHA * Mth.clamp(fade, 0f, 1f);
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
			return new FogBankParticle(level, x, y, z, this.sprites);
		}
	}
}
