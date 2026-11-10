package com.tarkrakov.tearsofink.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Капля в три фазы: висит под листом → падает → брызги на земле. */
public class DripParticle extends TextureSheetParticle {
	public enum Phase { HANG, FALL, LAND }

	private final Phase phase;
	private final ParticleOptions next;

	protected DripParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, Phase phase, ParticleOptions next) {
		super(level, x, y, z);
		this.phase = phase;
		this.next = next;
		this.pickSprite(sprites);
		this.quadSize = 0.06f;
		this.xd = this.yd = this.zd = 0;
		switch (phase) {
			case HANG -> { this.gravity = 0f; this.lifetime = Mth.nextInt(random, 30, 60); this.hasPhysics = false; }
			case FALL -> { this.gravity = 0.06f; this.lifetime = 200; this.hasPhysics = true; }
			case LAND -> { this.gravity = 0f; this.lifetime = Mth.nextInt(random, 10, 20); this.hasPhysics = false; }
		}
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.age++ >= this.lifetime) {
			if (phase == Phase.HANG && next != null) level.addParticle(next, x, y, z, 0, 0, 0);
			this.remove();
			return;
		}
		if (phase == Phase.FALL) {
			this.yd -= this.gravity;
			this.move(this.xd, this.yd, this.zd);
			this.yd *= 0.98;
			if (this.onGround) {
				if (next != null) level.addParticle(next, x, y + 0.01, z, 0, 0, 0);
				this.remove();
			}
		}
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;
		private final Phase phase;
		private final ParticleOptions next;

		public Provider(SpriteSet sprites, Phase phase, ParticleOptions next) {
			this.sprites = sprites;
			this.phase = phase;
			this.next = next;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
									   double xd, double yd, double zd) {
			return new DripParticle(level, x, y, z, sprites, phase, next);
		}
	}
}
