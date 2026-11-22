package com.tarkrakov.tearsofink.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tarkrakov.tearsofink.client.fog.InkFog;
import com.tarkrakov.tearsofink.client.fog.BattlefieldClarity;
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
    /** Low, client-only layer using the existing fog-bank sprites. */
    private boolean groundMist;

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

	private void configureGroundMist() {
        groundMist = true;
        quadSize = Mth.nextFloat(random, 1.1f, 2.0f);
        lifetime = Mth.nextInt(random, 105, 170);
        xd = yd = zd = 0;
        setColor(.89f, .91f, .95f);
    }

    @Override
    public void tick() {
		super.tick();
		this.xd *= 1.0; // без затухания скорости — плавный дрейф
		this.zd *= 1.0;
		float t = (float) this.age / this.lifetime;
		float fade = t < 0.2f ? t / 0.2f : (t > 0.7f ? (1f - t) / 0.3f : 1f);
		if (!wall && this.age % 5 == 0) nearLantern = LanternClarity.inBubble(this.x, this.y, this.z, 1); // стену фонари не рассеивают
		if (nearLantern) {
			lanternFade -= LANTERN_DISSOLVE_PER_TICK;
			if (lanternFade <= 0f) this.remove();
		}
		this.alpha = Mth.clamp(fade, 0f, 1f) * Math.max(0f, lanternFade); // итоговая прозрачность — в render()
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        if (groundMist) {
            renderGroundMist(buffer, camera, partialTick);
            return;
        }
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
		float battlefield = wall ? 1f : BattlefieldClarity.particleMultiplier(this.x, this.z);
        if (battlefield < 0.04f) return;
        this.alpha = baseAlpha * zoneAlpha * near * near * battlefield; // мягкое, а не линейное растворение
		super.render(buffer, camera, partialTick);
		this.alpha = baseAlpha;
	}

	private void renderGroundMist(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTick, xo, x) - cam.x);
        float py = (float) (Mth.lerp(partialTick, yo, y) - cam.y);
        float pz = (float) (Mth.lerp(partialTick, zo, z) - cam.z);
        float r = quadSize;
        int alpha8 = Mth.clamp(Math.round(alpha * .14f * 255f), 0, 255);
        if (alpha8 == 0) return;
        int light = getLightColor(partialTick);
        // A paper-thin double-sided veil just above the actual block surface.
        groundVertex(buffer,px-r,py,pz-r,getU0(),getV0(),light,alpha8);
        groundVertex(buffer,px-r,py,pz+r,getU0(),getV1(),light,alpha8);
        groundVertex(buffer,px+r,py,pz+r,getU1(),getV1(),light,alpha8);
        groundVertex(buffer,px+r,py,pz-r,getU1(),getV0(),light,alpha8);
        groundVertex(buffer,px+r,py,pz-r,getU1(),getV0(),light,alpha8);
        groundVertex(buffer,px+r,py,pz+r,getU1(),getV1(),light,alpha8);
        groundVertex(buffer,px-r,py,pz+r,getU0(),getV1(),light,alpha8);
        groundVertex(buffer,px-r,py,pz-r,getU0(),getV0(),light,alpha8);
    }

    private static void groundVertex(VertexConsumer buffer, float x,float y,float z,
                                     float u,float v,int light,int alpha) {
        buffer.addVertex(x,y,z).setUv(u,v).setColor(228,232,242,alpha).setLight(light);
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
            // Sentinel distinguishes the still, ground-hugging geometry
            // from ordinary drifting fog; no new registered particle.
            if (xd == -2d) {
                particle.configureGroundMist();
                return particle;
            }
			// xd/yd/zd здесь — не скорость, а оттенок клуба (см. FogBankSpawner)
			if (xd > 0 || yd > 0 || zd > 0) particle.setColor((float) xd, (float) yd, (float) zd);
			return particle;
		}
	}
}
