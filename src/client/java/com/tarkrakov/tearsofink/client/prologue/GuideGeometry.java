package com.tarkrakov.tearsofink.client.prologue;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Generated from blockbench/guide.bbmodel. */
final class GuideGeometry {
	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition part14 = root.addOrReplaceChild("robe", CubeListBuilder.create()
				.texOffs(1, 1).addBox(-5F, -22F, -3F, 10F, 21F, 7F), PartPose.offset(0F, 24F, 0F));
		part14.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(36, 1).addBox(-4.9F, -2F, -1F, 10F, 21F, 3F), PartPose.offsetAndRotation(0F, -19F, 3F, 0.087266F, 0F, -0.017453F));
		part14.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(63, 1).addBox(-4.2F, -1F, -4F, 6F, 17F, 3F), PartPose.offsetAndRotation(-2F, -17F, 0F, 0.087266F, 0.10472F, -0.069813F));
		part14.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(82, 1).addBox(-1.8F, -1F, -4F, 6F, 18F, 3F), PartPose.offsetAndRotation(2F, -19F, 0F, 0.05236F, -0.10472F, 0.069813F));
		part14.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(101, 1).addBox(-2F, -1F, -2F, 2F, 18F, 5F), PartPose.offsetAndRotation(-4F, -19F, 0F, 0F, 0F, -0.087266F));
		part14.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(116, 1).addBox(0F, 0F, -2F, 2F, 18F, 5F), PartPose.offsetAndRotation(4F, -19F, 0F, 0F, 0F, 0.069813F));
		PartDefinition part26 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(1, 30).addBox(-5F, -16F, -3F, 10F, 17F, 7F)
				.texOffs(108, 30).addBox(-7.3F, -17F, -2.8F, 14F, 7F, 7F)
				.texOffs(1, 55).addBox(-4.4F, -18F, -3.7F, 9F, 4F, 7F), PartPose.offsetAndRotation(0F, 7F, 0F, 0.244346F, 0F, 0F));
		part26.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(36, 30).addBox(-5.7F, -1F, -3.6F, 11F, 3F, 7F), PartPose.offsetAndRotation(0F, -2F, 0F, 0F, 0F, 0.069813F));
		part26.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(73, 30).addBox(-6.6F, 0F, -1.5F, 13F, 9F, 4F), PartPose.offsetAndRotation(0F, -16F, 3F, 0.087266F, 0F, 0F));
		part26.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(34, 55).addBox(-3.5F, -2F, -1.2F, 6F, 14F, 2F), PartPose.offsetAndRotation(3F, -15F, -3F, 0F, 0F, 0.139626F));
		part26.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(51, 55).addBox(-3.5F, -1F, -1.1F, 5F, 12F, 2F), PartPose.offsetAndRotation(-3F, -15F, -3F, 0F, 0F, -0.15708F));
		part26.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(66, 55).addBox(-0.8F, -1F, -0.7F, 3F, 2F, 1F), PartPose.offsetAndRotation(0F, -7F, -4F, 0F, 0F, 0.122173F));
		PartDefinition part40 = part26.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(75, 55).addBox(-4.8F, -9F, -1.8F, 9F, 10F, 8F)
				.texOffs(1, 74).addBox(-3.8F, -7.7F, -4.5F, 7F, 7F, 4F), PartPose.offsetAndRotation(0F, -16F, -2.8F, -0.122173F, 0F, 0F));
		part40.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(110, 55).addBox(-4.2F, -4F, -4.8F, 8F, 5F, 8F), PartPose.offsetAndRotation(0F, -6F, 1.8F, 0.122173F, 0F, 0.05236F));
		part40.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(24, 74).addBox(-3.3F, -1.7F, -5.8F, 6F, 3F, 8F), PartPose.offsetAndRotation(0F, -8F, 3.8F, 0.174533F, 0F, -0.087266F));
		part40.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(53, 74).addBox(-4.3F, -1.8F, -3.2F, 9F, 2F, 5F), PartPose.offsetAndRotation(0F, -7F, -2.2F, 0.15708F, 0F, -0.05236F));
		part40.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(82, 74).addBox(-1.3F, -2.1F, -3.5F, 2F, 8F, 5F), PartPose.offsetAndRotation(-4F, -5F, -1.2F, 0F, 0.122173F, -0.10472F));
		part40.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(97, 74).addBox(-0.9F, -2F, -4F, 2F, 7F, 5F), PartPose.offsetAndRotation(3.9F, -5F, -1.2F, 0F, -0.122173F, 0.087266F));
		part40.addOrReplaceChild("cube_6", CubeListBuilder.create()
				.texOffs(112, 74).addBox(-3.4F, -0.5F, -0.7F, 7F, 3F, 2F), PartPose.offsetAndRotation(0F, -2F, -4.2F, -0.069813F, 0F, -0.069813F));
		part40.addOrReplaceChild("cube_7", CubeListBuilder.create()
				.texOffs(131, 74).addBox(-4.3F, -1.3F, -0.6F, 8F, 8F, 3F), PartPose.offsetAndRotation(0F, -4F, 4.8F, 0.226893F, 0F, 0.05236F));
		PartDefinition part57 = part26.addOrReplaceChild("arm_l", CubeListBuilder.create()
				.texOffs(1, 88).addBox(-2.7F, -1.5F, -2.2F, 5F, 13F, 5F), PartPose.offsetAndRotation(6.6F, -14F, 0.1F, -0.05236F, 0F, -0.034907F));
		part57.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(22, 88).addBox(-3.45F, -1.8F, -2.35F, 6F, 7F, 5F), PartPose.offsetAndRotation(0F, -1F, 0F, 0F, 0F, -0.122173F));
		PartDefinition part61 = part57.addOrReplaceChild("elbow_l", CubeListBuilder.create()
				.texOffs(45, 88).addBox(-2.1F, -2.1F, -2.1F, 4F, 12F, 4F)
				.texOffs(81, 88).addBox(-2.3F, 8.9F, -2.55F, 4F, 2F, 4F)
				.texOffs(98, 88).addBox(-1.7F, 9.6F, -2F, 3F, 4F, 2F), PartPose.offsetAndRotation(0.9F, 11F, 0F, -0.10472F, 0F, 0.069813F));
		part61.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(62, 88).addBox(-1.9F, -2F, -2.35F, 4F, 4F, 5F), PartPose.offsetAndRotation(0F, 8F, 0F, 0F, 0F, 0.10472F));
		part61.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(109, 88).addBox(-1.85F, -0.8F, -1.35F, 3F, 3F, 2F), PartPose.offsetAndRotation(0F, 13F, -1F, -0.296706F, 0F, 0F));
		part61.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(120, 88).addBox(-0.5F, -0.8F, -0.9F, 1F, 3F, 1F), PartPose.offsetAndRotation(-0.85F, 11F, -2F, 0.20944F, 0F, 0.15708F));
		PartDefinition part71 = part26.addOrReplaceChild("arm_r", CubeListBuilder.create()
				.texOffs(125, 88).addBox(-2.7F, -1.5F, -2.2F, 5F, 13F, 5F), PartPose.offsetAndRotation(-6.6F, -14F, -0.1F, -0.05236F, 0F, 0.034907F));
		part71.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(1, 107).addBox(-3.45F, -1.8F, -2.35F, 6F, 7F, 5F), PartPose.offsetAndRotation(0F, -1F, 0F, 0F, 0F, 0.122173F));
		PartDefinition part75 = part71.addOrReplaceChild("elbow_r", CubeListBuilder.create()
				.texOffs(24, 107).addBox(-2.1F, -2.1F, -2.1F, 4F, 12F, 4F)
				.texOffs(60, 107).addBox(-2.3F, 8.9F, -2.55F, 4F, 2F, 4F)
				.texOffs(77, 107).addBox(-1.7F, 9.6F, -2F, 3F, 4F, 2F), PartPose.offsetAndRotation(-0.9F, 11F, 0F, -0.10472F, 0F, -0.069813F));
		part75.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(41, 107).addBox(-1.9F, -2F, -2.35F, 4F, 4F, 5F), PartPose.offsetAndRotation(0F, 8F, 0F, 0F, 0F, -0.10472F));
		part75.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(88, 107).addBox(-1.85F, -0.8F, -1.35F, 3F, 3F, 2F), PartPose.offsetAndRotation(0F, 13F, -1F, -0.296706F, 0F, 0F));
		part75.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(99, 107).addBox(-0.5F, -0.8F, -0.9F, 1F, 3F, 1F), PartPose.offsetAndRotation(0.85F, 11F, -2F, 0.20944F, 0F, -0.15708F));
		return LayerDefinition.create(mesh, 160, 160);
	}
}
