package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity;
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

/** Модель Бледного щитовика — автора (blockbench/pale_soldier_shieldman.bbmodel). */
public class PaleShieldbearerModel extends HierarchicalModel<PaleShieldbearerEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_shieldbearer"), "main");
	private static final float WALK_SPEED = 1.6f, WALK_STRENGTH = 2.5f;

	private final ModelPart root;
	private final ModelPart head;

	public PaleShieldbearerModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("body").getChild("neck").getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(-6, -3).addBox(-4.0F, -3.0F, -3.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 2.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(-2, -1).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 2.0F, -1.0F));

		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(-2, -1).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 10.0F, -1.0F));

		PartDefinition Shield = hand_r3.addOrReplaceChild("Shield", CubeListBuilder.create().texOffs(-14, -13).addBox(-6.0F, -16.0F, -7.0F, 3.0F, 25.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(-5, -3).addBox(-4.0F, -5.0F, -2.0F, 4.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 1.5F, 1.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(-6, -3).addBox(-2.0F, -5.0F, -3.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 4.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(-2, -1).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 0.0F, -1.0F));

		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(-2, -1).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.0F, 8.0F, -1.0F));

		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(-3, -2).addBox(-2.25F, 0.0F, -1.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 10.0F, 0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(-4, -3).addBox(-2.25F, -1.0F, -3.5F, 3.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 18.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(-4, -3).addBox(-1.25F, -1.0F, -3.5F, 3.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 18.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(-3, -2).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 11.0F, 0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(-14, -7).addBox(-5.0F, -6.5F, -4.0F, 9.0F, 11.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, -0.5F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(-2, -1).addBox(-2.0F, -3.0F, -3.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.5F, 1.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(-10, -4).addBox(-3.0F, -4.5F, -2.25F, 5.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(PaleShieldbearerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		int act = entity.action();
		if (act == PaleShieldbearerEntity.ACT_NONE || act == PaleShieldbearerEntity.ACT_BLOCK) {
			head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
			head.xRot += headPitch * Mth.DEG_TO_RAD * 0.4f;
		}
		switch (act) {
			case PaleShieldbearerEntity.ACT_BASH -> this.animate(entity.bashState, PaleShieldbearerAnimations.BASH, ageInTicks, 1f);
			case PaleShieldbearerEntity.ACT_CHARGE_WINDUP -> this.animate(entity.chargeStartState, PaleShieldbearerAnimations.CHARGE_START, ageInTicks, 1f);
			case PaleShieldbearerEntity.ACT_CHARGE -> this.animate(entity.chargeState, PaleShieldbearerAnimations.CHARGE, ageInTicks, 1f);
			case PaleShieldbearerEntity.ACT_STUN -> this.animate(entity.stunState, PaleShieldbearerAnimations.STUN, ageInTicks, 1f);
			case PaleShieldbearerEntity.ACT_BLOCK -> {
				this.animateWalk(PaleShieldbearerAnimations.BLOCK_WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
				this.animate(entity.blockState, PaleShieldbearerAnimations.BLOCK, ageInTicks, 1f);
			}
			default -> {
				this.animateWalk(PaleShieldbearerAnimations.WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
				this.animate(entity.idleState, PaleShieldbearerAnimations.IDLE, ageInTicks, 1f);
			}
		}
	}
}
