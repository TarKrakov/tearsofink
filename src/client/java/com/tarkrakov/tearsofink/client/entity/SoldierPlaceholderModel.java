package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.patrol.PaleCrossbowmanEntity;
import com.tarkrakov.tearsofink.entity.patrol.PaleShieldbearerEntity;
import com.tarkrakov.tearsofink.entity.patrol.PaleSoldier;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * ЗАГЛУШКА-фигурка щитовика и арбалетчика (гуманоид + щит или арбалет), позы кодом по действию.
 * Будет заменена моделями и анимациями автора — как у копейщика.
 */
public class SoldierPlaceholderModel<T extends PaleSoldier> extends HierarchicalModel<T> {
	public static final ModelLayerLocation SHIELD_LAYER = new ModelLayerLocation(TearsOfInk.id("pale_shieldbearer_placeholder"), "main");
	public static final ModelLayerLocation CROSSBOW_LAYER = new ModelLayerLocation(TearsOfInk.id("pale_crossbowman_placeholder"), "main");

	private final ModelPart root, head, body, armR, armL, legR, legL;

	public SoldierPlaceholderModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.armR = root.getChild("arm_r");
		this.armL = root.getChild("arm_l");
		this.legR = root.getChild("leg_r");
		this.legL = root.getChild("leg_l");
	}

	private static PartDefinition humanoid(MeshDefinition mesh) {
		PartDefinition r = mesh.getRoot();
		r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 0, 0));
		r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.offset(0, 0, 0));
		r.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-1.9f, 12, 0));
		r.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(1.9f, 12, 0));
		return r;
	}

	public static LayerDefinition createShield() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition r = humanoid(mesh);
		r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
		PartDefinition armL = r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
		// щит: доска перед левой рукой
		armL.addOrReplaceChild("shield", CubeListBuilder.create().texOffs(0, 32).addBox(-5, -6, -1, 12, 18, 1), PartPose.offset(1, 4, -3));
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static LayerDefinition createCrossbow() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition r = humanoid(mesh);
		PartDefinition armR = r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
		r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
		// арбалет: ложе вдоль руки + плечи-дуга
		armR.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5f, 0, -7, 1, 1, 9)
				.texOffs(0, 44).addBox(-4.5f, 0, -6, 9, 1, 1), PartPose.offset(-1, 9, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
		head.xRot = headPitch * Mth.DEG_TO_RAD;
		float swing = Mth.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount;
		legR.xRot = swing;
		legL.xRot = -swing;
		armR.xRot = -swing * 0.6f;
		armL.xRot = swing * 0.6f;
		int act = entity.action();

		if (entity instanceof PaleShieldbearerEntity) {
			switch (act) {
				case PaleShieldbearerEntity.ACT_BLOCK, PaleShieldbearerEntity.ACT_CHARGE_WINDUP, PaleShieldbearerEntity.ACT_CHARGE -> {
					armL.xRot = -1.35f;
					armL.yRot = 0.4f;
					if (act == PaleShieldbearerEntity.ACT_CHARGE) body.xRot = 0.3f;
				}
				case PaleShieldbearerEntity.ACT_BASH -> {
					armL.xRot = -1.6f;
					armL.z = -4;
				}
				case PaleShieldbearerEntity.ACT_STUN -> {
					head.xRot = 0.6f;
					armL.xRot = 0.2f;
					armR.xRot = 0.2f;
					head.zRot = Mth.sin(ageInTicks * 0.5f) * 0.3f;
				}
				default -> { }
			}
		} else if (entity instanceof PaleCrossbowmanEntity) {
			switch (act) {
				case PaleCrossbowmanEntity.ACT_AIM, PaleCrossbowmanEntity.ACT_SHOOT -> {
					armR.xRot = -1.5f + head.xRot;
					armR.yRot = -0.1f + head.yRot;
					armL.xRot = -1.5f + head.xRot;
					armL.yRot = 0.5f + head.yRot;
					if (act == PaleCrossbowmanEntity.ACT_SHOOT) armR.xRot -= 0.25f;
				}
				case PaleCrossbowmanEntity.ACT_RELOAD -> {
					armR.xRot = -0.6f;
					armL.xRot = -0.9f + Mth.sin(ageInTicks * 0.8f) * 0.3f;
				}
				case PaleCrossbowmanEntity.ACT_DODGE -> {
					body.xRot = -0.3f;
					legR.xRot = -0.8f;
					legL.xRot = -0.4f;
				}
				default -> { }
			}
		}
	}
}
