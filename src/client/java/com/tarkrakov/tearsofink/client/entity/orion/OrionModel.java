package com.tarkrakov.tearsofink.client.entity.orion;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.orion.OrionCombat.Clip;
import com.tarkrakov.tearsofink.entity.orion.OrionEntity;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.List;

public final class OrionModel extends HierarchicalModel<OrionEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TearsOfInk.id("orion"), "main");
    private final ModelPart root;
    private final List<ModelPart> parts;
    private final Vector3f scratch = new Vector3f();
    private final PartPose[] former;
    private final float[][] scale;

    public OrionModel(ModelPart root) {
        this.root = root;
        parts = root.getAllParts().toList();
        former = new PartPose[parts.size()];
        scale = new float[parts.size()][3];
    }

    public static LayerDefinition createBodyLayer() { return OrionGeometry.createBodyLayer(); }
    @Override public ModelPart root() { return root; }

    @Override public void setupAnim(OrionEntity entity, float limbSwing, float limbAmount, float age,
                                    float headYaw, float headPitch) {
        var state = entity.performance();
        long now = entity.level().getGameTime();
        float partial = age - entity.tickCount;
        float mix = Mth.clamp(state.elapsedTicks(now, partial) / 3, 0, 1);
        if (mix < 1) {
            apply(state.previous(), state.previousSeconds(state.start()));
            for (int i = 0; i < parts.size(); i++) {
                ModelPart part = parts.get(i);
                former[i] = part.storePose();
                scale[i][0] = part.xScale;
                scale[i][1] = part.yScale;
                scale[i][2] = part.zScale;
            }
        }
        apply(state.clip(), state.seconds(now, partial));
        if (mix >= 1) return;
        for (int i = 0; i < parts.size(); i++) {
            ModelPart part = parts.get(i);
            PartPose previous = former[i];
            part.setPos(Mth.lerp(mix, previous.x, part.x), Mth.lerp(mix, previous.y, part.y), Mth.lerp(mix, previous.z, part.z));
            part.setRotation(blendAngle(mix, previous.xRot, part.xRot), blendAngle(mix, previous.yRot, part.yRot),
                    blendAngle(mix, previous.zRot, part.zRot));
            part.xScale = Mth.lerp(mix, scale[i][0], part.xScale);
            part.yScale = Mth.lerp(mix, scale[i][1], part.yScale);
            part.zScale = Mth.lerp(mix, scale[i][2], part.zScale);
        }
    }

    private void apply(Clip clip, float time) {
        for (ModelPart part : parts) part.resetPose();
        AnimationDefinition animation = switch (clip) {
            case IDLE -> OrionAnimations.idle;
            case WALK -> OrionAnimations.walk;
            case RUN -> OrionAnimations.run;
            case SLASH_COMBO -> OrionAnimations.slash_combo;
            case SPIN_DASH -> OrionAnimations.spin_dash;
            case CLEAVE -> OrionAnimations.cleave;
            case DODGE_BACK -> OrionAnimations.dodge_back;
            case DODGE_RIGHT -> OrionAnimations.dodge_right;
            case DODGE_LEFT -> OrionAnimations.dodge_left;
            case EMPOWER -> OrionAnimations.empower;
            case DEATH -> OrionAnimations.death;
        };
        KeyframeAnimations.animate(this, animation, (long)(time * 1000), 1, scratch);
    }

    private static float blendAngle(float weight, float from, float to) {
        return from + weight * Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }
}
