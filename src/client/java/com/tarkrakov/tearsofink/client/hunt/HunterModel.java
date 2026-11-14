package com.tarkrakov.tearsofink.client.hunt;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.hunt.HunterEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * Охотник — заглушка на скелете игрока (64×64, развёртка как у скина). Удары от третьего лица — те же, что у игрока
 * (ThirdPerson через HumanoidModelMixin). Модель автора с навесками gear_* встанет сюда же.
 */
public class HunterModel extends PlayerModel<HunterEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("hunter"), "main");

	public HunterModel(ModelPart root) {
		super(root, false);
	}

	public static LayerDefinition createBodyLayer() {
		return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
	}

	@Override
	public void prepareMobModel(HunterEntity entity, float limbSwing, float limbAmount, float partialTick) {
		super.prepareMobModel(entity, limbSwing, limbAmount, partialTick);
		rightArmPose = entity.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
		leftArmPose = entity.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
	}
}
