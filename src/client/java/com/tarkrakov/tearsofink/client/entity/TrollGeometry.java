package com.tarkrakov.tearsofink.client.entity;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Generated from blockbench/troll.bbmodel. Original UV, cuboids and hierarchy. */
final class TrollGeometry {
static LayerDefinition createBodyLayer(){
MeshDefinition mesh=new MeshDefinition();
PartDefinition root=mesh.getRoot();
PartDefinition main=root.addOrReplaceChild("main",CubeListBuilder.create(),PartPose.offsetAndRotation(2.5f,24f,0f,0f,0f,0f));
PartDefinition head_with_neck=main.addOrReplaceChild("head_with_neck",CubeListBuilder.create(),PartPose.offsetAndRotation(-2.5f,-56f,5f,0f,0f,0f));
PartDefinition head_only=head_with_neck.addOrReplaceChild("head_only",CubeListBuilder.create(),PartPose.offsetAndRotation(-0.5f,-4f,-3f,0f,0f,0f));
head_only.addOrReplaceChild("cube_1",CubeListBuilder.create().texOffs(0,83).mirror(false).addBox(-7.5f,-8.5f,-9.5f,15f,15f,16f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_2",CubeListBuilder.create().texOffs(40,114).mirror(false).addBox(-1.5f,-5.5f,-12.5f,3f,7f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_3",CubeListBuilder.create().texOffs(52,114).mirror(false).addBox(-1.5f,-3.5f,-14.5f,3f,6f,2f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_4",CubeListBuilder.create().texOffs(54,122).mirror(false).addBox(-1.5f,-2.5f,-15.5f,3f,7f,1f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_5",CubeListBuilder.create().texOffs(124,102).mirror(false).addBox(-4.5f,-1.5f,-12.5f,3f,3f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_6",CubeListBuilder.create().texOffs(132,41).mirror(false).addBox(1.5f,-1.5f,-12.5f,3f,3f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_7",CubeListBuilder.create().texOffs(40,124).mirror(false).addBox(1.5f,-11.5f,-1.5f,4f,3f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_8",CubeListBuilder.create().texOffs(132,47).mirror(false).addBox(-10.5f,1.5f,-1.5f,3f,3f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_9",CubeListBuilder.create().texOffs(136,102).mirror(false).addBox(7.5f,0.5f,1.5f,3f,3f,3f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_10",CubeListBuilder.create().texOffs(94,81).mirror(false).addBox(7.5f,1.5f,-7.5f,1f,1f,1f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_11",CubeListBuilder.create().texOffs(98,81).mirror(false).addBox(-8.5f,-2.5f,-4.5f,1f,1f,1f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_12",CubeListBuilder.create().texOffs(86,78).mirror(false).addBox(-9.5f,-1.5f,1.5f,2f,2f,2f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_only.addOrReplaceChild("cube_13",CubeListBuilder.create().texOffs(94,78).mirror(false).addBox(-3.5f,-9.5f,-5.5f,2f,1f,2f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-5.5f,1.5f,0.17453f,0f,0f));
head_with_neck.addOrReplaceChild("cube_14",CubeListBuilder.create().texOffs(112,113).mirror(false).addBox(-5.5f,5.5f,-5.5f,12f,9f,10f,new CubeDeformation(0f)),PartPose.offsetAndRotation(-0.5f,-9.5f,-1.5f,0.17453f,0f,0f));
PartDefinition body_main=main.addOrReplaceChild("body_main",CubeListBuilder.create(),PartPose.offsetAndRotation(-2.5f,-34.4f,0f,0f,0f,0f));
PartDefinition body_fat1=body_main.addOrReplaceChild("body_fat1",CubeListBuilder.create(),PartPose.offsetAndRotation(0f,-0.6f,-14f,0f,0f,0f));
body_fat1.addOrReplaceChild("cube_15",CubeListBuilder.create().texOffs(62,83).mirror(false).addBox(-11.5f,-11f,-4f,23f,22f,8f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,-3f,1f,0f,0f,0f));
PartDefinition body_fat2=body_fat1.addOrReplaceChild("body_fat2",CubeListBuilder.create(),PartPose.offsetAndRotation(-0.5f,4f,-3f,0f,0f,0f));
body_fat2.addOrReplaceChild("cube_16",CubeListBuilder.create().texOffs(0,114).mirror(false).addBox(-8f,-7.5f,-2f,16f,15f,4f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0.5f,-1.5f,1f,0f,0f,0f));
body_fat2.addOrReplaceChild("cube_17",CubeListBuilder.create().texOffs(112,41).mirror(false).addBox(-7f,3.5f,-3f,4f,6f,6f,new CubeDeformation(0f)),PartPose.offsetAndRotation(1.5f,-1.5f,0.75f,0f,0f,0f));
body_main.addOrReplaceChild("cube_18",CubeListBuilder.create().texOffs(0,0).mirror(false).addBox(-13.5f,-13f,-14f,28f,26f,28f,new CubeDeformation(0f)),PartPose.offsetAndRotation(-0.5f,-7.6f,2f,0f,0f,0f));
body_main.addOrReplaceChild("cube_19",CubeListBuilder.create().texOffs(0,54).mirror(false).addBox(-11f,-4f,-10.5f,22f,8f,21f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,9.4f,3.5f,0f,0f,0f));
body_main.addOrReplaceChild("cube_20",CubeListBuilder.create().texOffs(112,24).mirror(false).addBox(-12f,-8f,-1f,24f,16f,1f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,13.4f,-7.25f,0f,0f,0f));
body_main.addOrReplaceChild("cube_21",CubeListBuilder.create().texOffs(62,113).mirror(false).addBox(-12f,-8f,0f,24f,16f,1f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,13.4f,14.25f,0f,0f,0f));
PartDefinition leg_l=main.addOrReplaceChild("leg_l",CubeListBuilder.create(),PartPose.offsetAndRotation(11.5f,-31f,0f,0f,0f,0f));
leg_l.addOrReplaceChild("cube_22",CubeListBuilder.create().texOffs(0,133).mirror(false).addBox(-4f,-7.5f,-4.5f,8f,12f,9f,new CubeDeformation(0f)),PartPose.offsetAndRotation(4.5f,8.5f,2.5f,-0.6545f,0f,0f));
PartDefinition leg_l_1=leg_l.addOrReplaceChild("leg_l_1",CubeListBuilder.create(),PartPose.offsetAndRotation(4.5f,15f,-2f,0f,0f,0f));
leg_l_1.addOrReplaceChild("cube_23",CubeListBuilder.create().texOffs(124,78).mirror(false).addBox(-4f,0.63733f,-4.71496f,8f,15f,9f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,0f,0f,0.04363f,0f,0f));
PartDefinition leg_r=main.addOrReplaceChild("leg_r",CubeListBuilder.create(),PartPose.offsetAndRotation(-16.5f,-31f,0f,0f,0f,0f));
leg_r.addOrReplaceChild("cube_24",CubeListBuilder.create().texOffs(134,132).mirror(false).addBox(-4f,-7.5f,-4.5f,8f,12f,9f,new CubeDeformation(0f)),PartPose.offsetAndRotation(-4.5f,8.5f,2.5f,-0.6545f,0f,0f));
PartDefinition leg_r_1=leg_r.addOrReplaceChild("leg_r_1",CubeListBuilder.create(),PartPose.offsetAndRotation(-4.5f,15f,-2f,0f,0f,0f));
leg_r_1.addOrReplaceChild("cube_25",CubeListBuilder.create().texOffs(40,130).mirror(false).addBox(-4f,-4.5f,-4.5f,8f,15f,9f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,5.14181f,0.00933f,0.04363f,0f,0f));
PartDefinition hand_r=main.addOrReplaceChild("hand_r",CubeListBuilder.create(),PartPose.offsetAndRotation(-19f,-50f,2f,0f,0f,0f));
hand_r.addOrReplaceChild("cube_26",CubeListBuilder.create().texOffs(86,54).mirror(false).addBox(-34.5f,-13f,-6f,18f,11f,13f,new CubeDeformation(0f)),PartPose.offsetAndRotation(16f,8f,0f,0f,0f,0f));
hand_r.addOrReplaceChild("cube_27",CubeListBuilder.create().texOffs(148,41).mirror(false).addBox(-4.5f,-7.5f,-3f,9f,13f,6f,new CubeDeformation(0f)),PartPose.offsetAndRotation(-12f,14.5f,3f,0.47997f,0f,0f));
PartDefinition hand_r_1=hand_r.addOrReplaceChild("hand_r_1",CubeListBuilder.create(),PartPose.offsetAndRotation(-12f,25f,4f,0f,0f,0f));
hand_r_1.addOrReplaceChild("cube_28",CubeListBuilder.create().texOffs(74,130).mirror(false).addBox(-4.5f,-5.5f,-3f,9f,20f,6f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,2.5f,-5f,-1.13446f,0f,0f));
PartDefinition hand_l=main.addOrReplaceChild("hand_l",CubeListBuilder.create(),PartPose.offsetAndRotation(14f,-50f,2f,0f,0f,0f));
hand_l.addOrReplaceChild("cube_29",CubeListBuilder.create().texOffs(86,54).mirror(true).addBox(16.5f,-13f,-6f,18f,11f,13f,new CubeDeformation(0f)),PartPose.offsetAndRotation(-16f,8f,0f,0f,0f,0f));
hand_l.addOrReplaceChild("cube_30",CubeListBuilder.create().texOffs(148,41).mirror(true).addBox(-4.5f,-7.5f,-3f,9f,13f,6f,new CubeDeformation(0f)),PartPose.offsetAndRotation(12f,14.5f,3f,0.47997f,0f,0f));
PartDefinition hand_l_1=hand_l.addOrReplaceChild("hand_l_1",CubeListBuilder.create(),PartPose.offsetAndRotation(12f,25f,4f,0f,0f,0f));
hand_l_1.addOrReplaceChild("cube_31",CubeListBuilder.create().texOffs(74,130).mirror(true).addBox(-4.5f,-5.5f,-3f,9f,20f,6f,new CubeDeformation(0f)),PartPose.offsetAndRotation(0f,2.5f,-5f,-1.13446f,0f,0f));
return LayerDefinition.create(mesh,256,256);
}
}
