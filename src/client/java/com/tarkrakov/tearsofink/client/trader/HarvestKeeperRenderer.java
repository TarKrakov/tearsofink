package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.hunt.HarvestKeeperEntity;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Хранитель Алой Жатвы — заглушка: модель Торговки, своя текстура entity/harvest_keeper.png (перекраска в багровое). */
public class HarvestKeeperRenderer extends SoulTraderRenderer {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/harvest_keeper.png");

	public HarvestKeeperRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(SoulTraderEntity entity) {
		return TEXTURE;
	}
}
