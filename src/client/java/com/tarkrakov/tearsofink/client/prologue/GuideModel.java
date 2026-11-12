package com.tarkrakov.tearsofink.client.prologue;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.prologue.GuideEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Заглушка Г-на Проводника: высокий сгорбленный балахон (~2,6 блока). Заменится моделью автора. */
public class GuideModel extends HierarchicalModel<GuideEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("guide"), "main");
	private final ModelPart root, body, head, armL, armR;

	public GuideModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.armL = body.getChild("arm_l");
		this.armR = body.getChild("arm_r");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition r = mesh.getRoot();
		r.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(0, 32).addBox(-6, -18, -4, 12, 18, 8), PartPose.offset(0, 24, 0));
		PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-5, -16, -3, 10, 16, 6),
				PartPose.offsetAndRotation(0, 6, 0, 0.35f, 0, 0));
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
				.texOffs(0, 16).addBox(-4.5f, -8.5f, -4.5f, 9, 5, 9), PartPose.offsetAndRotation(0, -16, -1, 0.2f, 0, 0));
		body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(40, 32).addBox(0, 0, -2, 3, 18, 4), PartPose.offset(5, -15, 0));
		body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(40, 32).addBox(-3, 0, -2, 3, 18, 4), PartPose.offset(-5, -15, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(GuideEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
		head.xRot += pitch * Mth.DEG_TO_RAD * 0.5f;
		body.xRot += Mth.sin(age * 0.05f) * 0.03f;
		armL.zRot = -0.05f - Mth.sin(age * 0.05f) * 0.03f;
		armR.zRot = 0.05f + Mth.sin(age * 0.05f) * 0.03f;
		switch (e.action()) {
			case GuideEntity.ACT_TALK -> armR.xRot = -0.6f + Mth.sin(age * 0.25f) * 0.25f;
			case GuideEntity.ACT_GIFT -> { armR.xRot = -1.3f; armL.xRot = -1.3f; }
			case GuideEntity.ACT_VANISH -> body.xRot += 0.5f;
			default -> {}
		}
	}
}
