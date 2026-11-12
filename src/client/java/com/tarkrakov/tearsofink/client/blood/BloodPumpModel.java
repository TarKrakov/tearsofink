package com.tarkrakov.tearsofink.client.blood;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * Модель помпы Кровосборника — автора (blockbench/blood_pump.bbmodel → blood_pump_model.java).
 * Кости: base (корпус, носик, трубы), piston (шток), roof (крышка) — piston и roof ходят при ударе.
 */
public class BloodPumpModel extends HierarchicalModel<Entity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("blood_pump"), "main");

	private final ModelPart root;

	public BloodPumpModel(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition roof = partdefinition.addOrReplaceChild("roof", CubeListBuilder.create().texOffs(0, 32).addBox(-8.0F, -4.0F, -8.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, 0.0F));

		PartDefinition piston = partdefinition.addOrReplaceChild("piston", CubeListBuilder.create().texOffs(32, 52).addBox(-2.0F, -18.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, 0.0F));

		PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F))
		.texOffs(0, 52).addBox(-4.0F, -14.0F, -16.0F, 8.0F, 14.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(48, 52).addBox(-11.0F, -14.0F, -1.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F))
		.texOffs(64, 0).addBox(-11.0F, -14.0F, -6.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F))
		.texOffs(56, 52).addBox(-11.0F, -14.0F, 4.0F, 2.0F, 13.0F, 2.0F, new CubeDeformation(1.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
