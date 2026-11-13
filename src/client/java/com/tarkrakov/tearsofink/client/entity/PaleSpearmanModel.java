package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.spearman.PaleSpearmanEntity;
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
 * Модель Бледного копейщика — автора (blockbench/pale_soldier_spearman.bbmodel).
 * Куб-ориентир «рост игрока» (bb_main) убран; модель сдвинута так, чтобы стоять по центру хитбокса.
 */
public class PaleSpearmanModel extends HierarchicalModel<PaleSpearmanEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_spearman"), "main");
	private static final float WALK_SPEED = 2.0f, WALK_STRENGTH = 2.5f;

	private final ModelPart root;
	private final ModelPart head;

	public PaleSpearmanModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("body").getChild("neck").getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition hand_r = partdefinition.addOrReplaceChild("hand_r", CubeListBuilder.create().texOffs(28, 18).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(6.0F, 4.0F, 0.0F));

		PartDefinition hand_r2 = hand_r.addOrReplaceChild("hand_r2", CubeListBuilder.create().texOffs(0, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_r3 = hand_r.addOrReplaceChild("hand_r3", CubeListBuilder.create().texOffs(8, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition spear = hand_r3.addOrReplaceChild("spear", CubeListBuilder.create().texOffs(24, 18).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 31.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(40, 34).addBox(-1.5F, 21.0F, -0.5F, 3.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(32, 35).addBox(-0.5F, 26.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 6.5F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition hand_l = partdefinition.addOrReplaceChild("hand_l", CubeListBuilder.create().texOffs(28, 26).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.0F, 4.0F, 0.0F));

		PartDefinition hand_l2 = hand_l.addOrReplaceChild("hand_l2", CubeListBuilder.create().texOffs(16, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, 0.0F));

		PartDefinition hand_l3 = hand_l.addOrReplaceChild("hand_l3", CubeListBuilder.create().texOffs(28, 40).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition leg_r = partdefinition.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 30).addBox(-1.75F, 0.0F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 10.0F, 0.0F));

		PartDefinition leg_r2 = partdefinition.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(32, 9).addBox(-1.75F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, 18.0F, 0.5F));

		PartDefinition leg_l2 = partdefinition.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(32, 0).addBox(-1.25F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 18.0F, 0.5F));

		PartDefinition leg_l = partdefinition.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(12, 30).addBox(-1.25F, -1.0F, -1.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.5F, 11.0F, 0.0F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.5F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 5.5F, 0.0F));

		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(28, 34).addBox(-1.0F, -3.0F, -3.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.5F, 6.0F));

		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(0, 21).mirror().addBox(3.0F, 0.0F, -3.0F, 0.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 21).addBox(-3.0F, 0.0F, -3.0F, 0.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(PaleSpearmanEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);
		head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6f;
		head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;

		if (entity.specialAnimating()) {
			this.animate(entity.thrustState, PaleSpearmanAnimations.THRUST, ageInTicks, 1f);
			this.animate(entity.dodgeState, PaleSpearmanAnimations.DODGE, ageInTicks, 1f);
			return;
		}
		if (entity.isAggressiveSynced()) {
			this.animateWalk(PaleSpearmanAnimations.COMBAT_WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
			this.animate(entity.stanceState, PaleSpearmanAnimations.STANCE, ageInTicks, 1f);
		} else {
			this.animateWalk(PaleSpearmanAnimations.WALK, limbSwing, limbSwingAmount, WALK_SPEED, WALK_STRENGTH);
			this.animate(entity.idleState, PaleSpearmanAnimations.IDLE, ageInTicks, 1f);
		}
	}
}
