package com.tarkrakov.tearsofink.client.particle;

import com.tarkrakov.tearsofink.block.lantern.FlameAnchor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Огонёк в камере фонаря: никуда не летит, по кругу листает кадры огня автора
 * и чуть колышется, как пламя на ветру. Гаснет вместе с фонарём.
 */
public class FlameDanceParticle extends TextureSheetParticle {
	// ===== Настройки — крутить здесь =====
	/** Размер огонька (половина стороны квадрата, в блоках). */
	public static final float SIZE = 0.2f;
	/** Сколько тиков показывается каждый кадр (20 тиков = 1 секунда). */
	public static final int TICKS_PER_FRAME = 3;
	/** Насколько огонёк колышется (в блоках). */
	public static final double SWAY = 0.012;

	private final SpriteSet sprites;
	private final BlockPos anchor;
	private final double baseX, baseY, baseZ;
	private final float phase;

	protected FlameDanceParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		this.anchor = BlockPos.containing(x, y - 1, z); // огонёк выше блока фонаря — ищем блок под ним
		this.baseX = x;
		this.baseY = y;
		this.baseZ = z;
		this.phase = this.random.nextFloat() * 100f;
		this.quadSize = SIZE;
		this.lifetime = Integer.MAX_VALUE;
		this.gravity = 0f;
		this.hasPhysics = false;
		this.xd = this.yd = this.zd = 0;
		this.setSprite(sprites.get(0, 1));
	}

	/** Блок фонаря, к которому привязан огонёк (сам блок или под ним). */
	private boolean anchorStillBurning() {
		for (BlockPos pos : new BlockPos[]{BlockPos.containing(baseX, baseY, baseZ), anchor}) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof FlameAnchor flame) {
				Vec3 offset = flame.flameOffset(state);
				if (offset != null && Math.abs(pos.getX() + offset.x - baseX) < 0.01
						&& Math.abs(pos.getY() + offset.y - baseY) < 0.01 && Math.abs(pos.getZ() + offset.z - baseZ) < 0.01) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.age++;
		if (this.age % 10 == 0 && !anchorStillBurning()) {
			this.remove();
			return;
		}
		float t = this.age + phase;
		this.setPos(baseX + Mth.sin(t * 0.13f) * SWAY, baseY, baseZ + Mth.cos(t * 0.11f) * SWAY);
		// 8 кадров по кругу
		int frames = 8;
		int frame = (this.age / TICKS_PER_FRAME) % frames;
		this.setSprite(sprites.get(frame, frames - 1));
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
			return new FlameDanceParticle(level, x, y, z, this.sprites);
		}
	}
}
