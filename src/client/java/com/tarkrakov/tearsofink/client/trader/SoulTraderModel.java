package com.tarkrakov.tearsofink.client.trader;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.trader.SoulTraderEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/** Собранная форма Шпиля. Согнутые предплечья и кисти входят в исходную позу. */
public class SoulTraderModel extends HierarchicalModel<SoulTraderEntity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("soul_trader"), "main");
	private final ModelPart root, body, head, armL, armR, forearmL, forearmR;

	public SoulTraderModel(ModelPart root) {
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.armL = body.getChild("left_arm");
		this.armR = body.getChild("right_arm");
		this.forearmL = armL.getChild("l_forearm");
		this.forearmR = armR.getChild("r_forearm");
	}

	public static LayerDefinition createBodyLayer() {
		return SoulTraderGeometry.createBodyLayer();
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(SoulTraderEntity e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		head.yRot += Mth.clamp(yaw * 0.7f, -50f, 50f) * Mth.DEG_TO_RAD;
		head.xRot += Mth.clamp(pitch * 0.5f, -22f, 22f) * Mth.DEG_TO_RAD;
		// парит и чуть колышется, руки сложены перед собой
		root.y += Mth.sin(age * 0.06f) * 0.6f;
		body.xRot += Mth.sin(age * 0.05f) * 0.03f;
		armL.xRot += Mth.sin(age * 0.05f) * 0.025f;
		armR.xRot += Mth.sin(age * 0.05f + 1f) * 0.025f;
		forearmL.xRot += Mth.sin(age * 0.05f + 0.5f) * 0.015f;
		forearmR.xRot += Mth.sin(age * 0.05f + 1.5f) * 0.015f;
	}
}
