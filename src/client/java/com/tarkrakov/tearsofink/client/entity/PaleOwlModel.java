package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleOwlEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Author geometry and keyframes; no procedural head rotation on the solid body. */
public final class PaleOwlModel extends HierarchicalModel<PaleOwlEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_owl"), "main");
    private final ModelPart root;
    private final Vector3f animationScratch = new Vector3f();
    public PaleOwlModel(ModelPart root) { this.root = root; }
    @Override public ModelPart root() { return root; }

    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Owl_main = partdefinition.addOrReplaceChild("Owl_main", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 17.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition owl_body = Owl_main.addOrReplaceChild("owl_body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.7667F, -7.8333F, 9.0F, 12.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(32, 29).addBox(-1.0F, -1.8667F, 1.1667F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-0.5F, 0.1333F, 1.1667F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -1.2333F, 2.8333F));

		PartDefinition Owl_wing_l = Owl_main.addOrReplaceChild("Owl_wing_l", CubeListBuilder.create(), PartPose.offset(-4.0F, -2.0F, -0.5F));

		PartDefinition cube_r1 = Owl_wing_l.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(16, 21).addBox(1.0F, -2.0F, -3.5F, 1.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		PartDefinition Owl_wing_r = Owl_main.addOrReplaceChild("Owl_wing_r", CubeListBuilder.create().texOffs(0, 21).addBox(1.0F, -2.0F, -3.5F, 1.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -2.0F, -0.5F));

		PartDefinition Owl_leg_l = Owl_main.addOrReplaceChild("Owl_leg_l", CubeListBuilder.create().texOffs(32, 25).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 32).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 7.0F, 0.0F));

		PartDefinition Owl_leg_r = Owl_main.addOrReplaceChild("Owl_leg_r", CubeListBuilder.create().texOffs(32, 21).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 35).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 7.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}


    private static AnimationDefinition clip(PaleOwlEntity.Phase phase) {
        return switch (phase) {
            case IDLE -> PaleOwlAnimations.IDLE;
            case SWAY -> PaleOwlAnimations.IDLE_SWAY;
            case TAKEOFF -> PaleOwlAnimations.TAKEOFF;
            case FLY -> PaleOwlAnimations.FLY;
            case DIVE -> PaleOwlAnimations.DIVE;
            case LAND -> PaleOwlAnimations.LAND;
        };
    }

    @Override
    public void setupAnim(PaleOwlEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long current = entity.phaseStamp();
        long previous = entity.previousPhaseStamp();
        float partial = Mth.clamp(ageInTicks - entity.tickCount, 0.0f, 1.0f);
        float elapsed = current == 0L ? ageInTicks :
                Math.max(0L, entity.level().getGameTime() - PaleOwlEntity.startTick(current)) + partial;
        // Sample the old pose at the transition, not a cache shared between owls.
        float weight = previous == current || previous == 0L || current == 0L ? 1.0f : Mth.clamp(elapsed / 4.0f, 0.0f, 1.0f);
        weight = weight * weight * (3.0f - 2.0f * weight);
        if (weight < 1.0f) {
            long previousMs = Math.max(0L, PaleOwlEntity.startTick(current) - PaleOwlEntity.startTick(previous)) * 50L;
            KeyframeAnimations.animate(this, clip(PaleOwlEntity.phaseOf(previous)), previousMs, 1.0f - weight, animationScratch);
        }
        KeyframeAnimations.animate(this, clip(PaleOwlEntity.phaseOf(current)), (long)(elapsed * 50.0f), weight, animationScratch);
    }
}
