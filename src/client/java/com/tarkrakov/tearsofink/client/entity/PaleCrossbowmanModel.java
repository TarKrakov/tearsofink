package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity;
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

/** Модель Бледного арбалетчика — автора (blockbench/pale_soldier_crossbowman.bbmodel; перенесена из .bbmodel кодом). */
public class PaleCrossbowmanModel extends HierarchicalModel<PaleCrossbowmanEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_crossbowman"), "main");
	private static final float WALK_SPEED = 1.6f, WALK_STRENGTH = 2.5f;

	private final ModelPart root;
	private final ModelPart head;

	public PaleCrossbowmanModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("body").getChild("neck").getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(-3, -2).addBox(-1.0F, -2.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 4.0F, -2.0F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));
		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition crossbow = hand_r3.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(-9, -8).addBox(-3.0F, 2.0F, -3.5F, 4.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(-9, -8).addBox(-3.0F, 3.0F, -3.5F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(-2, -1).addBox(2.0F, 4.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(-2, -1).addBox(-5.0F, 4.0F, -1.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(-10, -8).addBox(0.0F, 3.0F, -3.5F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(1, 1).addBox(-1.5F, -1.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.5F, -0.5F, 1.5708F, 0.0F, 0.0F));
		PartDefinition cube_r1 = crossbow.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(-9, -8).addBox(1.0F, 4.0F, -6.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(-5, -2).addBox(-4.0F, -2.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 4.0F, -2.0F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));
		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));
		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.75F, 0.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 10.0F, -1.0F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.75F, 0.0F, -1.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 18.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.25F, 0.0F, -1.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 18.0F, -0.5F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(-1, 0).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 11.0F, -1.0F, -3.1416F, 0.0F, 3.1416F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(-9, -5).addBox(-2.0F, -5.5F, -3.0F, 4.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 5.5F, -1.0F, 2.8362F, 0.0F, 3.1416F));
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(-2, -2).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.5F, 6.0F, 0.2618F, 0.0F, 0.0F));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 3.0F));
		PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(-6, -3).addBox(-2.5F, -2.0F, 0.75F, 5.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -3.0F, 0.0436F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(PaleCrossbowmanEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		int act = e.action();
		if (act == PaleCrossbowmanEntity.ACT_NONE) {
			head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
			head.xRot += headPitch * Mth.DEG_TO_RAD * 0.4f;
		}
		float speed = e.fastCycle() ? 2f : 1f; // по мобам целится и перезаряжается вдвое быстрее
		if (act != PaleCrossbowmanEntity.ACT_DODGE) this.animateWalk(PaleCrossbowmanAnimations.WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
		switch (act) {
			case PaleCrossbowmanEntity.ACT_AIM -> this.animate(e.aimState, PaleCrossbowmanAnimations.AIM, ageInTicks, speed);
			case PaleCrossbowmanEntity.ACT_SHOOT -> this.animate(e.shootState, PaleCrossbowmanAnimations.SHOOT, ageInTicks, 1f);
			case PaleCrossbowmanEntity.ACT_RELOAD -> this.animate(e.reloadState, PaleCrossbowmanAnimations.RELOAD, ageInTicks, speed);
			case PaleCrossbowmanEntity.ACT_DODGE -> this.animate(e.dodgeState, PaleCrossbowmanAnimations.DODGE, ageInTicks, 1f);
			default -> this.animate(e.idleState, PaleCrossbowmanAnimations.IDLE, ageInTicks, 1f);
		}
	}
}
