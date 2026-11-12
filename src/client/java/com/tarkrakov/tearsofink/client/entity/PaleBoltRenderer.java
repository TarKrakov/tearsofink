package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleBoltEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Болт рисуется как стрела, со своей текстурой (заглушка — разметка как у ванильной стрелы). */
public class PaleBoltRenderer extends ArrowRenderer<PaleBoltEntity> {
	private static final ResourceLocation TEXTURE = TearsOfInk.id("textures/entity/projectiles/pale_bolt.png");

	public PaleBoltRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(PaleBoltEntity entity) {
		return TEXTURE;
	}
}
