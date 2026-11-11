package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.CaterpillarEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * ЗАГЛУШКА модели гусеницы: голова + 4 сегмента, ползёт волной.
 * Будет заменена моделью автора из Blockbench (формат Modded Entity).
 */
public class CaterpillarModel extends HierarchicalModel<CaterpillarEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("caterpillar"), "main");
	private static final int SEGMENTS = 4;

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart[] segments = new ModelPart[SEGMENTS];

	public CaterpillarModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
		for (int i = 0; i < SEGMENTS; i++) segments[i] = root.getChild("segment" + i);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2f, -4f, -2f, 4, 4, 4),
				PartPose.offset(0f, 24f, -6f));
		for (int i = 0; i < SEGMENTS; i++) {
			root.addOrReplaceChild("segment" + i, CubeListBuilder.create().texOffs(0, 8 + i * 6).addBox(-1.5f, -3f, -1.5f, 3, 3, 3),
					PartPose.offset(0f, 24f, -3f + i * 3f));
		}
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(CaterpillarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// голова смотрит по сторонам и «кусает»
		head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
		head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f + Mth.sin(this.attackTime * Mth.PI) * 0.8f;
		// волна по сегментам при ходьбе + лёгкое «дыхание» стоя
		for (int i = 0; i < SEGMENTS; i++) {
			float wave = Mth.sin(limbSwing * 1.6f - i * 0.9f) * limbSwingAmount * 1.4f;
			float breathe = Mth.sin(ageInTicks * 0.1f - i * 0.6f) * 0.15f;
			segments[i].y = 24f - Math.max(0f, wave) - breathe;
		}
		head.y = 24f - Math.max(0f, Mth.sin(limbSwing * 1.6f + 0.9f) * limbSwingAmount * 1.0f);
	}
}
