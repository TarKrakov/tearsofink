package com.tarkrakov.tearsofink.client.entity;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.entity.MorokEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.animation.*;
import org.joml.Vector3f;
public final class MorokModel extends HierarchicalModel<MorokEntity>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(TearsOfInk.id("morok"),"main");
    private final ModelPart root;private final Vector3f scratch=new Vector3f();
    public MorokModel(ModelPart root){this.root=root;}
    @Override public ModelPart root(){return root;}
    public static LayerDefinition createBodyLayer(){
        MeshDefinition mesh=new MeshDefinition();
        // Authored 8x8x8 geometry and UV; translate export pivot to the entity hitbox.
        mesh.getRoot().addOrReplaceChild("main",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8,new CubeDeformation(0)),PartPose.offset(0,19,0));
        return LayerDefinition.create(mesh,32,32);
    }
    @Override public void setupAnim(MorokEntity e,float limb,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        var clip=switch(e.phase()){case MorokEntity.CHARGE->MorokAnimations.charge;case MorokEntity.DETONATE->MorokAnimations.detonate;default->MorokAnimations.Idle;};
        float elapsed=e.phase()==MorokEntity.DETONATE?16-e.fuseLeft()+Math.max(0f,age-e.tickCount):Math.max(0L,e.level().getGameTime()-e.phaseStart())+Math.max(0f,age-e.tickCount);
        KeyframeAnimations.animate(this,clip,(long)(elapsed*50f),1f,scratch);
    }
}
