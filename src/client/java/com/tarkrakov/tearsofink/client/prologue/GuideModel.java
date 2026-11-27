package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.GuideEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/** Сутулый силуэт Проводника, с руками и капюшоном на независимых суставах. */
public class GuideModel extends HierarchicalModel<GuideEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("guide"), "main");
	private final ModelPart root, body, head, armL, armR, elbowL, elbowR;

	public GuideModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.armL = body.getChild("arm_l");
		this.armR = body.getChild("arm_r");
		this.elbowL = armL.getChild("elbow_l");
		this.elbowR = armR.getChild("elbow_r");
	}

	public static LayerDefinition createBodyLayer() {
		return GuideGeometry.createBodyLayer();
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(GuideEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		head.yRot += Mth.clamp(yaw * 0.6f, -48f, 48f) * Mth.DEG_TO_RAD;
		head.xRot += Mth.clamp(pitch * 0.5f, -22f, 22f) * Mth.DEG_TO_RAD;
		body.xRot += Mth.sin(age * 0.05f) * 0.03f;
		armL.zRot -= Mth.sin(age * 0.05f) * 0.03f;
		armR.zRot += Mth.sin(age * 0.05f) * 0.03f;
		switch (e.action()) {
			case GuideEntity.ACT_TALK -> {
				armR.xRot += -0.55f + Mth.sin(age * 0.25f) * 0.22f;
				elbowR.xRot -= 0.12f;
			}
			case GuideEntity.ACT_GIFT -> {
				armR.xRot -= 1.23f;
				armL.xRot -= 1.18f;
				elbowR.xRot -= 0.06f;
				elbowL.xRot -= 0.03f;
			}
			case GuideEntity.ACT_VANISH -> body.xRot += 0.5f;
			default -> {}
		}
	}
}
