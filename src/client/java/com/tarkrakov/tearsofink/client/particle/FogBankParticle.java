package com.tarkrakov.tearsofink.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tarkrakov.tearsofink.client.fog.InkFog;
import com.tarkrakov.tearsofink.client.fog.LanternClarity;
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
	/** Непрозрачность клуба, когда игрок СНАРУЖИ (издалека туман выглядит плотной стеной). */
	public static final float ALPHA_OUTSIDE = 0.5f;
	/** Непрозрачность клуба, когда игрок ВНУТРИ (вокруг себя туман прозрачнее). */
	public static final float ALPHA_INSIDE = 0.25f;
	/** Непрозрачность клубов стены тумана на краю Земель. */
	public static final float WALL_ALPHA = 0.8f;
	/** Размер клуба (половина стороны, в блоках). */
	public static final float MIN_SIZE = 2.5f, MAX_SIZE = 5.0f;
	/** Время жизни в тиках (20 тиков = 1 секунда). */
	/** Ближе этого расстояния (в блоках) клуб начинает растворяться. */
	public static final float NEAR_FADE_START = 4.5f;
	/** На этом расстоянии клуб полностью невидим. */
	public static final float NEAR_FADE_END = 1.0f;
	public static final int MIN_LIFETIME = 200, MAX_LIFETIME = 360;
	/** Как быстро клуб тает, попав в круг фонаря (доля за тик). */
	public static final float LANTERN_DISSOLVE_PER_TICK = 0.04f;

	/** 1 — клуб целый, 0 — растаял у фонаря. */
	private float lanternFade = 1f;
	private boolean nearLantern = false;
	/** Клуб стены тумана на краю: плотнее, ближе не растворяется так сильно. */
	private boolean wall;

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
		if (this.age % 5 == 0) nearLantern = LanternClarity.inBubble(this.x, this.y, this.z, 1);
		if (nearLantern) {
			lanternFade -= LANTERN_DISSOLVE_PER_TICK;
			if (lanternFade <= 0f) this.remove();
		}
		this.alpha = Mth.clamp(fade, 0f, 1f) * Math.max(0f, lanternFade); // итоговая прозрачность — в render()
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
		float zoneAlpha = wall ? WALL_ALPHA : Mth.lerp(InkFog.strength(), ALPHA_OUTSIDE, ALPHA_INSIDE);
		this.alpha = baseAlpha * zoneAlpha * near * near; // мягкое, а не линейное растворение
		super.render(buffer, camera, partialTick);
		this.alpha = baseAlpha;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	/** Клубы стены тумана на краю Земель. */
	public static class WallProvider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public WallProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
									   double xd, double yd, double zd) {
			FogBankParticle p = new FogBankParticle(level, x, y, z, sprites);
			p.wall = true;
			p.quadSize *= 1.4f;
			p.lifetime = Mth.nextInt(p.random, 100, 160);
			p.setColor(0.85f, 0.85f, 0.9f);
			return p;
		}
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
									   double xd, double yd, double zd) {
			FogBankParticle particle = new FogBankParticle(level, x, y, z, this.sprites);
			// xd/yd/zd здесь — не скорость, а оттенок клуба (см. FogBankSpawner)
			if (xd > 0 || yd > 0 || zd > 0) particle.setColor((float) xd, (float) yd, (float) zd);
			return particle;
		}
	}
}
