package com.tarkrakov.tearsofink.client.particle;

import com.tarkrakov.tearsofink.client.compat.ShaderCompat;
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
 * Клуб тумана: большой, полупрозрачный, медленно дрейфует, плавно появляется и тает.
 * С шейдерами — крупнее, плотнее и не принимает тени/освещение (яркость считаем сами по времени суток).
 */
public class FogBankParticle extends TextureSheetParticle {
	// ===== Настройки: без шейдеров =====
	public static final float MAX_ALPHA = 0.35f;
	public static final float MIN_SIZE = 2.5f, MAX_SIZE = 5.0f;
	// ===== Настройки: с шейдерами =====
	public static final float SHADER_MAX_ALPHA = 0.6f;
	public static final float SHADER_MIN_SIZE = 4.0f, SHADER_MAX_SIZE = 8.0f;
	/** Яркость клубов ночью в шейдерном режиме (днём = 1). */
	public static final float SHADER_NIGHT_BRIGHTNESS = 0.18f;

	public static final int MIN_LIFETIME = 200, MAX_LIFETIME = 360;

	private final boolean shaderMode;
	private final float maxAlpha;

	protected FogBankParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.shaderMode = ShaderCompat.shadersActive();
		this.maxAlpha = shaderMode ? SHADER_MAX_ALPHA : MAX_ALPHA;
		this.pickSprite(sprites);
		this.quadSize = shaderMode
				? Mth.nextFloat(this.random, SHADER_MIN_SIZE, SHADER_MAX_SIZE)
				: Mth.nextFloat(this.random, MIN_SIZE, MAX_SIZE);
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
		float t = (float) this.age / this.lifetime;
		float fade = t < 0.2f ? t / 0.2f : (t > 0.7f ? (1f - t) / 0.3f : 1f);
		this.alpha = this.maxAlpha * Mth.clamp(fade, 0f, 1f);

		if (shaderMode) {
			// Освещение отключено, поэтому день/ночь задаём цветом сами.
			float daylight = Mth.clamp((this.level.getSkyDarken(1f) - 0.2f) / 0.8f, 0f, 1f);
			float b = Mth.lerp(daylight, SHADER_NIGHT_BRIGHTNESS, 1f);
			this.setColor(b, b, b);
		}
	}

	@Override
	protected int getLightColor(float partialTick) {
		// С шейдерами — «полный свет», чтобы шейдер не клал на туман тени и грязь.
		return shaderMode ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
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
