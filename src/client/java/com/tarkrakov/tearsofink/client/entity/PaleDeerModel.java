package com.tarkrakov.tearsofink.client.entity;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.PaleDeerEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Author geometry from 6f33623; original pivots, UV and scale. */
public final class PaleDeerModel extends HierarchicalModel<PaleDeerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("pale_deer"), "main");
    private final ModelPart root;
    private final Vector3f scratch = new Vector3f();
    public PaleDeerModel(ModelPart root) { this.root=root; }
    @Override public ModelPart root() { return root; }

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition pale_deer_main = partdefinition.addOrReplaceChild("pale_deer_main", CubeListBuilder.create(), PartPose.offset(-0.2857F, 10.0F, -3.7857F));

		PartDefinition pale_deer_neck = pale_deer_main.addOrReplaceChild("pale_deer_neck", CubeListBuilder.create().texOffs(42, 33).addBox(-4.4667F, 1.6833F, -1.3333F, 9.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7476F, -28.6833F, -15.881F));

		PartDefinition pale_deer_head = pale_deer_neck.addOrReplaceChild("pale_deer_head", CubeListBuilder.create().texOffs(42, 50).addBox(-3.5F, -1.875F, -4.0F, 7.0F, 11.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0333F, -4.4417F, -0.3333F));

		PartDefinition cube_r1 = pale_deer_head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(8, 73).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, 4.375F, -2.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r2 = pale_deer_head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 73).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 4.375F, -2.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r3 = pale_deer_head.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(72, 72).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.5F, -9.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r4 = pale_deer_head.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(72, 67).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.5F, -7.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r5 = pale_deer_head.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(72, 30).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(12.5F, -5.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r6 = pale_deer_head.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(68, 56).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.5F, -5.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r7 = pale_deer_head.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(72, 41).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-15.0F, -7.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r8 = pale_deer_head.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(72, 46).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.5F, -9.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r9 = pale_deer_head.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(72, 36).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 1.375F, 0.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r10 = pale_deer_head.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(68, 62).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 1.375F, 0.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r11 = pale_deer_head.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(68, 50).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.5F, -1.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r12 = pale_deer_head.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(68, 24).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, -1.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r13 = pale_deer_head.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(68, 8).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(8.5F, -3.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition cube_r14 = pale_deer_head.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(68, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.5F, -3.625F, -1.5F, 1.5708F, 0.0F, 1.5708F));

		PartDefinition pale_deer_face = pale_deer_head.addOrReplaceChild("pale_deer_face", CubeListBuilder.create(), PartPose.offset(0.0F, 1.875F, -8.0F));

		PartDefinition cube_r15 = pale_deer_face.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 55).addBox(-2.5F, -5.5F, -3.5F, 5.0F, 11.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, 0.0F, 0.0F));

		PartDefinition pale_deer_leg_l = pale_deer_main.addOrReplaceChild("pale_deer_leg_l", CubeListBuilder.create().texOffs(48, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7857F, 1.0F, -9.7143F));

		PartDefinition pale_deer_leg_l2 = pale_deer_main.addOrReplaceChild("pale_deer_leg_l2", CubeListBuilder.create().texOffs(24, 55).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.7857F, 1.0F, 9.2857F));

		PartDefinition pale_deer_leg_r = pale_deer_main.addOrReplaceChild("pale_deer_leg_r", CubeListBuilder.create().texOffs(60, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.2143F, 1.0F, -9.7143F));

		PartDefinition pale_deer_leg_r2 = pale_deer_main.addOrReplaceChild("pale_deer_leg_r2", CubeListBuilder.create().texOffs(36, 67).addBox(-1.5F, -13.0F, -1.5F, 3.0F, 26.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.2143F, 1.0F, 9.2857F));

		PartDefinition pale_deer_body = pale_deer_main.addOrReplaceChild("pale_deer_body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -7.0F, -11.25F, 12.0F, 11.0F, 22.0F, new CubeDeformation(0.0F))
		.texOffs(0, 33).addBox(-4.0F, -3.0F, -6.25F, 8.0F, 9.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7143F, -17.0F, 0.0357F));

		PartDefinition pale_deer_tail = pale_deer_main.addOrReplaceChild("pale_deer_tail", CubeListBuilder.create().texOffs(68, 16).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.7143F, -20.5F, 12.7857F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}


    private static AnimationDefinition clip(PaleDeerEntity.Phase phase) {
        return switch (phase) {
            case IDLE -> PaleDeerAnimations.IDLE;
            case WALK -> PaleDeerAnimations.WALK;
            case EATING -> PaleDeerAnimations.EATING;
            case RUN -> PaleDeerAnimations.RUN;
            case WATCH_START -> PaleDeerAnimations.WATCH_START;
            case WATCH -> PaleDeerAnimations.WATCH;
            case WATCH_ROAR -> PaleDeerAnimations.WATCH_ROAR;
            case WATCH_END -> PaleDeerAnimations.WATCH_END;
            case ROAR -> PaleDeerAnimations.ROAR;
        };
    }
    private static long sampleTime(PaleDeerEntity entity, PaleDeerEntity.Phase phase,float ticks) {
        if (phase==PaleDeerEntity.Phase.RUN) return (long)(ticks*50*entity.individualTempo())+entity.runOffsetMs();
        if (phase==PaleDeerEntity.Phase.WALK) return (long)(ticks*50*entity.individualTempo())+entity.runOffsetMs();
        if (phase==PaleDeerEntity.Phase.IDLE) return (long)(ticks*50)+4L*entity.runOffsetMs();
        return (long)(ticks*50);
    }
    @Override public void setupAnim(PaleDeerEntity entity,float limbSwing,float limbSwingAmount,
                                  float ageInTicks,float netHeadYaw,float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        long current=entity.phaseStamp(), previous=entity.previousPhaseStamp();
        float partial=Mth.clamp(ageInTicks-entity.tickCount,0f,1f);
        float elapsed=current==0L?ageInTicks:Math.max(0L,entity.level().getGameTime()-PaleDeerEntity.startTick(current))+partial;
        PaleDeerEntity.Phase phase=PaleDeerEntity.phaseOf(current);
        float weight=previous==0L||previous==current||current==0L?1f:Mth.clamp(elapsed/4f,0f,1f);
        weight=weight*weight*(3f-2f*weight);
        if (weight<1f) {
            PaleDeerEntity.Phase prev=PaleDeerEntity.phaseOf(previous);
            float oldAge=Math.max(0L,PaleDeerEntity.startTick(current)-PaleDeerEntity.startTick(previous));
            KeyframeAnimations.animate(this,clip(prev),sampleTime(entity,prev,oldAge),1f-weight,scratch);
        }
        KeyframeAnimations.animate(this,clip(phase),sampleTime(entity,phase,elapsed),weight,scratch);
    }
}
