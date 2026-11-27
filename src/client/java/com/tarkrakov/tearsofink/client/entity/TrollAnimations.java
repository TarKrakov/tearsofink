package com.tarkrakov.tearsofink.client.entity;
import net.minecraft.client.animation.*;
/** Authored keyframes from troll.bbmodel; Bezier segments sampled at 50 Hz. */
final class TrollAnimations {
static final AnimationDefinition idle=idle();
static final AnimationDefinition walk=walk();
static final AnimationDefinition attack_punch_right=attack_punch_right();
static final AnimationDefinition attack_punch_left=attack_punch_left();
static final AnimationDefinition attack_belly=attack_belly();
static final AnimationDefinition attack_slam=attack_slam();
static final AnimationDefinition sit_down=sit_down();
static final AnimationDefinition stand_up=stand_up();
static final AnimationDefinition scratch_belly=scratch_belly();
static final AnimationDefinition sleep=sleep();
static final AnimationDefinition death=death();
private static AnimationChannel channel1(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel2(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel3(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel4(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,1f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel5(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel6(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,0.49952f,-0.02181f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel7(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel8(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0.49952f,-0.02181f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel9(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel10(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,1f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel11(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel12(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel13(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel14(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel15(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel16(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel17(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel18(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel19(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel20(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel21(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel22(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel23(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel24(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition idle(){
return AnimationDefinition.Builder.withLength(3f).looping()
.addAnimation("head_with_neck",channel1())
.addAnimation("head_with_neck",channel2())
.addAnimation("body_main",channel3())
.addAnimation("body_main",channel4())
.addAnimation("body_fat1",channel5())
.addAnimation("body_fat1",channel6())
.addAnimation("body_fat2",channel7())
.addAnimation("body_fat2",channel8())
.addAnimation("hand_r",channel9())
.addAnimation("hand_r",channel10())
.addAnimation("hand_r_1",channel11())
.addAnimation("hand_r_1",channel12())
.addAnimation("hand_l",channel13())
.addAnimation("hand_l",channel14())
.addAnimation("hand_l_1",channel15())
.addAnimation("hand_l_1",channel16())
.addAnimation("leg_l_1",channel17())
.addAnimation("leg_l_1",channel18())
.addAnimation("leg_l",channel19())
.addAnimation("leg_l",channel20())
.addAnimation("leg_r",channel21())
.addAnimation("leg_r",channel22())
.addAnimation("leg_r_1",channel23())
.addAnimation("leg_r_1",channel24())
.build();
}

private static AnimationChannel channel25(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel26(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel27(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel28(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel29(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel30(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel31(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel32(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.4981f,-0.04358f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.4981f,-0.04358f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel33(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel34(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel35(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel36(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,-0.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel37(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(22.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(22.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel38(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(35f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-15f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(35f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(-15f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(35f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel39(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(27.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(27.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(27.5f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel40(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(35f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(35f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition walk(){
return AnimationDefinition.Builder.withLength(4f).looping()
.addAnimation("head_with_neck",channel25())
.addAnimation("head_only",channel26())
.addAnimation("body_main",channel27())
.addAnimation("body_main",channel28())
.addAnimation("body_fat1",channel29())
.addAnimation("body_fat1",channel30())
.addAnimation("body_fat2",channel31())
.addAnimation("body_fat2",channel32())
.addAnimation("hand_r",channel33())
.addAnimation("hand_r",channel34())
.addAnimation("hand_l",channel35())
.addAnimation("hand_l",channel36())
.addAnimation("leg_r",channel37())
.addAnimation("leg_r_1",channel38())
.addAnimation("leg_l",channel39())
.addAnimation("leg_l_1",channel40())
.build();
}

private static AnimationChannel channel41(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel42(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel43(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(10.0284f,-4.9571f,-0.6542f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel44(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(7.5047f,-2.4905f,-0.218f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel45(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel46(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel47(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel48(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel49(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel50(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(40f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-69.6391f,-8.8605f,-4.6546f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel51(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel52(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-42.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(50f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel53(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel54(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,0f,-5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(0f,-10f,-5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel55(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel56(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-12.5f,25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel57(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel58(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel59(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel60(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel61(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel62(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel63(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel64(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel65(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition attack_punch_right(){
return AnimationDefinition.Builder.withLength(2f)
.addAnimation("head_with_neck",channel41())
.addAnimation("head_with_neck",channel42())
.addAnimation("head_only",channel43())
.addAnimation("body_main",channel44())
.addAnimation("body_main",channel45())
.addAnimation("body_fat1",channel46())
.addAnimation("body_fat1",channel47())
.addAnimation("body_fat2",channel48())
.addAnimation("body_fat2",channel49())
.addAnimation("hand_r",channel50())
.addAnimation("hand_r",channel51())
.addAnimation("hand_r_1",channel52())
.addAnimation("hand_r_1",channel53())
.addAnimation("hand_l",channel54())
.addAnimation("hand_l",channel55())
.addAnimation("hand_l_1",channel56())
.addAnimation("hand_l_1",channel57())
.addAnimation("leg_r",channel58())
.addAnimation("leg_r",channel59())
.addAnimation("leg_r_1",channel60())
.addAnimation("leg_r_1",channel61())
.addAnimation("leg_l",channel62())
.addAnimation("leg_l",channel63())
.addAnimation("leg_l_1",channel64())
.addAnimation("leg_l_1",channel65())
.build();
}

private static AnimationChannel channel66(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel67(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel68(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(10.0284f,4.9571f,0.6542f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel69(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(7.5047f,2.4905f,0.218f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel70(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel71(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel72(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel73(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel74(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel75(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,0f,5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(0f,10f,5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel76(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel77(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,-25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-12.5f,-25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel78(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel79(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(40f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-69.6391f,8.8605f,4.6546f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel80(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel81(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-42.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(50f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel82(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel83(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel84(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel85(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel86(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel87(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel88(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel89(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel90(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition attack_punch_left(){
return AnimationDefinition.Builder.withLength(2f)
.addAnimation("head_with_neck",channel66())
.addAnimation("head_with_neck",channel67())
.addAnimation("head_only",channel68())
.addAnimation("body_main",channel69())
.addAnimation("body_main",channel70())
.addAnimation("body_fat1",channel71())
.addAnimation("body_fat1",channel72())
.addAnimation("body_fat2",channel73())
.addAnimation("body_fat2",channel74())
.addAnimation("hand_r",channel75())
.addAnimation("hand_r",channel76())
.addAnimation("hand_r_1",channel77())
.addAnimation("hand_r_1",channel78())
.addAnimation("hand_l",channel79())
.addAnimation("hand_l",channel80())
.addAnimation("hand_l_1",channel81())
.addAnimation("hand_l_1",channel82())
.addAnimation("leg_r",channel83())
.addAnimation("leg_r",channel84())
.addAnimation("leg_r_1",channel85())
.addAnimation("leg_r_1",channel86())
.addAnimation("leg_l",channel87())
.addAnimation("leg_l",channel88())
.addAnimation("leg_l_1",channel89())
.addAnimation("leg_l_1",channel90())
.build();
}

private static AnimationChannel channel91(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.35f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-1.10648f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-4.79837f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-9.60401f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-13.29976f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-15.61131f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-16.89269f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-17.43894f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-17.34353f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-17.01843f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-16.61184f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-16.15679f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-15.67002f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-15.16147f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-14.63761f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-14.10295f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-13.56083f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-13.01379f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-12.4639f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-11.91284f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-11.362f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-10.81267f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-10.26587f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-9.72264f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-9.18387f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-8.65043f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-8.12311f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-7.60277f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-7.09017f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-6.58609f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-6.09132f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-5.60672f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-5.13313f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-4.67141f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-4.22255f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-3.78748f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-3.36733f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-2.96321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-2.57639f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-2.20826f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-1.86032f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-1.53426f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-1.23196f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-0.95556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-0.70749f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.4905f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.30784f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.16325f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.06122f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.0071f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel92(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.52293f,-5.97717f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,-0.52275f,-5.72444f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,-0.52213f,-4.88117f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,-0.52132f,-3.78352f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,-0.5207f,-2.93938f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,-0.52032f,-2.4114f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.posVec(0f,-0.5201f,-2.11871f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.posVec(0f,-0.52001f,-1.99395f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.posVec(0f,-0.52f,-1.98f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.posVec(0f,-0.51535f,-1.9623f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.posVec(0f,-0.50569f,-1.92551f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.posVec(0f,-0.49361f,-1.87951f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.posVec(0f,-0.48009f,-1.82803f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.posVec(0f,-0.46562f,-1.77295f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.posVec(0f,-0.45051f,-1.71541f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.posVec(0f,-0.43495f,-1.65614f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.posVec(0f,-0.41906f,-1.59565f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.posVec(0f,-0.40295f,-1.53431f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.posVec(0f,-0.3867f,-1.47242f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.posVec(0f,-0.37036f,-1.4102f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.posVec(0f,-0.35398f,-1.34785f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.posVec(0f,-0.33761f,-1.28553f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.posVec(0f,-0.32129f,-1.22338f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.posVec(0f,-0.30504f,-1.16151f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.posVec(0f,-0.2889f,-1.10005f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.posVec(0f,-0.27289f,-1.03909f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.posVec(0f,-0.25704f,-0.97873f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.posVec(0f,-0.24137f,-0.91907f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.posVec(0f,-0.22591f,-0.8602f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.posVec(0f,-0.21068f,-0.8022f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.posVec(0f,-0.1957f,-0.74517f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.posVec(0f,-0.181f,-0.68919f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.posVec(0f,-0.1666f,-0.63436f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.posVec(0f,-0.15253f,-0.58078f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.posVec(0f,-0.13881f,-0.52854f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.posVec(0f,-0.12547f,-0.47775f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.posVec(0f,-0.11254f,-0.42853f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.posVec(0f,-0.10006f,-0.38099f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,-0.08805f,-0.33527f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.posVec(0f,-0.07656f,-0.2915f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.posVec(0f,-0.06562f,-0.24985f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.posVec(0f,-0.05528f,-0.21048f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.posVec(0f,-0.04559f,-0.17359f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.posVec(0f,-0.03661f,-0.13939f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.posVec(0f,-0.02839f,-0.10812f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.posVec(0f,-0.02102f,-0.08005f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.posVec(0f,-0.01457f,-0.0555f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.posVec(0f,-0.00915f,-0.03483f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.posVec(0f,-0.00485f,-0.01847f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.posVec(0f,-0.00182f,-0.00693f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.posVec(0f,-0.00021f,-0.0008f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel93(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.35f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(-2.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-0.76125f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(5.0403f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(12.59202f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(18.39962f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(22.03205f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(24.04566f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(24.90404f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(25f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(24.77647f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(24.31204f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(23.7312f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(23.08112f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(22.38574f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(21.65925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(20.91087f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(20.14708f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(19.37261f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(18.59113f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(17.80557f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(17.01834f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(16.23143f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(15.44667f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(14.66553f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(13.88949f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(13.11981f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(12.35776f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(11.60445f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(10.8611f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(10.12881f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(9.40869f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(8.70189f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(8.0096f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(7.33304f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(6.67345f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(6.03221f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(5.41069f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(4.81047f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(4.23316f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(3.68056f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(3.15465f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(2.6576f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(2.1918f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(1.75994f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(1.36509f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(1.0107f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(0.70072f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.43977f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.23321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.08746f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.01015f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel94(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.35f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(15.91932f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(10.64518f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(3.77998f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-1.49966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-4.80187f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-6.63242f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-7.41277f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-7.43294f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-7.29361f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-7.11936f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-6.92434f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-6.71572f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-6.49777f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-6.27326f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-6.04412f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-5.81178f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-5.57734f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-5.34167f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-5.1055f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-4.86943f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-4.634f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-4.39966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-4.16685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-3.93594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-3.70733f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-3.48133f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-3.25833f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-3.03864f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-2.82261f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-2.61057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-2.40288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-2.19991f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-2.00203f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-1.80966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-1.62321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-1.44314f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-1.26995f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-1.10417f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-0.9464f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-0.79728f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-0.65754f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-0.52798f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-0.40953f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-0.30321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.21022f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.13193f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.06996f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.02624f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.00304f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel95(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.02f,KeyframeAnimations.posVec(0f,0f,-0.00525f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.posVec(0f,0f,-0.02029f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.posVec(0f,0f,-0.04417f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.posVec(0f,0f,-0.0761f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.posVec(0f,0f,-0.11538f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.posVec(0f,0f,-0.16142f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.posVec(0f,0f,-0.21369f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.posVec(0f,0f,-0.27174f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.posVec(0f,0f,-0.33515f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.posVec(0f,0f,-0.40355f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.posVec(0f,0f,-0.47663f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.posVec(0f,0f,-0.55409f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.posVec(0f,0f,-0.63566f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.posVec(0f,0f,-0.72109f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.posVec(0f,0f,-0.81018f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.posVec(0f,0f,-0.9027f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.posVec(0f,0f,-0.99848f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.posVec(0f,0f,-1.09734f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.posVec(0f,0f,-1.19912f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.posVec(0f,0f,-1.30367f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.posVec(0f,0f,-1.41085f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.posVec(0f,0f,-1.52053f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.posVec(0f,0f,-1.63259f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.posVec(0f,0f,-1.74689f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,0f,-1.86335f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,0f,-1.98185f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,0f,-2.10228f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,0f,-2.22456f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,0f,-2.34858f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,0f,-2.47426f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,0f,-2.6015f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,0f,-2.73023f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.posVec(0f,0f,-2.86033f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.posVec(0f,0f,-2.99175f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.posVec(0f,0f,-3.12438f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.posVec(0f,0f,-3.25813f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.posVec(0f,0f,-3.39292f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.posVec(0f,0f,-3.52865f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.posVec(0f,0f,-3.66522f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.posVec(0f,0f,-3.80254f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.posVec(0f,0f,-3.94049f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.posVec(0f,0f,-4.07894f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.posVec(0f,0f,-4.21778f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.posVec(0f,0f,-4.35684f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.posVec(0f,0f,-4.49597f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.posVec(0f,0f,-4.63496f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.posVec(0f,0f,-4.77361f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.posVec(0f,0f,-4.91161f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.posVec(0f,0f,-5.04865f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,0f,-5.1843f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,0f,-5.31798f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,0f,-5.44896f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,0f,-5.57614f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,0f,-5.69788f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,0f,-5.81144f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.posVec(0f,0f,-5.9116f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.posVec(0f,0f,-5.98543f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.posVec(0f,0f,-6f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.posVec(0f,0f,-5.94635f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.posVec(0f,0f,-5.83489f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.posVec(0f,0f,-5.69549f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.posVec(0f,0f,-5.53947f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.posVec(0f,0f,-5.37258f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.posVec(0f,0f,-5.19822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.posVec(0f,0f,-5.01861f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.posVec(0f,0f,-4.8353f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.posVec(0f,0f,-4.64943f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.posVec(0f,0f,-4.46187f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.posVec(0f,0f,-4.27334f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.posVec(0f,0f,-4.0844f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.posVec(0f,0f,-3.89554f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.posVec(0f,0f,-3.7072f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.posVec(0f,0f,-3.51973f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.posVec(0f,0f,-3.33348f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.posVec(0f,0f,-3.14875f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.posVec(0f,0f,-2.96586f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.posVec(0f,0f,-2.78507f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.posVec(0f,0f,-2.60666f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.posVec(0f,0f,-2.43091f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.posVec(0f,0f,-2.25809f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.posVec(0f,0f,-2.08845f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.posVec(0f,0f,-1.9223f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.posVec(0f,0f,-1.75993f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.posVec(0f,0f,-1.60163f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.posVec(0f,0f,-1.44773f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.posVec(0f,0f,-1.29857f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.posVec(0f,0f,-1.15451f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,0f,-1.01596f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.posVec(0f,0f,-0.88333f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.posVec(0f,0f,-0.75712f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.posVec(0f,0f,-0.63782f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.posVec(0f,0f,-0.52603f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.posVec(0f,0f,-0.42239f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.posVec(0f,0f,-0.32762f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.posVec(0f,0f,-0.24257f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.posVec(0f,0f,-0.16817f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.posVec(0f,0f,-0.10554f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.posVec(0f,0f,-0.05597f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.posVec(0f,0f,-0.02099f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.posVec(0f,0f,-0.00244f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel96(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(12.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(11.70966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(9.07259f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(5.63999f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(3.00017f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(1.34907f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(0.43379f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(0.04362f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel97(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.02f,KeyframeAnimations.posVec(0f,-0.00011f,-0.00087f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.posVec(0f,-0.00044f,-0.00335f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.posVec(0f,-0.00096f,-0.0073f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.posVec(0f,-0.00166f,-0.01257f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.posVec(0f,-0.00251f,-0.01907f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.posVec(0f,-0.00351f,-0.02667f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.posVec(0f,-0.00465f,-0.03531f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.posVec(0f,-0.00591f,-0.0449f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.posVec(0f,-0.00729f,-0.05538f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.posVec(0f,-0.00878f,-0.06668f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.posVec(0f,-0.01037f,-0.07876f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.posVec(0f,-0.01205f,-0.09156f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.posVec(0f,-0.01383f,-0.10504f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.posVec(0f,-0.01569f,-0.11915f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.posVec(0f,-0.01762f,-0.13387f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.posVec(0f,-0.01964f,-0.14916f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.posVec(0f,-0.02172f,-0.16499f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.posVec(0f,-0.02387f,-0.18133f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.posVec(0f,-0.02609f,-0.19814f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.posVec(0f,-0.02836f,-0.21542f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.posVec(0f,-0.03069f,-0.23313f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.posVec(0f,-0.03308f,-0.25125f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.posVec(0f,-0.03552f,-0.26977f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.posVec(0f,-0.038f,-0.28866f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,-0.04054f,-0.3079f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,-0.04311f,-0.32748f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,-0.04573f,-0.34738f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,-0.04839f,-0.36759f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,-0.05109f,-0.38808f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,-0.05383f,-0.40885f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,-0.05659f,-0.42987f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,-0.05939f,-0.45115f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.posVec(0f,-0.06222f,-0.47264f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.posVec(0f,-0.06508f,-0.49436f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.posVec(0f,-0.06797f,-0.51627f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.posVec(0f,-0.07088f,-0.53838f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.posVec(0f,-0.07381f,-0.56065f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.posVec(0f,-0.07676f,-0.58308f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.posVec(0f,-0.07973f,-0.60564f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.posVec(0f,-0.08272f,-0.62833f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.posVec(0f,-0.08572f,-0.65113f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.posVec(0f,-0.08873f,-0.67401f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.posVec(0f,-0.09176f,-0.69695f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.posVec(0f,-0.09478f,-0.71993f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.posVec(0f,-0.09781f,-0.74292f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.posVec(0f,-0.10083f,-0.76589f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.posVec(0f,-0.10385f,-0.78879f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.posVec(0f,-0.10685f,-0.8116f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.posVec(0f,-0.10983f,-0.83424f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.11278f,-0.85666f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,-0.11569f,-0.87875f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,-0.11854f,-0.90039f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,-0.12131f,-0.92141f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,-0.12395f,-0.94152f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,-0.12642f,-0.96029f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.posVec(0f,-0.1286f,-0.97684f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.posVec(0f,-0.13021f,-0.98904f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.posVec(0f,-0.13053f,-0.99144f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.posVec(0f,-0.12936f,-0.98258f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.posVec(0f,-0.12693f,-0.96416f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.posVec(0f,-0.1239f,-0.94113f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.posVec(0f,-0.12051f,-0.91535f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.posVec(0f,-0.11688f,-0.88777f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.posVec(0f,-0.11308f,-0.85896f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.posVec(0f,-0.10918f,-0.82928f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.posVec(0f,-0.10519f,-0.79899f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.posVec(0f,-0.10115f,-0.76827f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.posVec(0f,-0.09707f,-0.73728f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.posVec(0f,-0.09296f,-0.70613f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.posVec(0f,-0.08885f,-0.67491f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.posVec(0f,-0.08475f,-0.6437f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.posVec(0f,-0.08065f,-0.61258f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.posVec(0f,-0.07657f,-0.5816f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.posVec(0f,-0.07252f,-0.55083f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.posVec(0f,-0.0685f,-0.5203f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.posVec(0f,-0.06452f,-0.49008f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.posVec(0f,-0.06059f,-0.46021f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.posVec(0f,-0.05671f,-0.43073f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.posVec(0f,-0.05288f,-0.40169f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.posVec(0f,-0.04912f,-0.37313f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.posVec(0f,-0.04543f,-0.3451f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.posVec(0f,-0.04182f,-0.31764f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.posVec(0f,-0.03829f,-0.29081f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.posVec(0f,-0.03484f,-0.26465f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.posVec(0f,-0.03149f,-0.23922f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.posVec(0f,-0.02825f,-0.21458f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.posVec(0f,-0.02512f,-0.19077f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,-0.0221f,-0.16788f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.posVec(0f,-0.01922f,-0.14596f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.posVec(0f,-0.01647f,-0.12511f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.posVec(0f,-0.01388f,-0.10539f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.posVec(0f,-0.01144f,-0.08692f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.posVec(0f,-0.00919f,-0.0698f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.posVec(0f,-0.00713f,-0.05414f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.posVec(0f,-0.00528f,-0.04008f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.posVec(0f,-0.00366f,-0.02779f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.posVec(0f,-0.0023f,-0.01744f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.posVec(0f,-0.00122f,-0.00925f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.posVec(0f,-0.00046f,-0.00347f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.posVec(0f,-0.00005f,-0.0004f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel98(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(16.86773f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(14.75807f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(12.01199f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(9.90014f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(8.57925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(7.84703f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(7.53489f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(7.43294f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(7.29361f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(7.11936f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(6.92434f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(6.71572f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(6.49777f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(6.27326f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(6.04412f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(5.81178f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(5.57734f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(5.34167f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(5.1055f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(4.86943f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(4.634f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(4.39966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(4.16685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(3.93594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(3.70733f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(3.48133f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(3.25833f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(3.03864f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(2.82261f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(2.61057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(2.40288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(2.19991f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(2.00203f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(1.80966f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(1.62321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(1.44314f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(1.26995f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(1.10417f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(0.9464f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(0.79728f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(0.65754f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(0.52798f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(0.40953f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(0.30321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(0.21022f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.13193f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.06996f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.02624f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.00304f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel99(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.02f,KeyframeAnimations.posVec(0f,-0.00023f,-0.00174f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.posVec(0f,-0.00088f,-0.0067f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.posVec(0f,-0.00192f,-0.0146f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.posVec(0f,-0.00331f,-0.02515f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.posVec(0f,-0.00502f,-0.03813f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.posVec(0f,-0.00702f,-0.05335f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.posVec(0f,-0.0093f,-0.07062f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.posVec(0f,-0.01182f,-0.0898f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.posVec(0f,-0.01458f,-0.11076f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.posVec(0f,-0.01756f,-0.13337f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.posVec(0f,-0.02074f,-0.15752f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.posVec(0f,-0.02411f,-0.18312f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.posVec(0f,-0.02766f,-0.21007f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.posVec(0f,-0.03137f,-0.23831f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.posVec(0f,-0.03525f,-0.26775f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.posVec(0f,-0.03928f,-0.29833f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.posVec(0f,-0.04344f,-0.32998f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.posVec(0f,-0.04774f,-0.36265f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.posVec(0f,-0.05217f,-0.39629f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.posVec(0f,-0.05672f,-0.43084f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.posVec(0f,-0.06138f,-0.46626f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.posVec(0f,-0.06616f,-0.50251f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.posVec(0f,-0.07103f,-0.53954f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.posVec(0f,-0.07601f,-0.57732f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,-0.08107f,-0.6158f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,-0.08623f,-0.65497f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,-0.09147f,-0.69477f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,-0.09679f,-0.73518f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,-0.10218f,-0.77616f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,-0.10765f,-0.8177f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,-0.11319f,-0.85975f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,-0.11879f,-0.90229f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.posVec(0f,-0.12445f,-0.94529f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.posVec(0f,-0.13017f,-0.98872f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.posVec(0f,-0.13594f,-1.03255f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.posVec(0f,-0.14176f,-1.07675f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.posVec(0f,-0.14762f,-1.1213f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.posVec(0f,-0.15353f,-1.16615f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.posVec(0f,-0.15947f,-1.21129f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.posVec(0f,-0.16544f,-1.25667f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.posVec(0f,-0.17145f,-1.30226f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.posVec(0f,-0.17747f,-1.34801f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.posVec(0f,-0.18351f,-1.3939f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.posVec(0f,-0.18956f,-1.43986f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.posVec(0f,-0.19561f,-1.48584f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.posVec(0f,-0.20166f,-1.53177f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.posVec(0f,-0.20769f,-1.57759f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.posVec(0f,-0.2137f,-1.6232f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.posVec(0f,-0.21966f,-1.66849f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,-0.22556f,-1.71331f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,-0.23138f,-1.7575f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,-0.23708f,-1.80078f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,-0.24261f,-1.84281f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,-0.24791f,-1.88304f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,-0.25285f,-1.92058f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.posVec(0f,-0.25721f,-1.95368f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.posVec(0f,-0.26042f,-1.97808f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.posVec(0f,-0.26105f,-1.98289f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.posVec(0f,-0.25872f,-1.96516f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.posVec(0f,-0.25387f,-1.92832f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.posVec(0f,-0.2478f,-1.88225f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.posVec(0f,-0.24102f,-1.83069f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.posVec(0f,-0.23375f,-1.77554f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.posVec(0f,-0.22617f,-1.71792f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.posVec(0f,-0.21835f,-1.65856f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.posVec(0f,-0.21038f,-1.59798f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.posVec(0f,-0.20229f,-1.53655f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.posVec(0f,-0.19413f,-1.47457f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.posVec(0f,-0.18593f,-1.41226f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.posVec(0f,-0.17771f,-1.34982f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.posVec(0f,-0.16949f,-1.28741f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.posVec(0f,-0.1613f,-1.22516f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.posVec(0f,-0.15314f,-1.16321f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.posVec(0f,-0.14504f,-1.10165f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.posVec(0f,-0.137f,-1.04061f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.posVec(0f,-0.12904f,-0.98016f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.posVec(0f,-0.12117f,-0.92041f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.posVec(0f,-0.11341f,-0.86145f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.posVec(0f,-0.10577f,-0.80337f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.posVec(0f,-0.09825f,-0.74626f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.posVec(0f,-0.09087f,-0.6902f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.posVec(0f,-0.08364f,-0.63529f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.posVec(0f,-0.07657f,-0.58162f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.posVec(0f,-0.06968f,-0.52931f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.posVec(0f,-0.06299f,-0.47845f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.posVec(0f,-0.0565f,-0.42915f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.posVec(0f,-0.05023f,-0.38154f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,-0.0442f,-0.33576f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.posVec(0f,-0.03843f,-0.29193f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.posVec(0f,-0.03294f,-0.25021f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.posVec(0f,-0.02775f,-0.21079f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.posVec(0f,-0.02289f,-0.17384f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.posVec(0f,-0.01838f,-0.13959f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.posVec(0f,-0.01425f,-0.10827f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.posVec(0f,-0.01055f,-0.08016f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.posVec(0f,-0.00732f,-0.05558f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.posVec(0f,-0.00459f,-0.03488f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.posVec(0f,-0.00244f,-0.0185f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.posVec(0f,-0.00091f,-0.00694f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.posVec(0f,-0.00011f,-0.0008f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel100(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(11.7679f,-4.2454f,19.5621f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(59.3673f,-30.1803f,21.9122f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(53.82437f,-29.20991f,21.32119f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(35.32977f,-25.97207f,19.34924f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(11.25581f,-21.75747f,16.7824f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-7.25812f,-18.51626f,14.80838f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-18.83785f,-16.48901f,13.57371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-25.25698f,-15.36522f,12.88928f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-27.9934f,-14.88615f,12.59752f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-28.2993f,-14.8326f,12.5649f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-28.04627f,-14.69998f,12.45256f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-27.52055f,-14.42443f,12.21914f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-26.86305f,-14.07981f,11.92721f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-26.12719f,-13.69412f,11.60048f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-25.34003f,-13.28155f,11.25098f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-24.51766f,-12.85052f,10.88585f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-23.67052f,-12.40651f,10.50972f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-22.80593f,-11.95334f,10.12584f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-21.92925f,-11.49385f,9.7366f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-21.04464f,-11.03019f,9.34383f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-20.1554f,-10.56411f,8.94901f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-19.26428f,-10.09705f,8.55335f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-18.37353f,-9.63017f,8.15785f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-17.4852f,-9.16457f,7.76343f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-16.60097f,-8.70112f,7.37084f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-15.72251f,-8.24069f,6.9808f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-14.85126f,-7.78404f,6.59396f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-13.98863f,-7.33191f,6.21096f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-13.13591f,-6.88497f,5.83235f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-12.29446f,-6.44394f,5.45875f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-11.46553f,-6.00946f,5.0907f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-10.65038f,-5.58222f,4.72877f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-9.8503f,-5.16287f,4.37354f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-9.06664f,-4.75213f,4.02559f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-8.30079f,-4.35072f,3.68556f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-7.55416f,-3.95938f,3.35405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-6.82829f,-3.57893f,3.03176f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-6.12475f,-3.21018f,2.71939f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-5.44531f,-2.85407f,2.41772f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-4.79182f,-2.51155f,2.12757f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-4.16629f,-2.18369f,1.84983f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-3.57098f,-1.87167f,1.58552f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-3.00833f,-1.57676f,1.3357f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-2.48105f,-1.3004f,1.10159f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-1.99221f,-1.04418f,0.88454f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-1.54524f,-0.80991f,0.68609f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-1.14408f,-0.59965f,0.50797f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.7932f,-0.41574f,0.35218f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.4978f,-0.26092f,0.22102f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.26399f,-0.13837f,0.11721f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.099f,-0.05189f,0.04396f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.01149f,-0.00602f,0.0051f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel101(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel102(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-25.09988f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-8.75007f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(12.53205f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(28.89894f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(39.13579f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(44.8105f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(47.22958f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(47.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(47.0753f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(46.19288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(45.08928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(43.85414f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(42.53291f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(41.15257f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(39.73066f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(38.27945f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(36.80796f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(35.32314f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(33.83057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(32.33484f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(30.83972f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(29.34867f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(27.86451f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(26.39003f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(24.92764f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(23.47974f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(22.04845f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(20.6361f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(19.24474f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(17.87652f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(16.53359f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(15.21823f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(13.93277f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(12.67955f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(11.46119f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(10.28031f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(9.13989f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(8.043f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(6.99306f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(5.99384f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(5.04944f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(4.16441f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(3.34389f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(2.59367f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(1.92032f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(1.33137f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.83556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.44311f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.16617f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.01928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel103(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel104(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(11.7679f,4.2454f,-19.5621f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(59.3673f,30.1803f,-21.9122f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(53.82437f,29.20991f,-21.32119f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(35.32977f,25.97207f,-19.34924f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(11.25581f,21.75747f,-16.7824f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-7.25812f,18.51626f,-14.80838f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-18.83785f,16.48901f,-13.57371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-25.25698f,15.36522f,-12.88928f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-27.9934f,14.88615f,-12.59752f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-28.2993f,14.8326f,-12.5649f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-28.04627f,14.69998f,-12.45256f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-27.52055f,14.42443f,-12.21914f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-26.86305f,14.07981f,-11.92721f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-26.12719f,13.69412f,-11.60048f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-25.34003f,13.28155f,-11.25098f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-24.51766f,12.85052f,-10.88585f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-23.67052f,12.40651f,-10.50972f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-22.80593f,11.95334f,-10.12584f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-21.92925f,11.49385f,-9.7366f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-21.04464f,11.03019f,-9.34383f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-20.1554f,10.56411f,-8.94901f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-19.26428f,10.09705f,-8.55335f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-18.37353f,9.63017f,-8.15785f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-17.4852f,9.16457f,-7.76343f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-16.60097f,8.70112f,-7.37084f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-15.72251f,8.24069f,-6.9808f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-14.85126f,7.78404f,-6.59396f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-13.98863f,7.33191f,-6.21096f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-13.13591f,6.88497f,-5.83235f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-12.29446f,6.44394f,-5.45875f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-11.46553f,6.00946f,-5.0907f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-10.65038f,5.58222f,-4.72877f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-9.8503f,5.16287f,-4.37354f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-9.06664f,4.75213f,-4.02559f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-8.30079f,4.35072f,-3.68556f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-7.55416f,3.95938f,-3.35405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-6.82829f,3.57893f,-3.03176f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-6.12475f,3.21018f,-2.71939f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-5.44531f,2.85407f,-2.41772f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-4.79182f,2.51155f,-2.12757f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-4.16629f,2.18369f,-1.84983f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-3.57098f,1.87167f,-1.58552f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-3.00833f,1.57676f,-1.3357f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-2.48105f,1.3004f,-1.10159f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-1.99221f,1.04418f,-0.88454f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-1.54524f,0.80991f,-0.68609f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-1.14408f,0.59965f,-0.50797f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.7932f,0.41574f,-0.35218f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.4978f,0.26092f,-0.22102f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.26399f,0.13837f,-0.11721f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.099f,0.05189f,-0.04396f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.01149f,0.00602f,-0.0051f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel105(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel106(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-25.09988f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-8.75007f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(12.53205f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(28.89894f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(39.13579f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(44.8105f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(47.22958f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(47.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(47.0753f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(46.19288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(45.08928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(43.85414f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(42.53291f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(41.15257f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(39.73066f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(38.27945f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(36.80796f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(35.32314f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(33.83057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(32.33484f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(30.83972f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(29.34867f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(27.86451f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(26.39003f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(24.92764f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(23.47974f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(22.04845f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(20.6361f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(19.24474f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(17.87652f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(16.53359f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(15.21823f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(13.93277f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(12.67955f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(11.46119f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(10.28031f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(9.13989f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(8.043f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(6.99306f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(5.99384f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(5.04944f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(4.16441f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(3.34389f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(2.59367f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(1.92032f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(1.33137f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.83556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.44311f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.16617f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.01928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel107(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel108(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-15f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(-22.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-20.07397f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-11.97923f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-1.44252f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(6.66067f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(11.7289f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(14.53843f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(15.73612f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(15.87f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(15.72811f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(15.43328f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(15.06456f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(14.6519f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(14.21047f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(13.74929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(13.27422f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(12.78936f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(12.29773f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(11.80165f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(11.30297f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(10.80324f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(10.30371f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(9.80555f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(9.30968f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(8.81705f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(8.32846f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(7.8447f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(7.3665f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(6.89463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(6.42977f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(5.97264f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(5.52396f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(5.08449f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(4.65501f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(4.2363f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(3.82925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(3.43471f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(3.05368f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(2.68721f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(2.33642f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(2.00257f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(1.68704f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(1.39135f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(1.11721f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(0.86656f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(0.64159f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(0.44482f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.27916f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.14804f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.05552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.00644f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel109(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel110(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(25f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(41.67f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(38.74574f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(28.98859f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(16.28797f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(6.52063f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(0.41155f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-2.97497f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-4.41862f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-4.58f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-4.53905f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-4.45397f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-4.34756f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-4.22846f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-4.10107f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-3.96797f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-3.83087f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-3.69094f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-3.54906f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-3.40589f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-3.26198f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-3.11776f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-2.9736f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-2.82983f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-2.68673f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-2.54455f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-2.40355f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-2.26394f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-2.12594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-1.98975f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-1.8556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-1.72367f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-1.59419f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-1.46736f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-1.34341f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-1.22258f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-1.1051f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-0.99124f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-0.88128f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-0.77551f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-0.67428f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-0.57793f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-0.48687f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-0.40154f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-0.32242f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-0.25008f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-0.18516f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.12837f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.08057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.04272f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.01602f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.00186f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel111(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel112(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-15f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(-22.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-20.07397f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-11.97923f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-1.44252f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(6.66067f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(11.7289f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(14.53843f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(15.73612f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(15.87f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(15.72811f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(15.43328f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(15.06456f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(14.6519f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(14.21047f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(13.74929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(13.27422f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(12.78936f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(12.29773f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(11.80165f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(11.30297f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(10.80324f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(10.30371f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(9.80555f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(9.30968f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(8.81705f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(8.32846f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(7.8447f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(7.3665f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(6.89463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(6.42977f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(5.97264f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(5.52396f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(5.08449f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(4.65501f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(4.2363f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(3.82925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(3.43471f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(3.05368f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(2.68721f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(2.33642f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(2.00257f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(1.68704f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(1.39135f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(1.11721f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(0.86656f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(0.64159f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(0.44482f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(0.27916f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(0.14804f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(0.05552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(0.00644f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel113(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel114(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(25f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1f,KeyframeAnimations.degreeVec(41.67f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(38.74574f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(28.98859f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(16.28797f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(6.52063f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(0.41155f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-2.97497f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-4.41862f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-4.58f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.17f,KeyframeAnimations.degreeVec(-4.53905f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.19f,KeyframeAnimations.degreeVec(-4.45397f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.21f,KeyframeAnimations.degreeVec(-4.34756f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.23f,KeyframeAnimations.degreeVec(-4.22846f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-4.10107f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.27f,KeyframeAnimations.degreeVec(-3.96797f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.29f,KeyframeAnimations.degreeVec(-3.83087f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.31f,KeyframeAnimations.degreeVec(-3.69094f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.33f,KeyframeAnimations.degreeVec(-3.54906f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(-3.40589f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(-3.26198f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(-3.11776f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(-2.9736f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(-2.82983f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-2.68673f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(-2.54455f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(-2.40355f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(-2.26394f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(-2.12594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-1.98975f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(-1.8556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(-1.72367f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(-1.59419f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(-1.46736f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-1.34341f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(-1.22258f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(-1.1051f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(-0.99124f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(-0.88128f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-0.77551f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(-0.67428f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(-0.57793f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(-0.48687f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(-0.40154f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(-0.32242f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(-0.25008f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(-0.18516f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(-0.12837f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(-0.08057f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(-0.04272f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(-0.01602f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(-0.00186f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel115(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition attack_belly(){
return AnimationDefinition.Builder.withLength(2f)
.addAnimation("head_with_neck",channel91())
.addAnimation("head_with_neck",channel92())
.addAnimation("head_only",channel93())
.addAnimation("body_main",channel94())
.addAnimation("body_main",channel95())
.addAnimation("body_fat1",channel96())
.addAnimation("body_fat1",channel97())
.addAnimation("body_fat2",channel98())
.addAnimation("body_fat2",channel99())
.addAnimation("hand_r",channel100())
.addAnimation("hand_r",channel101())
.addAnimation("hand_r_1",channel102())
.addAnimation("hand_r_1",channel103())
.addAnimation("hand_l",channel104())
.addAnimation("hand_l",channel105())
.addAnimation("hand_l_1",channel106())
.addAnimation("hand_l_1",channel107())
.addAnimation("leg_r",channel108())
.addAnimation("leg_r",channel109())
.addAnimation("leg_r_1",channel110())
.addAnimation("leg_r_1",channel111())
.addAnimation("leg_l",channel112())
.addAnimation("leg_l",channel113())
.addAnimation("leg_l_1",channel114())
.addAnimation("leg_l_1",channel115())
.build();
}

private static AnimationChannel channel116(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,1.64391f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,7.12901f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,14.26882f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,19.75964f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,23.19394f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,25.09771f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,25.90928f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.65f,KeyframeAnimations.posVec(0f,26f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.67f,KeyframeAnimations.posVec(0f,25.88649f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.69f,KeyframeAnimations.posVec(0f,25.60361f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.71f,KeyframeAnimations.posVec(0f,25.20793f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.73f,KeyframeAnimations.posVec(0f,24.73384f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,24.20439f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.77f,KeyframeAnimations.posVec(0f,23.6361f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.79f,KeyframeAnimations.posVec(0f,23.04154f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.81f,KeyframeAnimations.posVec(0f,22.43074f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.83f,KeyframeAnimations.posVec(0f,21.81204f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.85f,KeyframeAnimations.posVec(0f,21.19265f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.87f,KeyframeAnimations.posVec(0f,20.57908f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.89f,KeyframeAnimations.posVec(0f,19.97737f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.91f,KeyframeAnimations.posVec(0f,19.39332f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.93f,KeyframeAnimations.posVec(0f,18.8327f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.95f,KeyframeAnimations.posVec(0f,18.30142f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.97f,KeyframeAnimations.posVec(0f,17.80564f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.99f,KeyframeAnimations.posVec(0f,17.35204f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.01f,KeyframeAnimations.posVec(0f,16.94796f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.03f,KeyframeAnimations.posVec(0f,16.60169f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.05f,KeyframeAnimations.posVec(0f,16.32278f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.07f,KeyframeAnimations.posVec(0f,16.12249f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.09f,KeyframeAnimations.posVec(0f,16.01439f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,16f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.12f,KeyframeAnimations.posVec(0f,15.56681f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.posVec(0f,14.34784f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.posVec(0f,12.46399f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.posVec(0f,10.03614f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.posVec(0f,7.18518f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.posVec(0f,4.032f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.posVec(0f,0.69748f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.posVec(0f,-2.69748f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.posVec(0f,-6.032f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.posVec(0f,-9.18518f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.posVec(0f,-12.03614f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.posVec(0f,-14.46399f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.36f,KeyframeAnimations.posVec(0f,-16.34784f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.38f,KeyframeAnimations.posVec(0f,-17.56681f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.42f,KeyframeAnimations.posVec(0f,-17.88663f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.44f,KeyframeAnimations.posVec(0f,-17.64134f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.46f,KeyframeAnimations.posVec(0f,-17.32637f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.48f,KeyframeAnimations.posVec(0f,-16.96719f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-16.57723f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.52f,KeyframeAnimations.posVec(0f,-16.16463f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.54f,KeyframeAnimations.posVec(0f,-15.73483f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.56f,KeyframeAnimations.posVec(0f,-15.29167f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.58f,KeyframeAnimations.posVec(0f,-14.83805f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.6f,KeyframeAnimations.posVec(0f,-14.37621f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.62f,KeyframeAnimations.posVec(0f,-13.90803f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.64f,KeyframeAnimations.posVec(0f,-13.43501f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.66f,KeyframeAnimations.posVec(0f,-12.95854f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.68f,KeyframeAnimations.posVec(0f,-12.47981f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.7f,KeyframeAnimations.posVec(0f,-11.99999f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.72f,KeyframeAnimations.posVec(0f,-11.52019f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.74f,KeyframeAnimations.posVec(0f,-11.04146f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.76f,KeyframeAnimations.posVec(0f,-10.56499f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.78f,KeyframeAnimations.posVec(0f,-10.09197f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.8f,KeyframeAnimations.posVec(0f,-9.62379f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.82f,KeyframeAnimations.posVec(0f,-9.16195f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.84f,KeyframeAnimations.posVec(0f,-8.70833f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.86f,KeyframeAnimations.posVec(0f,-8.26517f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.88f,KeyframeAnimations.posVec(0f,-7.83537f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.9f,KeyframeAnimations.posVec(0f,-7.42277f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.92f,KeyframeAnimations.posVec(0f,-7.03281f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.94f,KeyframeAnimations.posVec(0f,-6.67363f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.96f,KeyframeAnimations.posVec(0f,-6.35866f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.98f,KeyframeAnimations.posVec(0f,-6.11337f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,-6f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.02f,KeyframeAnimations.posVec(0f,-5.94768f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.04f,KeyframeAnimations.posVec(0f,-5.84063f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.06f,KeyframeAnimations.posVec(0f,-5.70782f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.08f,KeyframeAnimations.posVec(0f,-5.55982f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.1f,KeyframeAnimations.posVec(0f,-5.40195f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.12f,KeyframeAnimations.posVec(0f,-5.23729f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.14f,KeyframeAnimations.posVec(0f,-5.06785f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.16f,KeyframeAnimations.posVec(0f,-4.89503f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.18f,KeyframeAnimations.posVec(0f,-4.71985f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.2f,KeyframeAnimations.posVec(0f,-4.54309f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.22f,KeyframeAnimations.posVec(0f,-4.36538f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.24f,KeyframeAnimations.posVec(0f,-4.1872f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.26f,KeyframeAnimations.posVec(0f,-4.009f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.28f,KeyframeAnimations.posVec(0f,-3.83114f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,-3.65394f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.32f,KeyframeAnimations.posVec(0f,-3.47769f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.34f,KeyframeAnimations.posVec(0f,-3.30266f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.36f,KeyframeAnimations.posVec(0f,-3.12908f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.38f,KeyframeAnimations.posVec(0f,-2.9572f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.4f,KeyframeAnimations.posVec(0f,-2.78724f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.42f,KeyframeAnimations.posVec(0f,-2.61943f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.44f,KeyframeAnimations.posVec(0f,-2.45397f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.46f,KeyframeAnimations.posVec(0f,-2.29109f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.48f,KeyframeAnimations.posVec(0f,-2.13101f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.5f,KeyframeAnimations.posVec(0f,-1.97395f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.52f,KeyframeAnimations.posVec(0f,-1.82016f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.54f,KeyframeAnimations.posVec(0f,-1.66987f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.56f,KeyframeAnimations.posVec(0f,-1.52333f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.58f,KeyframeAnimations.posVec(0f,-1.38083f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.6f,KeyframeAnimations.posVec(0f,-1.24264f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.62f,KeyframeAnimations.posVec(0f,-1.10908f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.64f,KeyframeAnimations.posVec(0f,-0.98049f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.66f,KeyframeAnimations.posVec(0f,-0.85723f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.68f,KeyframeAnimations.posVec(0f,-0.73969f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.7f,KeyframeAnimations.posVec(0f,-0.62832f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.72f,KeyframeAnimations.posVec(0f,-0.5236f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.74f,KeyframeAnimations.posVec(0f,-0.42607f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.76f,KeyframeAnimations.posVec(0f,-0.33634f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.78f,KeyframeAnimations.posVec(0f,-0.25511f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.8f,KeyframeAnimations.posVec(0f,-0.18315f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.82f,KeyframeAnimations.posVec(0f,-0.12136f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.84f,KeyframeAnimations.posVec(0f,-0.0708f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.86f,KeyframeAnimations.posVec(0f,-0.0327f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.88f,KeyframeAnimations.posVec(0f,-0.00851f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.9f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel117(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(15f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(7.17f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel118(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.posVec(0f,-1f,-7f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,-0.48f,-3.35f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel119(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(12.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(12.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel120(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(12.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(22.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel121(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,-1f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,-0.56f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel122(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-17.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel123(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.posVec(0f,0.60141f,-1.90743f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,0.38f,-1.19f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel124(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(15f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel125(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.posVec(0f,0f,-2f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,0f,-1.25f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel126(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(32.3993f,-14.0284f,20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(77.3993f,-14.0284f,20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-90.1007f,-14.0284f,20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-20.1007f,-14.0284f,20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel127(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,-1f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,-0.56f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel128(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-45f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(37.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel129(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel130(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(32.3993f,14.0284f,-20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(77.3993f,14.0284f,-20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-90.1007f,14.0284f,-20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-20.1007f,14.0284f,-20.9057f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel131(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,-1f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,-0.56f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel132(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-45f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(37.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel133(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel134(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,10f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-62.5f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-62.5f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.4f,KeyframeAnimations.degreeVec(-15f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel135(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel136(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,-12.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-25f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-25f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.4f,KeyframeAnimations.degreeVec(40f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel137(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel138(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,-10f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-62.5f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-62.5f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.4f,KeyframeAnimations.degreeVec(-15f,0f,-17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel139(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel140(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,12.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.65f,KeyframeAnimations.degreeVec(-25f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-25f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.4f,KeyframeAnimations.degreeVec(40f,0f,17.5f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel141(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.9f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationDefinition attack_slam(){
return AnimationDefinition.Builder.withLength(2.9f)
.addAnimation("main",channel116())
.addAnimation("head_with_neck",channel117())
.addAnimation("head_with_neck",channel118())
.addAnimation("head_only",channel119())
.addAnimation("body_main",channel120())
.addAnimation("body_main",channel121())
.addAnimation("body_fat1",channel122())
.addAnimation("body_fat1",channel123())
.addAnimation("body_fat2",channel124())
.addAnimation("body_fat2",channel125())
.addAnimation("hand_r",channel126())
.addAnimation("hand_r",channel127())
.addAnimation("hand_r_1",channel128())
.addAnimation("hand_r_1",channel129())
.addAnimation("hand_l",channel130())
.addAnimation("hand_l",channel131())
.addAnimation("hand_l_1",channel132())
.addAnimation("hand_l_1",channel133())
.addAnimation("leg_r",channel134())
.addAnimation("leg_r",channel135())
.addAnimation("leg_r_1",channel136())
.addAnimation("leg_r_1",channel137())
.addAnimation("leg_l",channel138())
.addAnimation("leg_l",channel139())
.addAnimation("leg_l_1",channel140())
.addAnimation("leg_l_1",channel141())
.build();
}

private static AnimationChannel channel142(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel143(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel144(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel145(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel146(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-5.3125f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel147(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel148(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel149(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel150(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel151(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel152(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-9.6658f,2.576f,24.7822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-9.6658f,2.576f,12.2822f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel153(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel154(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-19.3701f,5.0787f,-0.8672f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(1.1969f,-4.071f,-1.371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-7.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel155(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel156(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-9.6658f,-2.576f,-24.7822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-9.6658f,-2.576f,-12.2822f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel157(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel158(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-19.3701f,-5.0787f,0.8672f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(1.1969f,4.071f,1.371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-7.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel159(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel160(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-53.669f,11.1447f,13.579f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel161(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel162(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(10.0947f,0.3157f,-15.1949f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel163(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel164(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-53.669f,-11.1447f,-13.579f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel165(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel166(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(10.0947f,-0.3157f,15.1949f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel167(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition sit_down(){
return AnimationDefinition.Builder.withLength(0.75f)
.addAnimation("main",channel142())
.addAnimation("head_with_neck",channel143())
.addAnimation("head_with_neck",channel144())
.addAnimation("head_only",channel145())
.addAnimation("body_main",channel146())
.addAnimation("body_main",channel147())
.addAnimation("body_fat1",channel148())
.addAnimation("body_fat1",channel149())
.addAnimation("body_fat2",channel150())
.addAnimation("body_fat2",channel151())
.addAnimation("hand_r",channel152())
.addAnimation("hand_r",channel153())
.addAnimation("hand_r_1",channel154())
.addAnimation("hand_r_1",channel155())
.addAnimation("hand_l",channel156())
.addAnimation("hand_l",channel157())
.addAnimation("hand_l_1",channel158())
.addAnimation("hand_l_1",channel159())
.addAnimation("leg_r",channel160())
.addAnimation("leg_r",channel161())
.addAnimation("leg_r_1",channel162())
.addAnimation("leg_r_1",channel163())
.addAnimation("leg_l",channel164())
.addAnimation("leg_l",channel165())
.addAnimation("leg_l_1",channel166())
.addAnimation("leg_l_1",channel167())
.build();
}

private static AnimationChannel channel168(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel169(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0.5f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel170(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel171(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel172(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(7.8125f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel173(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel174(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel175(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel176(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel177(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel178(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-9.6658f,2.576f,12.2822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-9.6658f,2.576f,24.7822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel179(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel180(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-7.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(1.1969f,-4.071f,-1.371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-19.3701f,5.0787f,-0.8672f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel181(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel182(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-9.6658f,-2.576f,-12.2822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-9.6658f,-2.576f,-24.7822f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel183(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel184(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-7.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(1.1969f,4.071f,1.371f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-19.3701f,-5.0787f,0.8672f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel185(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel186(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-53.669f,11.1447f,13.579f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel187(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel188(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(10.0947f,0.3157f,-15.1949f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel189(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel190(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(-53.669f,-11.1447f,-13.579f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel191(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel192(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.25f,KeyframeAnimations.degreeVec(10.0947f,-0.3157f,15.1949f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel193(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0.75f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition stand_up(){
return AnimationDefinition.Builder.withLength(0.75f)
.addAnimation("main",channel168())
.addAnimation("head_with_neck",channel169())
.addAnimation("head_with_neck",channel170())
.addAnimation("head_only",channel171())
.addAnimation("body_main",channel172())
.addAnimation("body_main",channel173())
.addAnimation("body_fat1",channel174())
.addAnimation("body_fat1",channel175())
.addAnimation("body_fat2",channel176())
.addAnimation("body_fat2",channel177())
.addAnimation("hand_r",channel178())
.addAnimation("hand_r",channel179())
.addAnimation("hand_r_1",channel180())
.addAnimation("hand_r_1",channel181())
.addAnimation("hand_l",channel182())
.addAnimation("hand_l",channel183())
.addAnimation("hand_l_1",channel184())
.addAnimation("hand_l_1",channel185())
.addAnimation("leg_r",channel186())
.addAnimation("leg_r",channel187())
.addAnimation("leg_r_1",channel188())
.addAnimation("leg_r_1",channel189())
.addAnimation("leg_l",channel190())
.addAnimation("leg_l",channel191())
.addAnimation("leg_l_1",channel192())
.addAnimation("leg_l_1",channel193())
.build();
}

private static AnimationChannel channel194(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel195(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel196(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-2.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel197(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,1f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel198(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(7.8526f,-0.3418f,2.4765f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-0.21f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(6.5238f,0.2842f,-2.4838f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-2.12f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel199(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.posVec(0f,0.49952f,-0.02181f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel200(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(7.4717f,-0.6518f,4.9574f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.15f,KeyframeAnimations.degreeVec(-2.88f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(3.9549f,0.3457f,-4.9881f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(-5.55f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel201(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.posVec(0f,0.49952f,-0.02181f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel202(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-5.5463f,-39.5053f,-3.3167f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-25.5463f,-39.5053f,-3.3167f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.25f,KeyframeAnimations.degreeVec(-5.5463f,-39.5053f,-3.3167f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel203(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel204(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.75f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-25.8425f,-9.0172f,4.341f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(0.5431f,-24.1197f,3.9535f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(-20.6047f,-28.3865f,5.8409f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2.25f,KeyframeAnimations.degreeVec(-20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel205(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel206(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel207(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel208(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel209(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel210(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel211(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel212(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel213(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel214(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel215(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel216(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel217(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-2f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(3f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition scratch_belly(){
return AnimationDefinition.Builder.withLength(3f)
.addAnimation("head_with_neck",channel194())
.addAnimation("head_with_neck",channel195())
.addAnimation("body_main",channel196())
.addAnimation("body_main",channel197())
.addAnimation("body_fat1",channel198())
.addAnimation("body_fat1",channel199())
.addAnimation("body_fat2",channel200())
.addAnimation("body_fat2",channel201())
.addAnimation("hand_r",channel202())
.addAnimation("hand_r",channel203())
.addAnimation("hand_r_1",channel204())
.addAnimation("hand_r_1",channel205())
.addAnimation("hand_l",channel206())
.addAnimation("hand_l",channel207())
.addAnimation("hand_l_1",channel208())
.addAnimation("hand_l_1",channel209())
.addAnimation("leg_r",channel210())
.addAnimation("leg_r",channel211())
.addAnimation("leg_r_1",channel212())
.addAnimation("leg_r_1",channel213())
.addAnimation("leg_l",channel214())
.addAnimation("leg_l",channel215())
.addAnimation("leg_l_1",channel216())
.addAnimation("leg_l_1",channel217())
.build();
}

private static AnimationChannel channel218(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.posVec(0f,-18f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel219(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(20f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(20f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel220(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel221(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(2.5f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(2.5f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(2.5f,0f,-15f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(0f,0f,-15f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel222(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(6f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(10f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel223(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel224(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(15f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(15f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(15f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel225(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel226(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel227(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel228(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-16.4783f,-53.7487f,26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(-25.5899f,-51.0449f,37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-16.4783f,-53.7487f,26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(-25.5899f,-51.0449f,37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(-16.4783f,-53.7487f,26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(-25.5899f,-51.0449f,37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(-16.4783f,-53.7487f,26.1374f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel229(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-22.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-22.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(-22.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(-22.9528f,-0.0296f,-0.7405f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel230(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-16.4783f,53.7487f,-26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(-25.5899f,51.0449f,-37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-16.4783f,53.7487f,-26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(6f,KeyframeAnimations.degreeVec(-25.5899f,51.0449f,-37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(-16.4783f,53.7487f,-26.1374f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(10f,KeyframeAnimations.degreeVec(-25.5899f,51.0449f,-37.6207f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(-16.4783f,53.7487f,-26.1374f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel231(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-22.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(4f,KeyframeAnimations.degreeVec(-22.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(8f,KeyframeAnimations.degreeVec(-22.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(12f,KeyframeAnimations.degreeVec(-22.9528f,0.0296f,0.7405f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel232(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.degreeVec(-48.735f,9.9318f,14.482f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel233(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.degreeVec(-2.7192f,-9.0022f,-25.6839f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel234(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.degreeVec(-48.735f,-9.9318f,-14.482f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel235(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(4f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(8f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(12f,KeyframeAnimations.degreeVec(-2.7192f,9.0022f,25.6839f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationDefinition sleep(){
return AnimationDefinition.Builder.withLength(12f).looping()
.addAnimation("main",channel218())
.addAnimation("head_with_neck",channel219())
.addAnimation("head_with_neck",channel220())
.addAnimation("head_only",channel221())
.addAnimation("body_main",channel222())
.addAnimation("body_main",channel223())
.addAnimation("body_fat1",channel224())
.addAnimation("body_fat1",channel225())
.addAnimation("body_fat2",channel226())
.addAnimation("body_fat2",channel227())
.addAnimation("hand_r",channel228())
.addAnimation("hand_r_1",channel229())
.addAnimation("hand_l",channel230())
.addAnimation("hand_l_1",channel231())
.addAnimation("leg_r",channel232())
.addAnimation("leg_r_1",channel233())
.addAnimation("leg_l",channel234())
.addAnimation("leg_l_1",channel235())
.build();
}

private static AnimationChannel channel236(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.05f,KeyframeAnimations.degreeVec(-50f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel237(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.posVec(0f,-5f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.05f,KeyframeAnimations.posVec(0f,-11f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel238(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-15f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel239(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel240(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(-0.02882f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(-0.11099f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(-0.24088f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(-0.41379f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(-0.62573f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(-0.87323f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(-1.15333f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(-1.46342f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(-1.80123f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(-2.16469f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(-2.55201f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(-2.96153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(-3.39182f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(-3.84153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(-4.30942f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(-4.79438f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(-5.29539f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(-5.81148f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(-6.34177f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(-6.88542f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(-7.44167f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(-8.00973f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(-8.58891f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(-9.17852f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-9.77795f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(-10.38656f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(-11.00368f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(-11.62871f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(-12.2611f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(-12.90018f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(-13.54531f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(-14.19591f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(-14.85127f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(-15.51071f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(-16.17345f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(-16.83864f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(-17.50541f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(-18.17269f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(-18.83932f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(-19.5039f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(-20.16475f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(-20.81976f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(-21.46634f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(-22.10096f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(-22.71884f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(-23.31301f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(-23.87256f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(-24.37857f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(-24.79213f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-25f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-24.92348f,-0.1619f,0.34588f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-24.69607f,-0.64308f,1.37381f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-24.32501f,-1.42818f,3.05103f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-23.82418f,-2.48785f,5.3148f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-23.21548f,-3.77579f,8.06622f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-22.52963f,-5.22694f,11.16631f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-21.806f,-6.75803f,14.43719f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(-21.09087f,-8.27116f,17.66968f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(-20.43409f,-9.66079f,20.63835f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(-19.88485f,-10.82291f,23.12098f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(-19.48708f,-11.66453f,24.91893f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(-19.27608f,-12.11097f,25.87267f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.25f,KeyframeAnimations.degreeVec(-19.2488f,-12.1687f,25.996f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel241(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(-0.00865f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(-0.0333f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(-0.07226f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(-0.12414f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(-0.18772f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(-0.26197f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(-0.346f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(-0.43903f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(-0.54037f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(-0.64941f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(-0.7656f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(-0.88846f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(-1.01755f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(-1.15246f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(-1.29283f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(-1.43831f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(-1.58862f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(-1.74344f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(-1.90253f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(-2.06563f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(-2.2325f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(-2.40292f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(-2.57667f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(-2.75356f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(-2.93338f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(-3.11597f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(-3.3011f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(-3.48861f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(-3.67833f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(-3.87005f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(-4.06359f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(-4.25877f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(-4.45538f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(-4.65321f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(-4.85203f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(-5.05159f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(-5.25162f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(-5.45181f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(-5.6518f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(-5.85117f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(-6.04942f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(-6.24593f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(-6.4399f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(-6.63029f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(-6.81565f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(-6.9939f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(-7.16177f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(-7.31357f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(-7.43764f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(-7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-7.7554f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-8.39187f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-9.28216f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-10.34885f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-11.54012f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-12.81878f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-14.15652f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(-15.53084f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(-16.9229f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(-18.31653f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(-19.69707f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(-21.05092f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(-22.36504f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(-23.62642f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(-24.82182f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(-25.93732f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(-26.95791f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.36f,KeyframeAnimations.degreeVec(-27.86709f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.38f,KeyframeAnimations.degreeVec(-28.6462f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-29.27374f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.42f,KeyframeAnimations.degreeVec(-29.7244f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.44f,KeyframeAnimations.degreeVec(-29.96762f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(-30f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel242(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel243(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.01153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.04439f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.09635f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.16552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.25029f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.34929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.46133f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.58537f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.72049f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.86587f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.0208f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.18461f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(1.35673f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(1.53661f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.72377f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.91775f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(2.11816f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(2.32459f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(2.53671f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(2.75417f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(2.97667f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(3.20389f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(3.43556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(3.67141f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(3.91118f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(4.15463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(4.40147f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(4.65149f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(4.90444f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(5.16007f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(5.41813f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(5.67836f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(5.94051f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(6.20429f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(6.46938f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(6.73546f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(7.00216f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(7.26908f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(7.53573f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(7.80156f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(8.0659f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(8.3279f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(8.58654f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(8.84038f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(9.08754f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(9.32521f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(9.54902f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(9.75143f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(9.91685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel244(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel245(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.00716f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.02766f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.06015f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.10354f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.15685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.21926f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.29005f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.36858f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.45429f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.54668f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(0.64531f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(0.74976f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(0.85968f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(0.97473f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.09462f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.21905f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(1.34779f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(1.48059f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(1.61723f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(1.75751f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(1.90122f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(2.04821f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(2.19829f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(2.3513f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(2.50709f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(2.66552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(2.82644f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(2.9897f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(3.15519f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(3.32276f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(3.4923f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(3.66367f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(3.83674f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(4.01139f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(4.18747f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(4.36485f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(4.54341f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(4.72296f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(4.90337f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(5.08444f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(5.26598f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(5.4478f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(5.62961f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(5.81115f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(5.99208f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(6.17197f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(6.35032f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(6.52645f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(6.6995f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(6.86824f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(7.03091f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(7.18468f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(7.32463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(7.44037f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(7.5f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel246(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel247(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.0191f,0.02388f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.07375f,0.09218f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.1604f,0.2005f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.27609f,0.34512f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.41825f,0.52282f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.58469f,0.73086f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.77346f,0.96682f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.98287f,1.22859f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(1.21143f,1.51429f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(1.45781f,1.82227f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.72081f,2.15102f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.99936f,2.49919f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(2.29247f,2.86559f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(2.59928f,3.2491f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(2.91899f,3.64874f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(3.25081f,4.06351f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(3.5941f,4.49263f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(3.94824f,4.9353f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(4.31262f,5.39077f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(4.68668f,5.85835f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(5.06993f,6.33742f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(5.4619f,6.82737f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(5.86212f,7.32764f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(6.27013f,7.83767f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(6.68557f,8.35697f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(7.10805f,8.88506f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(7.53716f,9.42146f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(7.97254f,9.96567f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(8.41383f,10.51729f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(8.8607f,11.07587f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(9.31279f,11.64099f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(9.76978f,12.21222f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(10.23131f,12.78914f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(10.69703f,13.37129f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(11.16658f,13.95822f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(11.63961f,14.54951f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(12.11576f,15.1447f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(12.59457f,15.74322f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(13.07565f,16.34456f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(13.55851f,16.94814f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(14.04262f,17.55328f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(14.52746f,18.15933f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(15.0123f,18.76538f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(15.49641f,19.37051f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(15.97888f,19.9736f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(16.45858f,20.57322f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(16.93417f,21.16772f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(17.40386f,21.75483f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(17.86532f,22.33166f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(18.31531f,22.89414f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(18.74908f,23.43635f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(19.15915f,23.94894f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(19.53235f,24.41544f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(19.84099f,24.80123f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(20f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(19.7339f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(18.94306f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(17.65269f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(15.91106f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(13.79426f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(11.40921f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(8.89276f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(6.40585f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(4.1219f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(2.21189f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(0.82864f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(0.09488f,25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(0f,25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(-13.6328f,5.6077f,5.1031f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel248(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.posVec(0f,0.00287f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.posVec(0f,0.01106f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.posVec(0f,0.02406f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.posVec(0f,0.04141f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.posVec(0f,0.06274f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.posVec(0f,0.0877f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.posVec(0f,0.11602f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.posVec(0f,0.14743f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.posVec(0f,0.18171f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.posVec(0f,0.21867f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.posVec(0f,0.25812f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.posVec(0f,0.2999f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.posVec(0f,0.34387f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.posVec(0f,0.38989f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.posVec(0f,0.43785f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.posVec(0f,0.48762f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.posVec(0f,0.53912f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.posVec(0f,0.59224f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.posVec(0f,0.64689f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.posVec(0f,0.703f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.posVec(0f,0.76049f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.posVec(0f,0.81928f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.posVec(0f,0.87932f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.posVec(0f,0.94052f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,1.00284f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,1.06621f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,1.13057f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,1.19588f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,1.26208f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,1.3291f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,1.39692f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,1.46547f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.posVec(0f,1.5347f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.posVec(0f,1.60455f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.posVec(0f,1.67499f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.posVec(0f,1.74594f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.posVec(0f,1.81736f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.posVec(0f,1.88919f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.posVec(0f,1.96135f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.posVec(0f,2.03378f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.posVec(0f,2.10639f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.posVec(0f,2.17912f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.posVec(0f,2.25185f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.posVec(0f,2.32446f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.posVec(0f,2.39683f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.posVec(0f,2.46879f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.posVec(0f,2.54013f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.posVec(0f,2.61058f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.posVec(0f,2.6798f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,2.7473f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,2.81236f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,2.87387f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,2.92985f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,2.97615f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,3f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel249(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.01116f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.04328f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.09457f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.16344f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.24856f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.3487f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.46281f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.58995f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.72928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.88003f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.04153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.21315f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(1.39434f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(1.58456f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.78334f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.99026f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(2.20492f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(2.42692f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(2.65593f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(2.89162f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(3.13371f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(3.3819f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(3.63594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(3.89556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(4.16053f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(4.43064f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(4.70569f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(4.98545f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(5.26975f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(5.55839f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(5.85121f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(6.14804f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(6.44872f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(6.75308f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(7.06099f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(7.37225f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(7.68678f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(8.0044f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(8.32498f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(8.64838f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(8.97445f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(9.30306f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(9.63405f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(9.96727f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(10.30258f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(10.63985f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(10.97887f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(11.31951f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(11.66158f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(12.00485f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(12.34916f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(12.69421f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(13.03978f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(13.38557f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(13.73123f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(14.07634f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(14.42045f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(14.76295f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(15.10313f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(15.44008f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(15.77256f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(16.0989f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(16.4167f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(16.72224f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(17.00932f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(17.26568f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(17.45977f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(17.79926f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(18.46929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(19.34288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(20.34431f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(21.4315f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(22.57803f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(23.76588f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(24.98196f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(26.2163f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(27.46089f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(28.70924f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(29.9557f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(31.19536f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(32.42372f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(33.63661f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(34.83014f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(36.00043f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(37.1436f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(38.2558f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(39.333f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(40.37094f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(41.36518f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(42.31084f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(43.20259f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(44.03454f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(44.80011f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(45.49176f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(46.10085f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(46.61731f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(47.02925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(47.32239f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(47.47925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(47.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel250(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel251(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.0191f,-0.02388f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.07375f,-0.09218f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.1604f,-0.2005f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.27609f,-0.34512f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.41825f,-0.52282f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.58469f,-0.73086f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.77346f,-0.96682f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.98287f,-1.22859f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(1.21143f,-1.51429f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(1.45781f,-1.82227f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.72081f,-2.15102f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.99936f,-2.49919f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(2.29247f,-2.86559f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(2.59928f,-3.2491f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(2.91899f,-3.64874f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(3.25081f,-4.06351f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(3.5941f,-4.49263f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(3.94824f,-4.9353f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(4.31262f,-5.39077f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(4.68668f,-5.85835f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(5.06993f,-6.33742f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(5.4619f,-6.82737f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(5.86212f,-7.32764f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(6.27013f,-7.83767f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(6.68557f,-8.35697f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(7.10805f,-8.88506f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(7.53716f,-9.42146f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(7.97254f,-9.96567f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(8.41383f,-10.51729f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(8.8607f,-11.07587f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(9.31279f,-11.64099f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(9.76978f,-12.21222f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(10.23131f,-12.78914f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(10.69703f,-13.37129f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(11.16658f,-13.95822f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(11.63961f,-14.54951f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(12.11576f,-15.1447f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(12.59457f,-15.74322f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(13.07565f,-16.34456f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(13.55851f,-16.94814f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(14.04262f,-17.55328f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(14.52746f,-18.15933f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(15.0123f,-18.76538f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(15.49641f,-19.37051f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(15.97888f,-19.9736f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(16.45858f,-20.57322f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(16.93417f,-21.16772f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(17.40386f,-21.75483f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(17.86532f,-22.33166f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(18.31531f,-22.89414f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(18.74908f,-23.43635f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(19.15915f,-23.94894f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(19.53235f,-24.41544f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(19.84099f,-24.80123f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(20f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(19.7339f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(18.94306f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(17.65269f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(15.91106f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(13.79426f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(11.40921f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(8.89276f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(6.40585f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(4.1219f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(2.21189f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(0.82864f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(0.09488f,-25f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(0f,-25f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2f,KeyframeAnimations.degreeVec(-13.6328f,-5.6077f,-5.1031f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel252(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.posVec(0f,0.00287f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.posVec(0f,0.01106f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.posVec(0f,0.02406f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.posVec(0f,0.04141f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.posVec(0f,0.06274f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.posVec(0f,0.0877f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.posVec(0f,0.11602f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.posVec(0f,0.14743f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.posVec(0f,0.18171f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.posVec(0f,0.21867f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.posVec(0f,0.25812f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.posVec(0f,0.2999f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.posVec(0f,0.34387f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.posVec(0f,0.38989f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.posVec(0f,0.43785f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.posVec(0f,0.48762f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.posVec(0f,0.53912f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.posVec(0f,0.59224f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.posVec(0f,0.64689f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.posVec(0f,0.703f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.posVec(0f,0.76049f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.posVec(0f,0.81928f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.posVec(0f,0.87932f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.posVec(0f,0.94052f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.posVec(0f,1.00284f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.posVec(0f,1.06621f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.posVec(0f,1.13057f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.posVec(0f,1.19588f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.posVec(0f,1.26208f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.posVec(0f,1.3291f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.posVec(0f,1.39692f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.posVec(0f,1.46547f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.posVec(0f,1.5347f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.posVec(0f,1.60455f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.posVec(0f,1.67499f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.posVec(0f,1.74594f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.posVec(0f,1.81736f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.posVec(0f,1.88919f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.posVec(0f,1.96135f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.posVec(0f,2.03378f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.posVec(0f,2.10639f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.posVec(0f,2.17912f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.posVec(0f,2.25185f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.posVec(0f,2.32446f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.posVec(0f,2.39683f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.posVec(0f,2.46879f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.posVec(0f,2.54013f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.posVec(0f,2.61058f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.posVec(0f,2.6798f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.posVec(0f,2.7473f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.posVec(0f,2.81236f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.posVec(0f,2.87387f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.posVec(0f,2.92985f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.posVec(0f,2.97615f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.posVec(0f,3f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel253(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.01116f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.04328f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.09457f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.16344f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.24856f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.3487f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.46281f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.58995f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.72928f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.88003f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.04153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.21315f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(1.39434f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(1.58456f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.78334f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.99026f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(2.20492f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(2.42692f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(2.65593f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(2.89162f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(3.13371f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(3.3819f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(3.63594f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(3.89556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(4.16053f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(4.43064f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(4.70569f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(4.98545f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(5.26975f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(5.55839f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(5.85121f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(6.14804f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(6.44872f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(6.75308f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(7.06099f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(7.37225f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(7.68678f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(8.0044f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(8.32498f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(8.64838f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(8.97445f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(9.30306f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(9.63405f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(9.96727f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(10.30258f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(10.63985f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(10.97887f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(11.31951f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(11.66158f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(12.00485f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(12.34916f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(12.69421f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(13.03978f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(13.38557f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(13.73123f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(14.07634f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(14.42045f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(14.76295f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(15.10313f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(15.44008f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(15.77256f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(16.0989f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(16.4167f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(16.72224f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(17.00932f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(17.26568f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(17.45977f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.35f,KeyframeAnimations.degreeVec(17.5f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.37f,KeyframeAnimations.degreeVec(17.79926f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.39f,KeyframeAnimations.degreeVec(18.46929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.41f,KeyframeAnimations.degreeVec(19.34288f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.43f,KeyframeAnimations.degreeVec(20.34431f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.45f,KeyframeAnimations.degreeVec(21.4315f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.47f,KeyframeAnimations.degreeVec(22.57803f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.49f,KeyframeAnimations.degreeVec(23.76588f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.51f,KeyframeAnimations.degreeVec(24.98196f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.53f,KeyframeAnimations.degreeVec(26.2163f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(27.46089f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.57f,KeyframeAnimations.degreeVec(28.70924f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.59f,KeyframeAnimations.degreeVec(29.9557f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.61f,KeyframeAnimations.degreeVec(31.19536f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.63f,KeyframeAnimations.degreeVec(32.42372f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.65f,KeyframeAnimations.degreeVec(33.63661f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.67f,KeyframeAnimations.degreeVec(34.83014f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.69f,KeyframeAnimations.degreeVec(36.00043f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.71f,KeyframeAnimations.degreeVec(37.1436f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.73f,KeyframeAnimations.degreeVec(38.2558f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.75f,KeyframeAnimations.degreeVec(39.333f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.77f,KeyframeAnimations.degreeVec(40.37094f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.79f,KeyframeAnimations.degreeVec(41.36518f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.81f,KeyframeAnimations.degreeVec(42.31084f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.83f,KeyframeAnimations.degreeVec(43.20259f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.85f,KeyframeAnimations.degreeVec(44.03454f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.87f,KeyframeAnimations.degreeVec(44.80011f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.89f,KeyframeAnimations.degreeVec(45.49176f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.91f,KeyframeAnimations.degreeVec(46.10085f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.93f,KeyframeAnimations.degreeVec(46.61731f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.95f,KeyframeAnimations.degreeVec(47.02925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.97f,KeyframeAnimations.degreeVec(47.32239f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.99f,KeyframeAnimations.degreeVec(47.47925f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(2f,KeyframeAnimations.degreeVec(47.5f,0f,0f),AnimationChannel.Interpolations.CATMULLROM));}

private static AnimationChannel channel254(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel255(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0f,0.02594f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0f,0.09989f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0f,0.21679f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0f,0.37241f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0f,0.56315f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0f,0.78591f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0f,1.03799f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0f,1.31708f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0f,1.6211f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0f,1.94822f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(0f,2.29681f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(0f,2.66537f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(0f,3.05264f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(0f,3.45737f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(0f,3.87848f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(0f,4.31494f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(0f,4.76585f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(0f,5.23033f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(0f,5.7076f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(0f,6.19688f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(0f,6.6975f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(0f,7.20876f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(0f,7.73002f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(0f,8.26067f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,8.80015f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(0f,9.34791f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(0f,9.90331f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(0f,10.46584f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(0f,11.03499f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(0f,11.61016f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(0f,12.19078f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(0f,12.77632f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(0f,13.36614f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(0f,13.95964f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(0f,14.5561f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(0f,15.15478f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(0f,15.75487f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(0f,16.35542f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(0f,16.95539f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(0f,17.55351f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(0f,18.14827f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(0f,18.73778f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(0f,19.31971f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(0f,19.89086f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(0f,20.44696f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(0f,20.98171f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(0f,21.48531f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(0f,21.94071f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(0f,22.31291f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-0.55701f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-1.86559f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-3.62344f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-5.6755f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-7.92894f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-10.32247f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-12.81261f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(-15.36672f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(-17.95891f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(-20.56809f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(-23.17617f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(-25.76731f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(-28.32678f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(-30.84095f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(-33.29668f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(-35.68069f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(-37.97979f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.36f,KeyframeAnimations.degreeVec(-40.17991f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.38f,KeyframeAnimations.degreeVec(-42.26641f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-44.22328f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.42f,KeyframeAnimations.degreeVec(-46.03295f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.44f,KeyframeAnimations.degreeVec(-47.67572f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.46f,KeyframeAnimations.degreeVec(-49.12906f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.48f,KeyframeAnimations.degreeVec(-50.36704f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-51.35898f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.52f,KeyframeAnimations.degreeVec(-52.0681f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.54f,KeyframeAnimations.degreeVec(-52.44938f,22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-52.5f,22.5f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.05f,KeyframeAnimations.degreeVec(-32.5f,22.5f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel256(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel257(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.01153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.04439f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.09635f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.16552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.25029f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.34929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.46133f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.58537f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.72049f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.86587f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.0208f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.18461f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(1.35673f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(1.53661f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.72377f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.91775f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(2.11816f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(2.32459f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(2.53671f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(2.75417f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(2.97667f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(3.20389f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(3.43556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(3.67141f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(3.91118f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(4.15463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(4.40147f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(4.65149f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(4.90444f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(5.16007f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(5.41813f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(5.67836f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(5.94051f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(6.20429f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(6.46938f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(6.73546f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(7.00216f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(7.26908f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(7.53573f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(7.80156f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(8.0659f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(8.3279f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(8.58654f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(8.84038f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(9.08754f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(9.32521f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(9.54902f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(9.75143f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(9.91685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel258(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel259(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0f,-0.02594f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0f,-0.09989f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0f,-0.21679f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0f,-0.37241f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0f,-0.56315f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0f,-0.78591f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0f,-1.03799f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0f,-1.31708f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0f,-1.6211f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0f,-1.94822f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(0f,-2.29681f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(0f,-2.66537f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(0f,-3.05264f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(0f,-3.45737f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(0f,-3.87848f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(0f,-4.31494f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(0f,-4.76585f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(0f,-5.23033f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(0f,-5.7076f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(0f,-6.19688f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(0f,-6.6975f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(0f,-7.20876f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(0f,-7.73002f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(0f,-8.26067f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(0f,-8.80015f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(0f,-9.34791f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(0f,-9.90331f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(0f,-10.46584f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(0f,-11.03499f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(0f,-11.61016f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(0f,-12.19078f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(0f,-12.77632f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(0f,-13.36614f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(0f,-13.95964f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(0f,-14.5561f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(0f,-15.15478f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(0f,-15.75487f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(0f,-16.35542f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(0f,-16.95539f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(0f,-17.55351f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(0f,-18.14827f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(0f,-18.73778f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(0f,-19.31971f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(0f,-19.89086f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(0f,-20.44696f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(0f,-20.98171f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(0f,-21.48531f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(0f,-21.94071f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(0f,-22.31291f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(0f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.02f,KeyframeAnimations.degreeVec(-0.55701f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.04f,KeyframeAnimations.degreeVec(-1.86559f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.06f,KeyframeAnimations.degreeVec(-3.62344f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.08f,KeyframeAnimations.degreeVec(-5.6755f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.1f,KeyframeAnimations.degreeVec(-7.92894f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.12f,KeyframeAnimations.degreeVec(-10.32247f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.14f,KeyframeAnimations.degreeVec(-12.81261f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.16f,KeyframeAnimations.degreeVec(-15.36672f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.18f,KeyframeAnimations.degreeVec(-17.95891f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.2f,KeyframeAnimations.degreeVec(-20.56809f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.22f,KeyframeAnimations.degreeVec(-23.17617f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.24f,KeyframeAnimations.degreeVec(-25.76731f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.26f,KeyframeAnimations.degreeVec(-28.32678f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.28f,KeyframeAnimations.degreeVec(-30.84095f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.3f,KeyframeAnimations.degreeVec(-33.29668f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.32f,KeyframeAnimations.degreeVec(-35.68069f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.34f,KeyframeAnimations.degreeVec(-37.97979f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.36f,KeyframeAnimations.degreeVec(-40.17991f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.38f,KeyframeAnimations.degreeVec(-42.26641f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.4f,KeyframeAnimations.degreeVec(-44.22328f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.42f,KeyframeAnimations.degreeVec(-46.03295f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.44f,KeyframeAnimations.degreeVec(-47.67572f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.46f,KeyframeAnimations.degreeVec(-49.12906f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.48f,KeyframeAnimations.degreeVec(-50.36704f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.5f,KeyframeAnimations.degreeVec(-51.35898f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.52f,KeyframeAnimations.degreeVec(-52.0681f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.54f,KeyframeAnimations.degreeVec(-52.44938f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1.55f,KeyframeAnimations.degreeVec(-52.5f,-22.5f,0f),AnimationChannel.Interpolations.CATMULLROM),
new Keyframe(2.05f,KeyframeAnimations.degreeVec(-32.5f,-22.5f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel260(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel261(){return new AnimationChannel(AnimationChannel.Targets.ROTATION,
new Keyframe(0f,KeyframeAnimations.degreeVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.02f,KeyframeAnimations.degreeVec(0.01153f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.04f,KeyframeAnimations.degreeVec(0.04439f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.06f,KeyframeAnimations.degreeVec(0.09635f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.08f,KeyframeAnimations.degreeVec(0.16552f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.1f,KeyframeAnimations.degreeVec(0.25029f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.12f,KeyframeAnimations.degreeVec(0.34929f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.14f,KeyframeAnimations.degreeVec(0.46133f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.16f,KeyframeAnimations.degreeVec(0.58537f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.18f,KeyframeAnimations.degreeVec(0.72049f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.2f,KeyframeAnimations.degreeVec(0.86587f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.22f,KeyframeAnimations.degreeVec(1.0208f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.24f,KeyframeAnimations.degreeVec(1.18461f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.26f,KeyframeAnimations.degreeVec(1.35673f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.28f,KeyframeAnimations.degreeVec(1.53661f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.3f,KeyframeAnimations.degreeVec(1.72377f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.32f,KeyframeAnimations.degreeVec(1.91775f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.34f,KeyframeAnimations.degreeVec(2.11816f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.36f,KeyframeAnimations.degreeVec(2.32459f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.38f,KeyframeAnimations.degreeVec(2.53671f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.4f,KeyframeAnimations.degreeVec(2.75417f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.42f,KeyframeAnimations.degreeVec(2.97667f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.44f,KeyframeAnimations.degreeVec(3.20389f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.46f,KeyframeAnimations.degreeVec(3.43556f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.48f,KeyframeAnimations.degreeVec(3.67141f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.5f,KeyframeAnimations.degreeVec(3.91118f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.52f,KeyframeAnimations.degreeVec(4.15463f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.54f,KeyframeAnimations.degreeVec(4.40147f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.56f,KeyframeAnimations.degreeVec(4.65149f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.58f,KeyframeAnimations.degreeVec(4.90444f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.6f,KeyframeAnimations.degreeVec(5.16007f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.62f,KeyframeAnimations.degreeVec(5.41813f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.64f,KeyframeAnimations.degreeVec(5.67836f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.66f,KeyframeAnimations.degreeVec(5.94051f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.68f,KeyframeAnimations.degreeVec(6.20429f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.7f,KeyframeAnimations.degreeVec(6.46938f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.72f,KeyframeAnimations.degreeVec(6.73546f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.74f,KeyframeAnimations.degreeVec(7.00216f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.76f,KeyframeAnimations.degreeVec(7.26908f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.78f,KeyframeAnimations.degreeVec(7.53573f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.8f,KeyframeAnimations.degreeVec(7.80156f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.82f,KeyframeAnimations.degreeVec(8.0659f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.84f,KeyframeAnimations.degreeVec(8.3279f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.86f,KeyframeAnimations.degreeVec(8.58654f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.88f,KeyframeAnimations.degreeVec(8.84038f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.9f,KeyframeAnimations.degreeVec(9.08754f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.92f,KeyframeAnimations.degreeVec(9.32521f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.94f,KeyframeAnimations.degreeVec(9.54902f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.96f,KeyframeAnimations.degreeVec(9.75143f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(0.98f,KeyframeAnimations.degreeVec(9.91685f,0f,0f),AnimationChannel.Interpolations.LINEAR),
new Keyframe(1f,KeyframeAnimations.degreeVec(10f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationChannel channel262(){return new AnimationChannel(AnimationChannel.Targets.POSITION,
new Keyframe(0f,KeyframeAnimations.posVec(0f,0f,0f),AnimationChannel.Interpolations.LINEAR));}

private static AnimationDefinition death(){
return AnimationDefinition.Builder.withLength(2.05f)
.addAnimation("main",channel236())
.addAnimation("main",channel237())
.addAnimation("head_with_neck",channel238())
.addAnimation("head_with_neck",channel239())
.addAnimation("head_only",channel240())
.addAnimation("body_main",channel241())
.addAnimation("body_main",channel242())
.addAnimation("body_fat1",channel243())
.addAnimation("body_fat1",channel244())
.addAnimation("body_fat2",channel245())
.addAnimation("body_fat2",channel246())
.addAnimation("hand_r",channel247())
.addAnimation("hand_r",channel248())
.addAnimation("hand_r_1",channel249())
.addAnimation("hand_r_1",channel250())
.addAnimation("hand_l",channel251())
.addAnimation("hand_l",channel252())
.addAnimation("hand_l_1",channel253())
.addAnimation("hand_l_1",channel254())
.addAnimation("leg_r",channel255())
.addAnimation("leg_r",channel256())
.addAnimation("leg_r_1",channel257())
.addAnimation("leg_r_1",channel258())
.addAnimation("leg_l",channel259())
.addAnimation("leg_l",channel260())
.addAnimation("leg_l_1",channel261())
.addAnimation("leg_l_1",channel262())
.build();
}
}
