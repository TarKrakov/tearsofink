package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Старая геометрия отключённого Хранителя Алой Жатвы. */
public class HarvestKeeperModel extends HierarchicalModel<SoulTraderEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("harvest_keeper"), "main");
	private final ModelPart root, body, head, armL, armR;

	public HarvestKeeperModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.armL = body.getChild("arm_l");
		this.armR = body.getChild("arm_r");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition r = mesh.getRoot();
		r.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(0, 32).addBox(-5, -16, -3.5f, 10, 16, 7), PartPose.offset(0, 24, 0));
		PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -14, -2.5f, 8, 14, 5),
				PartPose.offsetAndRotation(0, 8, 0, 0.1f, 0, 0));
		body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5f, -7, -3.5f, 7, 7, 7)
				.texOffs(0, 16).addBox(-4, -7.5f, -4, 8, 5, 8), PartPose.offset(0, -14, 0));
		body.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(40, 32).addBox(0, 0, -1.5f, 2, 15, 3), PartPose.offset(4, -13, 0));
		body.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(40, 32).addBox(-2, 0, -1.5f, 2, 15, 3), PartPose.offset(-4, -13, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(SoulTraderEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		head.yRot = yaw * Mth.DEG_TO_RAD * 0.7f;
		head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
		// парит и чуть колышется, руки сложены перед собой
		root.y += Mth.sin(age * 0.06f) * 0.6f;
		body.xRot += Mth.sin(age * 0.05f) * 0.03f;
		armL.xRot = -0.7f + Mth.sin(age * 0.05f) * 0.04f;
		armR.xRot = -0.7f + Mth.sin(age * 0.05f + 1f) * 0.04f;
		armL.zRot = -0.35f;
		armR.zRot = 0.35f;
	}
}
