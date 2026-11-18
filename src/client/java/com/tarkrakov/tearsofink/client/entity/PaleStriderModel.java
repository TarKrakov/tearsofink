package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleStriderEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Authored geometry; the animation export is not yet present in the accessible main. */
public final class PaleStriderModel extends HierarchicalModel<PaleStriderEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_strider"), "main");
    private final ModelPart root;
    public PaleStriderModel(ModelPart root) { this.root = root; }
    @Override public ModelPart root() { return root; }

    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition pale_strider_main = partdefinition.addOrReplaceChild("pale_strider_main", CubeListBuilder.create(), PartPose.offset(0.0F, -76.0F, 4.0F));

		PartDefinition pale_strider_body = pale_strider_main.addOrReplaceChild("pale_strider_body", CubeListBuilder.create(), PartPose.offset(1.0F, 2.0F, 0.0F));

		PartDefinition cube_r1 = pale_strider_body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(78, 68).addBox(-4.0F, -24.0F, -6.0F, 9.0F, 24.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 3.0F, 4.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition pale_strider_leg_l = pale_strider_main.addOrReplaceChild("pale_strider_leg_l", CubeListBuilder.create(), PartPose.offset(7.0F, 12.0F, 0.0F));

		PartDefinition cube_r2 = pale_strider_leg_l.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(68, 0).addBox(0.0F, -6.5387F, -6.5913F, 4.0F, 18.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -8.0F, 3.0F, -0.4363F, 0.0F, 0.0F));

		PartDefinition cube_r3 = pale_strider_leg_l.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(78, 99).addBox(-1.0F, -4.4991F, -0.2381F, 2.0F, 19.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, -6.0F, -0.4363F, 0.0F, 0.0F));

		PartDefinition pale_strider_leg_r = pale_strider_main.addOrReplaceChild("pale_strider_leg_r", CubeListBuilder.create(), PartPose.offset(-5.0F, 12.0F, 0.0F));

		PartDefinition cube_r4 = pale_strider_leg_r.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(40, 68).addBox(-4.0F, -6.5387F, -6.5913F, 4.0F, 18.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -8.0F, 3.0F, -0.4363F, 0.0F, 0.0F));

		PartDefinition cube_r5 = pale_strider_leg_r.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(98, 99).addBox(-1.0F, -4.4991F, -0.2381F, 2.0F, 19.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 1.0F, -6.0F, -0.4363F, 0.0F, 0.0F));

		PartDefinition pale_strider_leg_l_seg = pale_strider_main.addOrReplaceChild("pale_strider_leg_l_seg", CubeListBuilder.create(), PartPose.offset(7.0F, 32.0F, -10.0F));

		PartDefinition cube_r6 = pale_strider_leg_l_seg.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 35).addBox(-1.0F, -0.3872F, -2.9646F, 2.0F, 69.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition pale_strider_leg_r_seg = pale_strider_main.addOrReplaceChild("pale_strider_leg_r_seg", CubeListBuilder.create(), PartPose.offset(-6.0F, 32.0F, -10.0F));

		PartDefinition cube_r7 = pale_strider_leg_r_seg.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(20, 35).addBox(-1.0F, -0.3872F, -2.9646F, 2.0F, 69.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0873F, 0.0F, 0.0F));

		PartDefinition pale_strider_head = pale_strider_main.addOrReplaceChild("pale_strider_head", CubeListBuilder.create(), PartPose.offset(0.0F, -16.0F, -23.0F));

		PartDefinition cube_r8 = pale_strider_head.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -6.6194F, -24.0866F, 11.0F, 12.0F, 23.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition pale_strider_neck = pale_strider_main.addOrReplaceChild("pale_strider_neck", CubeListBuilder.create(), PartPose.offset(1.0F, -20.0F, 1.0F));

		PartDefinition cube_r9 = pale_strider_neck.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(40, 35).addBox(2.0F, -3.0F, -25.0F, 5.0F, 7.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, -2.0F, 0.0873F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}


    @Override public void setupAnim(PaleStriderEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        // No invented animation: integrate the author's ready tracks when available.
    }
}
