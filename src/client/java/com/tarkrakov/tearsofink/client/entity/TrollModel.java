package com.tarkrakov.tearsofink.client.entity;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.TrollEntity;
import net.minecraft.client.animation.*;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import java.util.List;
public final class TrollModel extends HierarchicalModel<TrollEntity>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(TearsOfInk.id("troll"),"main");
    private final ModelPart root;
    private final List<ModelPart> parts;
    private final PartPose[] previous;
    private final Vector3f scratch=new Vector3f();
    public TrollModel(ModelPart root){this.root=root;parts=root.getAllParts().toList();previous=new PartPose[parts.size()];}
    @Override public ModelPart root(){return root;}
    public static LayerDefinition createBodyLayer(){return TrollGeometry.createBodyLayer();}
    private AnimationDefinition clip(int c){return switch(c){
        case TrollEntity.WALK->TrollAnimations.walk;
        case TrollEntity.RIGHT->TrollAnimations.attack_punch_right;
        case TrollEntity.LEFT->TrollAnimations.attack_punch_left;
        case TrollEntity.BELLY->TrollAnimations.attack_belly;
        case TrollEntity.SLAM->TrollAnimations.attack_slam;
        case TrollEntity.SCRATCH->TrollAnimations.scratch_belly;
        case TrollEntity.SIT->TrollAnimations.sit_down;
        case TrollEntity.SLEEP->TrollAnimations.sleep;
        case TrollEntity.STAND->TrollAnimations.stand_up;
        case TrollEntity.DEATH->TrollAnimations.death;
        default->TrollAnimations.idle;
    };}
    private void apply(int c,float t){parts.forEach(ModelPart::resetPose);KeyframeAnimations.animate(this,clip(c),(long)(Math.max(0,t)*50),1,scratch);}
    @Override public void setupAnim(TrollEntity e,float limb,float amount,float age,float yaw,float pitch){
        int c=e.clip();float elapsed=Math.max(0,e.level().getGameTime()-e.clipStart()+Math.max(0,age-e.tickCount));
        float mix=Mth.clamp(elapsed/3,0,1);
        if(mix<1){apply(e.previousClip(),e.previousTicks());for(int i=0;i<parts.size();i++)previous[i]=parts.get(i).storePose();}
        apply(c,e.animationTicks(c,elapsed,Math.max(0,age-e.tickCount)));
        if(mix<1)for(int i=0;i<parts.size();i++){ModelPart p=parts.get(i);PartPose old=previous[i];p.setPos(Mth.lerp(mix,old.x,p.x),Mth.lerp(mix,old.y,p.y),Mth.lerp(mix,old.z,p.z));p.setRotation(angle(mix,old.xRot,p.xRot),angle(mix,old.yRot,p.yRot),angle(mix,old.zRot,p.zRot));}
    }
    private static float angle(float t,float a,float b){return a+t*Mth.wrapDegrees((b-a)*Mth.RAD_TO_DEG)*Mth.DEG_TO_RAD;}
}
