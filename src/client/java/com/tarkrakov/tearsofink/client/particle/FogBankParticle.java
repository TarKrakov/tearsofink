package com.tarkrakov.tearsofink.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Клуб тумана: большой, полупрозрачный, медленно дрейфует, плавно появляется и тает. */
public class FogBankParticle extends TextureSheetParticle {
	// ===== Настройки =====
	/** Максимальная непрозрачность клуба (0..1). */
	public static final float MAX_ALPHA = 0.25f;
	/** Размер клуба (половина стороны, в блоках). */
	public static final float MIN_SIZE = 2.5f, MAX_SIZE = 5.0f;
	/** Время жизни в тиках (20 тиков = 1 секунда). */
	/** Ближе этого расстояния (в блоках) клуб начинает растворяться. */
	public static final float NEAR_FADE_START = 6.0f;
	/** На этом расстоянии клуб полностью невидим. */
	public static final float NEAR_FADE_END = 1.5f;
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
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		// Чем ближе клуб к камере, тем он прозрачнее — чтобы не упираться в туман лицом.
		Vec3 cam = camera.getPosition();
		double dx = Mth.lerp(partialTick, this.xo, this.x) - cam.x;
		double dy = Mth.lerp(partialTick, this.yo, this.y) - cam.y;
		double dz = Mth.lerp(partialTick, this.zo, this.z) - cam.z;
		float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float near = Mth.clamp((distance - NEAR_FADE_END) / (NEAR_FADE_START - NEAR_FADE_END), 0f, 1f);
		if (near <= 0f) return;

		float baseAlpha = this.alpha;
		this.alpha = baseAlpha * near * near; // мягкое, а не линейное растворение
		super.render(buffer, camera, partialTick);
		this.alpha = baseAlpha;
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
