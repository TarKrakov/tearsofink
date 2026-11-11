package com.tarkrakov.tearsofink.client.blood;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * ЗАГЛУШКА модели помпы Кровосборника: корпус (`base`) + ходящий поршень (`piston`).
 * Будет заменена моделью автора из Blockbench (Modded Entity, Mojmaps) — как у гусеницы.
 */
public class BloodPumpModel extends HierarchicalModel<Entity> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("blood_pump"), "main");

	private final ModelPart root;

	public BloodPumpModel(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("base", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6f, -18f, -6f, 12, 18, 12)      // корпус
				.texOffs(0, 30).addBox(-7f, -20f, -7f, 14, 2, 14),     // крышка
				PartPose.offset(0f, 24f, 0f));
		root.addOrReplaceChild("piston", CubeListBuilder.create()
				.texOffs(48, 0).addBox(-2f, -12f, -2f, 4, 12, 4),
				PartPose.offset(0f, 4f, 0f));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}
