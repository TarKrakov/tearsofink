package com.tarkrakov.tearsofink.client.trader;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Generated from blockbench/soul_trader.bbmodel. */
final class SoulTraderGeometry {
	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition part14 = root.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(91, 1).addBox(-4F, -12F, -2F, 8F, 12F, 4F)
				.texOffs(116, 1).addBox(-3.9F, -12.2F, -2.5F, 8F, 3F, 6F)
				.texOffs(18, 18).addBox(-4.35F, -2.3F, -2.62F, 9F, 2F, 6F)
				.texOffs(49, 18).addBox(-1.85F, -2.2F, -3.36F, 3F, 2F, 1F), PartPose.offsetAndRotation(0F, 12F, 0F, 0.05236F, 0F, 0F));
		part14.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(1, 18).addBox(-2.8F, -1.1F, -0.1F, 7F, 8F, 1F), PartPose.offsetAndRotation(0F, -10F, -3F, 0F, 0F, -0.087266F));
		part14.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(58, 18).addBox(-3.56F, -0.6F, -0.3F, 5F, 9F, 2F), PartPose.offsetAndRotation(-1.6F, -2F, 2F, 0.139626F, 0.087266F, -0.069813F));
		part14.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(73, 18).addBox(-1.44F, -0.9F, -0.1F, 5F, 10F, 2F), PartPose.offsetAndRotation(1.6F, -2F, 2F, 0.191986F, -0.069813F, 0.069813F));
		part14.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(88, 18).addBox(-0.95F, -0.5F, -1.9F, 2F, 6F, 4F), PartPose.offsetAndRotation(4.1F, -2F, 0F, 0F, 0F, 0.087266F));
		part14.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(101, 18).addBox(-1.02F, -0.4F, -2F, 2F, 5F, 4F), PartPose.offsetAndRotation(-4.3F, -2F, 0F, 0F, 0F, -0.069813F));
		part14.addOrReplaceChild("cube_6", CubeListBuilder.create()
				.texOffs(114, 18).addBox(-1.45F, -1.1F, -1.45F, 2F, 4F, 3F), PartPose.offsetAndRotation(-4F, -3F, -1F, 0.15708F, 0F, -0.069813F));
		PartDefinition part31 = part14.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(1, 1).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
				.texOffs(34, 1).addBox(-3.5F, -7F, -4.22F, 7F, 6F, 1F), PartPose.offsetAndRotation(0F, -12F, 0F, -0.05236F, 0F, 0F));
		part31.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(51, 1).addBox(-3.95F, -1.4F, -1.58F, 8F, 3F, 2F), PartPose.offsetAndRotation(0F, -2F, -3F, 0.069813F, 0F, 0.034907F));
		part31.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(72, 1).addBox(-3.4F, -2.8F, -0.7F, 7F, 9F, 2F), PartPose.offsetAndRotation(0F, -4F, 4F, 0.139626F, 0F, 0.069813F));
		PartDefinition part38 = part31.addOrReplaceChild("gear_hat_1", CubeListBuilder.create()
				.texOffs(1, 63).addBox(-5F, -3F, -5F, 10F, 6F, 10F)
				.texOffs(42, 63).addBox(-7F, 3F, -1F, 3F, 1F, 2F)
				.texOffs(53, 63).addBox(-7F, 3F, -3F, 3F, 1F, 2F)
				.texOffs(64, 63).addBox(-7F, 3F, -4F, 3F, 1F, 1F)
				.texOffs(73, 63).addBox(-7F, 3F, -5F, 3F, 1F, 1F)
				.texOffs(82, 63).addBox(-7F, 3F, -6F, 14F, 1F, 1F)
				.texOffs(113, 63).addBox(-4F, 3F, -5F, 8F, 1F, 1F)
				.texOffs(132, 63).addBox(-4F, 3F, 4F, 8F, 1F, 1F)
				.texOffs(1, 80).addBox(-5F, 3F, -7F, 10F, 1F, 1F)
				.texOffs(24, 80).addBox(-5F, 3F, 6F, 10F, 1F, 1F)
				.texOffs(47, 80).addBox(-7F, 3F, 5F, 14F, 1F, 1F)
				.texOffs(78, 80).addBox(-7F, 3F, 4F, 3F, 1F, 1F)
				.texOffs(87, 80).addBox(-7F, 3F, 3F, 3F, 1F, 1F)
				.texOffs(96, 80).addBox(-7F, 3F, 1F, 3F, 1F, 2F)
				.texOffs(68, 84).addBox(-5F, 2F, -6F, 10F, 1F, 1F)
				.texOffs(40, 88).addBox(-5F, 2F, 5F, 10F, 1F, 1F)
				.texOffs(108, 88).addBox(-9F, -0.5F, -3F, 1F, 2F, 6F)
				.texOffs(123, 88).addBox(-8.5F, -1.5F, -2F, 1F, 1F, 4F)
				.texOffs(134, 88).addBox(-8F, -2.5F, -1F, 1F, 1F, 2F)
				.texOffs(1, 97).addBox(-8F, 2.5F, -5F, 1F, 1F, 10F)
				.texOffs(24, 97).addBox(-8.5F, 1.5F, -4F, 1F, 1F, 8F), PartPose.offsetAndRotation(0F, -8.2F, 0.15F, -0.017453F, 0F, -0.087266F));
		part38.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(107, 80).addBox(-5F, -1F, 0F, 5F, 1F, 1F), PartPose.offsetAndRotation(4F, 4F, 5F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(120, 80).addBox(-4F, -1F, 0F, 6F, 1F, 1F), PartPose.offsetAndRotation(6F, 4F, 4F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(135, 80).addBox(-5F, -1F, -1F, 7F, 1F, 2F), PartPose.offsetAndRotation(6F, 4F, 2F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(1, 84).addBox(-6F, -1F, -1F, 8F, 1F, 2F), PartPose.offsetAndRotation(6F, 4F, 0F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(22, 84).addBox(-8F, -1F, -1F, 10F, 1F, 1F), PartPose.offsetAndRotation(6F, 3F, -3F, 0F, 1.570796F, 0F));
		part38.addOrReplaceChild("cube_6", CubeListBuilder.create()
				.texOffs(45, 84).addBox(-8F, -1F, -1F, 10F, 1F, 1F), PartPose.offsetAndRotation(-5F, 3F, -3F, 0F, 1.570796F, 0F));
		part38.addOrReplaceChild("cube_7", CubeListBuilder.create()
				.texOffs(91, 84).addBox(-8F, -1F, -1F, 10F, 1F, 1F), PartPose.offsetAndRotation(-3F, -3F, -3F, 0F, 1.570796F, 0F));
		part38.addOrReplaceChild("cube_8", CubeListBuilder.create()
				.texOffs(114, 84).addBox(-8F, -1F, -1F, 10F, 1F, 1F), PartPose.offsetAndRotation(4F, -3F, -3F, 0F, 1.570796F, 0F));
		part38.addOrReplaceChild("cube_9", CubeListBuilder.create()
				.texOffs(137, 84).addBox(-3F, -1F, -1F, 5F, 1F, 1F), PartPose.offsetAndRotation(-3F, 0F, -5F, 1.570796F, 0F, 1.570796F));
		part38.addOrReplaceChild("cube_10", CubeListBuilder.create()
				.texOffs(1, 88).addBox(-3F, -1F, -1F, 5F, 1F, 1F), PartPose.offsetAndRotation(-3F, 0F, 6F, 1.570796F, 0F, 1.570796F));
		part38.addOrReplaceChild("cube_11", CubeListBuilder.create()
				.texOffs(14, 88).addBox(-8F, -1F, -1F, 5F, 1F, 1F), PartPose.offsetAndRotation(4F, -6F, 5F, -1.570796F, 0F, -1.570796F));
		part38.addOrReplaceChild("cube_12", CubeListBuilder.create()
				.texOffs(27, 88).addBox(-8F, -1F, -1F, 5F, 1F, 1F), PartPose.offsetAndRotation(4F, -6F, -6F, -1.570796F, 0F, -1.570796F));
		part38.addOrReplaceChild("cube_13", CubeListBuilder.create()
				.texOffs(63, 88).addBox(-5F, -1F, -1F, 7F, 1F, 2F), PartPose.offsetAndRotation(6F, 4F, -2F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_14", CubeListBuilder.create()
				.texOffs(82, 88).addBox(-4F, -1F, 0F, 6F, 1F, 1F), PartPose.offsetAndRotation(6F, 4F, -3F, -3.141593F, 0F, 3.141593F));
		part38.addOrReplaceChild("cube_15", CubeListBuilder.create()
				.texOffs(97, 88).addBox(-4F, -1F, 0F, 4F, 1F, 1F), PartPose.offsetAndRotation(4F, 4F, -4F, -3.141593F, 0F, 3.141593F));
		PartDefinition part90 = part14.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(125, 18).addBox(-2.5F, -2F, -1.7F, 4F, 4F, 4F)
				.texOffs(142, 18).addBox(-2.95F, -2F, -2F, 4F, 8F, 4F), PartPose.offsetAndRotation(-6F, -10F, 0F, -0.191986F, 0F, 0.296706F));
		part90.addOrReplaceChild("r_forearm", CubeListBuilder.create()
				.texOffs(1, 31).addBox(-1.3F, -1F, -1.8F, 3F, 7F, 4F)
				.texOffs(16, 31).addBox(-1.25F, -0.8F, -2.4F, 3F, 6F, 2F)
				.texOffs(27, 31).addBox(-1.2F, 4.5F, -2.08F, 3F, 2F, 4F)
				.texOffs(42, 31).addBox(-1.6F, 4.4F, -1.6F, 3F, 4F, 2F)
				.texOffs(53, 31).addBox(0.3F, 5F, -2.25F, 1F, 2F, 1F), PartPose.offsetAndRotation(-0.85F, 6F, 0F, -1.186824F, -0.942478F, 0.15708F));
		PartDefinition part99 = part90.addOrReplaceChild("gear_r_arm_1", CubeListBuilder.create()
				.texOffs(48, 109).addBox(-4F, 1F, -2F, 1F, 2F, 4F)
				.texOffs(59, 109).addBox(1F, 1F, -2F, 1F, 2F, 4F), PartPose.offset(-0.04F, -0.08F, 0.06F));
		part99.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(70, 109).addBox(0F, -2F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(-1F, 3F, -2F, 0F, 1.570796F, 0F));
		part99.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(81, 109).addBox(0F, -2F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(-1F, 3F, 3F, 0F, 1.570796F, 0F));
		PartDefinition part106 = part14.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(58, 31).addBox(-2F, 0F, -1.9F, 4F, 11F, 4F)
				.texOffs(75, 31).addBox(-1.95F, 9F, -2.8F, 4F, 3F, 5F)
				.texOffs(103, 31).addBox(-1.88F, 4F, -2.11F, 4F, 2F, 4F), PartPose.offsetAndRotation(-2.15F, 0F, 0F, 0F, 0F, -0.017453F));
		part106.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(94, 31).addBox(-1.42F, -2F, -0.45F, 3F, 6F, 1F), PartPose.offsetAndRotation(0F, 5F, -2F, 0.05236F, 0F, 0F));
		PartDefinition part112 = part106.addOrReplaceChild("gear_r_leg_1", CubeListBuilder.create()
				.texOffs(136, 109).addBox(-3F, 8F, 0F, 1F, 2F, 3F)
				.texOffs(145, 109).addBox(-3F, 9F, -2F, 1F, 1F, 2F)
				.texOffs(23, 119).addBox(-3F, 7F, 1.5F, 1F, 1F, 2F)
				.texOffs(30, 119).addBox(-3F, 6F, 2F, 1F, 1F, 2F), PartPose.offset(-0.04F, -0.08F, 0.06F));
		part112.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(1, 119).addBox(0F, -1F, 1F, 1F, 1F, 4F), PartPose.offsetAndRotation(-3F, 10F, -2F, 0F, 1.570796F, 0F));
		part112.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(12, 119).addBox(0F, -1F, 1F, 1F, 1F, 4F), PartPose.offsetAndRotation(-3F, 10F, 3F, 0F, 1.570796F, 0F));
		PartDefinition part121 = part14.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(120, 31).addBox(-0.8F, -2F, -1.7F, 4F, 4F, 4F)
				.texOffs(137, 31).addBox(-1.25F, -2F, -2F, 4F, 8F, 4F), PartPose.offsetAndRotation(6F, -10F, 0F, -0.279253F, 0F, -0.296706F));
		part121.addOrReplaceChild("l_forearm", CubeListBuilder.create()
				.texOffs(1, 47).addBox(-1.3F, -1F, -1.8F, 3F, 7F, 4F)
				.texOffs(16, 47).addBox(-1.25F, -0.8F, -2.4F, 3F, 6F, 2F)
				.texOffs(27, 47).addBox(-1.2F, 4.5F, -2.08F, 3F, 2F, 4F)
				.texOffs(42, 47).addBox(-1.6F, 4.4F, -1.6F, 3F, 4F, 2F)
				.texOffs(53, 47).addBox(-1.3F, 5F, -2.25F, 1F, 2F, 1F), PartPose.offsetAndRotation(0.85F, 6F, 0F, -1.308997F, 0.994838F, -0.15708F));
		PartDefinition part130 = part121.addOrReplaceChild("gear_l_arm_1", CubeListBuilder.create()
				.texOffs(92, 109).addBox(3F, 1F, -2F, 1F, 2F, 4F)
				.texOffs(114, 109).addBox(-2F, 1F, -2F, 1F, 2F, 4F), PartPose.offset(0.04F, -0.08F, 0.06F));
		part130.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(103, 109).addBox(0F, -2F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(1F, 3F, -2F, 0F, 1.570796F, 0F));
		part130.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(125, 109).addBox(0F, -2F, -2F, 1F, 2F, 4F), PartPose.offsetAndRotation(1F, 3F, 3F, 0F, 1.570796F, 0F));
		PartDefinition part137 = part14.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(58, 47).addBox(-2F, 0F, -1.9F, 4F, 11F, 4F)
				.texOffs(75, 47).addBox(-1.95F, 9F, -2.8F, 4F, 3F, 5F)
				.texOffs(103, 47).addBox(-1.88F, 4F, -2.11F, 4F, 2F, 4F), PartPose.offsetAndRotation(2.15F, 0F, 0F, 0F, 0F, 0.017453F));
		part137.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(94, 47).addBox(-1.42F, -2F, -0.45F, 3F, 6F, 1F), PartPose.offsetAndRotation(0F, 5F, -2F, 0.05236F, 0F, 0F));
		PartDefinition part143 = part137.addOrReplaceChild("gear_l_leg_1", CubeListBuilder.create()
				.texOffs(37, 119).addBox(2F, 7F, 1.5F, 1F, 1F, 2F)
				.texOffs(44, 119).addBox(2F, 8F, 0F, 1F, 2F, 3F)
				.texOffs(53, 119).addBox(2F, 6F, 2F, 1F, 1F, 2F)
				.texOffs(60, 119).addBox(2F, 9F, -2F, 1F, 1F, 2F), PartPose.offset(0.04F, -0.08F, 0.06F));
		part143.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(67, 119).addBox(0F, -1F, 1F, 1F, 1F, 4F), PartPose.offsetAndRotation(-3F, 10F, -2F, 0F, 1.570796F, 0F));
		part143.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(78, 119).addBox(0F, -1F, 1F, 1F, 1F, 4F), PartPose.offsetAndRotation(-3F, 10F, 3F, 0F, 1.570796F, 0F));
		PartDefinition part152 = part14.addOrReplaceChild("gear_body_2", CubeListBuilder.create()
				.texOffs(43, 97).addBox(-3F, 1F, -3F, 1F, 1F, 1F)
				.texOffs(48, 97).addBox(-2F, 2F, -3F, 1F, 1F, 1F)
				.texOffs(53, 97).addBox(-1F, 3F, -3F, 1F, 1F, 1F)
				.texOffs(58, 97).addBox(3F, 8F, -3F, 1F, 1F, 1F)
				.texOffs(63, 97).addBox(4F, 8F, -2F, 1F, 1F, 4F)
				.texOffs(74, 97).addBox(4F, 9F, -2F, 1F, 1F, 4F)
				.texOffs(85, 97).addBox(-5F, 8F, -2F, 1F, 1F, 4F)
				.texOffs(96, 97).addBox(-5F, 9F, -2F, 1F, 1F, 4F)
				.texOffs(39, 109).addBox(0F, 3F, -3F, 3F, 5F, 1F), PartPose.offset(0F, -11F, 0F));
		part152.addOrReplaceChild("cube_1", CubeListBuilder.create()
				.texOffs(107, 97).addBox(-1F, -1F, -1F, 1F, 1F, 8F), PartPose.offsetAndRotation(3F, 9F, 3F, 0F, -1.570796F, 0F));
		part152.addOrReplaceChild("cube_2", CubeListBuilder.create()
				.texOffs(126, 97).addBox(-1F, -7F, 0F, 1F, 7F, 1F), PartPose.offsetAndRotation(3F, 8F, 3F, 0F, -1.570796F, 0F));
		part152.addOrReplaceChild("cube_3", CubeListBuilder.create()
				.texOffs(131, 97).addBox(-1F, -7F, 0F, 1F, 7F, 1F), PartPose.offsetAndRotation(-2F, 8F, 3F, 0F, -1.570796F, 0F));
		part152.addOrReplaceChild("cube_4", CubeListBuilder.create()
				.texOffs(136, 97).addBox(-1F, -1F, 3F, 1F, 1F, 4F), PartPose.offsetAndRotation(3F, 9F, -2F, 0F, -1.570796F, 0F));
		part152.addOrReplaceChild("cube_5", CubeListBuilder.create()
				.texOffs(1, 109).addBox(-1F, -1F, -1F, 1F, 1F, 8F), PartPose.offsetAndRotation(3F, 10F, -2F, 0F, -1.570796F, 0F));
		part152.addOrReplaceChild("cube_6", CubeListBuilder.create()
				.texOffs(20, 109).addBox(-1F, -1F, -1F, 1F, 1F, 8F), PartPose.offsetAndRotation(3F, 10F, 3F, 0F, -1.570796F, 0F));
		return LayerDefinition.create(mesh, 160, 160);
	}
}
