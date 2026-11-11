package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.CaterpillarEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Модель гусеницы автора (blockbench/caterpillar.bbmodel → caterpillar_model.java).
 * Сетка UV 32×32, текстура может быть 64×64 (плотность 2×) — так и задумано.
 * Анимации: walk (скорость — по скорости движения), idle (стоя), bite (поедание).
 */
public class CaterpillarModel extends HierarchicalModel<CaterpillarEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("caterpillar"), "main");

	/** Сколько «миллисекунд анимации» на единицу пройденного пути: больше — чаще сжимается. */
	private static final float WALK_SPEED = 3.0f;
	/** Насколько быстро ходьба набирает полную силу. */
	private static final float WALK_STRENGTH = 4.0f;

	private final ModelPart root;
	private final ModelPart head;

	public CaterpillarModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeDeformation none = CubeDeformation.NONE;

		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 17)
				.addBox(-1.0F, -3.0F, -2.0F, 3.0F, 3.0F, 3.0F, none), PartPose.offset(0.0F, 24.0F, -6.0F));
		root.addOrReplaceChild("segment0", CubeListBuilder.create().texOffs(14, 10)
				.addBox(-2.0F, -4.0F, -1.0F, 4.0F, 4.0F, 3.0F, none), PartPose.offset(0.5F, 24.0F, -4.0F));
		root.addOrReplaceChild("segment1", CubeListBuilder.create().texOffs(0, 0)
				.addBox(-2.5F, -6.0F, -1.0F, 5.0F, 6.0F, 4.0F, none), PartPose.offset(0.5F, 24.0F, -1.0F));
		root.addOrReplaceChild("segment2", CubeListBuilder.create().texOffs(0, 10)
				.addBox(-1.5F, -5.0F, -0.75F, 4.0F, 5.0F, 3.0F, none), PartPose.offset(0.0F, 24.0F, 2.75F));
		root.addOrReplaceChild("segment3", CubeListBuilder.create().texOffs(0, 18)
				.addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 3.0F, none), PartPose.offset(0.5F, 24.0F, 5.5F));
		root.addOrReplaceChild("segment4", CubeListBuilder.create().texOffs(18, 0)
				.addBox(-1.0F, -2.0F, 1.5F, 2.0F, 2.0F, 2.0F, none), PartPose.offset(0.5F, 24.0F, 6.5F));

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(CaterpillarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		// голова чуть следит за взглядом
		head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4f;
		head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3f;

		if (!entity.biteState.isStarted()) {
			this.animateWalk(CaterpillarAnimations.WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
		}
		this.animate(entity.idleState, CaterpillarAnimations.IDLE, ageInTicks, 1.0f);
		this.animate(entity.biteState, CaterpillarAnimations.BITE, ageInTicks, 1.0f);
	}
}
