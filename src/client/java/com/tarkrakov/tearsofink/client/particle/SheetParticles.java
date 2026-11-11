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

/** Частицы из анимированных листов автора (кадры листаются за время жизни). */
public final class SheetParticles {
	private SheetParticles() {}

	// ======================================================================
	/** Дым: клубок, чуть поднимается, расползается и тает. Рывок, гашение фонаря. */
	public static class Smoke extends TextureSheetParticle {
		private final SpriteSet sprites;

		Smoke(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
			super(level, x, y, z, xd, yd, zd);
			this.sprites = sprites;
			this.xd = xd + (random.nextDouble() - 0.5) * 0.02;
			this.yd = yd + 0.015 + random.nextDouble() * 0.01;
			this.zd = zd + (random.nextDouble() - 0.5) * 0.02;
			this.lifetime = 14 + random.nextInt(8);
			this.quadSize = 0.22f + random.nextFloat() * 0.12f;
			this.friction = 0.9f;
			this.gravity = 0f;
			this.hasPhysics = false;
			setSpriteFromAge(sprites);
		}

		@Override
		public void tick() {
			super.tick();
			if (!removed) setSpriteFromAge(sprites);
			this.alpha = 1f - Math.max(0f, (age - lifetime * 0.6f) / (lifetime * 0.4f));
		}

		@Override
		public ParticleRenderType getRenderType() {
			return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
		}
	}

	// ======================================================================
	/**
	 * Искорка (мелкие искорки автора, белые — красятся здесь): разлетается, чуть падает, тает.
	 * ember — красно-оранжевая, светится сама; ink — чёрная.
	 */
	public static class Spark extends TextureSheetParticle {
		private final SpriteSet sprites;
		private final boolean glowing;

		Spark(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean ember) {
			super(level, x, y, z, xd, yd, zd);
			this.sprites = sprites;
			this.glowing = ember;
			this.xd = xd + (random.nextDouble() - 0.5) * 0.02;
			this.yd = yd + (random.nextDouble() - 0.5) * 0.02;
			this.zd = zd + (random.nextDouble() - 0.5) * 0.02;
			this.lifetime = 14 + random.nextInt(10);
			this.quadSize = 0.06f + random.nextFloat() * 0.05f;
			this.gravity = ember ? 0.04f : 0.08f;
			this.friction = 0.9f;
			this.hasPhysics = true;
			if (ember) {
				// от красного к оранжевому
				float g = 0.2f + random.nextFloat() * 0.35f;
				setColor(1f, g, 0.05f);
			} else {
				float v = 0.04f + random.nextFloat() * 0.08f;
				setColor(v, v * 0.8f, v * 1.2f);
			}
			setSpriteFromAge(sprites);
		}

		@Override
		public void tick() {
			super.tick();
			if (!removed) setSpriteFromAge(sprites);
			this.alpha = 1f - Math.max(0f, (age - lifetime * 0.6f) / (lifetime * 0.4f));
		}

		@Override
		protected int getLightColor(float partialTick) {
			return glowing ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
		}

		@Override
		public ParticleRenderType getRenderType() {
			return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
		}
	}

	// ======================================================================
	/** Клочок пергамента: разлетается, кувыркается (3 кадра по кругу), медленно оседает и тает. */
	public static class Paper extends TextureSheetParticle {
		private final SpriteSet sprites;
		private final float spin;

		Paper(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
			super(level, x, y, z, xd, yd, zd);
			this.sprites = sprites;
			this.xd = xd;
			this.yd = yd;
			this.zd = zd;
			this.lifetime = 40 + random.nextInt(25);
			this.quadSize = 0.12f + random.nextFloat() * 0.06f;
			this.gravity = 0.03f;
			this.friction = 0.92f;
			this.spin = random.nextFloat() * 10f;
			this.roll = random.nextFloat() * Mth.TWO_PI;
			this.setSprite(sprites.get(0, 2));
		}

		@Override
		public void tick() {
			super.tick();
			if (removed) return;
			// кувыркается: кадры по кругу + лёгкое покачивание
			int frame = ((age + (int) spin) / 4) % 3;
			setSprite(sprites.get(frame, 2));
			this.oRoll = this.roll;
			this.roll += 0.08f * (spin > 5 ? 1 : -1);
			this.xd += Mth.sin((age + spin) * 0.3f) * 0.004;
			this.alpha = 1f - Math.max(0f, (age - lifetime * 0.7f) / (lifetime * 0.3f));
		}

		@Override
		public ParticleRenderType getRenderType() {
			return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
		}
	}

	// ======================================================================
	public record SmokeProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double xd, double yd, double zd) {
			return new Smoke(l, x, y, z, xd, yd, zd, sprites);
		}
	}

	public record EmberProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double xd, double yd, double zd) {
			return new Spark(l, x, y, z, xd, yd, zd, sprites, true);
		}
	}

	public record InkSparkProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double xd, double yd, double zd) {
			return new Spark(l, x, y, z, xd, yd, zd, sprites, false);
		}
	}

	public record PaperProvider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double xd, double yd, double zd) {
			return new Paper(l, x, y, z, xd, yd, zd, sprites);
		}
	}
}
