package com.tarkrakov.tearsofink.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.weapon.ranged.RangedCombat;
import com.tarkrakov.tearsofink.weapon.ranged.RangedItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

/** The authored crossbow scene is a single model with articulated child parts, not a pair of weapons. */
public final class CrossbowAnims {
    private static final float SCALE=.68f, PX=1f/16f;
    private static final Vector3f GRIP=new Vector3f(-1.5f,6.5f,9.5f);
    private static final Vector3f HINGE=new Vector3f(5.25f,8.5f,5f);
    private static final Vector3f FP_R_ARM=new Vector3f(2.86f,3.30086f,-.04348f),
        FP_R_HAND=new Vector3f(2.86f,-6.69914f,-4.04348f),
        FP_L_ARM=new Vector3f(-1.14f,5.21871f,-6.36037f),
        FP_L_HAND=new Vector3f(2.86f,-4.78129f,-10.36037f);
    private static final Vector3f FP_SWORD=new Vector3f(90,85,90), F5_SWORD=new Vector3f(-90,21.25f,-90);
    private static final Vector3f F5_R_GUIDE=new Vector3f(37.21592f,-3.29292f,9.75924f);
    private static final Vector3f F5_L_GUIDE=new Vector3f(29.05828f,-9.06515f,34.0164f);
    private static final Map<Integer,State> states=new HashMap<>();
    private static final class State {
        float previousAim,aim;
        int shot,reload,max;
        long received;
    }
    private record Key(float time,Vector3f value,boolean smooth){}
    private record Frame(String clip,float time,boolean bolt,float boltRaise,boolean unloaded){}
    private static Map<String,Map<String,List<Key>>> clips;
    private static boolean loadFailed;
    private CrossbowAnims(){}
    public static boolean isCrossbow(ItemStack s){return RangedItem.kind(s)==RangedItem.Kind.CROSSBOW;}
    public static boolean holds(Player p){return isCrossbow(p.getMainHandItem());}
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(RangedCombat.CrossbowPose.TYPE,(v,ctx)->ctx.client().execute(()->{
            State s=states.computeIfAbsent(v.entity(),id->new State());
            s.shot=v.shotRemaining();s.reload=v.reloadTicks();s.max=v.reloadMax();s.received=System.nanoTime();
            if(s.shot>0||s.max>0)s.previousAim=s.aim=0;
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->states.clear());
        ClientTickEvents.END_CLIENT_TICK.register(mc->{
            if(mc.level==null){states.clear();return;}
            states.keySet().removeIf(id->!(mc.level.getEntity(id) instanceof Player p)||!holds(p)||!p.isAlive());
            for(Player p:mc.level.players()){
                if(!holds(p)||!p.isAlive())continue;
                State s=states.computeIfAbsent(p.getId(),id->new State());
                s.previousAim=s.aim;
                if(RangedCombat.loaded(p.getMainHandItem())&&RangedClient.aiming(p)&&!busy(s))
                    s.aim=Math.min(1,s.aim+1f/RangedCombat.CROSSBOW_AIM_TICKS);
                else s.aim=Math.max(0,s.aim-1f/RangedCombat.CROSSBOW_AIM_TICKS);
            }
        });
    }
    private static float elapsed(State s){return Math.max(0,(System.nanoTime()-s.received)/50_000_000f);}
    private static boolean busy(State s){float e=elapsed(s);return s.shot>e||s.max>0&&s.reload+e<s.max&&e<20;}
    public static boolean canFire(Player p){State s=states.get(p.getId());return s!=null&&s.aim>=.999f&&!busy(s)&&RangedCombat.loaded(p.getMainHandItem());}
    private static Frame frame(Player p,float partial){
        State s=states.get(p.getId());boolean loaded=RangedCombat.loaded(p.getMainHandItem());
        if(s!=null){
            float e=elapsed(s);
            if(s.shot>e)return new Frame("shoot",Math.clamp((RangedCombat.CROSSBOW_SHOT_TICKS-s.shot+e)/20f,0,.35f),false,0,false);
            if(s.max>0&&e<20&&!loaded){
                float t=Math.clamp((s.reload+e)/s.max*3.5f,0,3.5f);
                return new Frame("reload",t,t>=2.75f,Math.max(0,(3.25f-t)*6),false);
            }
            if(loaded){float aim=Mth.lerp(partial,s.previousAim,s.aim);if(aim>0)return new Frame("aim",aim*.25f,true,0,false);}
        }
        return new Frame("idle",0,loaded,0,!loaded);
    }
    private static boolean load(){
        if(clips!=null)return true;if(loadFailed)return false;
        try(var reader=Minecraft.getInstance().getResourceManager().getResource(TearsOfInk.id("weapon_anims/ink_crossbow_parts.json")).orElseThrow().openAsReader()){
            JsonObject root=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
            Map<String,Map<String,List<Key>>> result=new HashMap<>();
            for(var c:root.entrySet()){
                Map<String,List<Key>> tracks=new HashMap<>();
                for(var bone:c.getValue().getAsJsonObject().getAsJsonObject("bones").entrySet())
                    for(var channel:bone.getValue().getAsJsonObject().entrySet()){
                        if(!channel.getKey().equals("rotation")&&!channel.getKey().equals("position"))continue;
                        boolean rotation=channel.getKey().equals("rotation");
                        List<Key> keys=new ArrayList<>();JsonElement value=channel.getValue();
                        if(value.isJsonArray())keys.add(new Key(0,vector(value,rotation),false));
                        else if(value.isJsonObject())for(var k:value.getAsJsonObject().entrySet()){
                            JsonElement v=k.getValue();boolean smooth=false;
                            if(v.isJsonObject()){JsonObject o=v.getAsJsonObject();smooth=o.has("lerp_mode")&&o.get("lerp_mode").getAsString().equals("catmullrom");v=o.has("post")?o.get("post"):o.get("pre");}
                            keys.add(new Key(Float.parseFloat(k.getKey()),vector(v,rotation),smooth));
                        }
                        keys.sort(Comparator.comparingDouble(Key::time));
                        tracks.put(bone.getKey()+"/"+channel.getKey(),keys);
                    }
                result.put(c.getKey(),tracks);
            }
            if(!result.keySet().containsAll(List.of("idle","aim","shoot","reload")))throw new IllegalArgumentException("Missing crossbow clip");
            clips=result;return true;
        }catch(Exception ex){loadFailed=true;org.slf4j.LoggerFactory.getLogger("TearsOfInk/Crossbow").error("Crossbow animation load failed; retaining static model",ex);return false;}
    }
    /** Bedrock export mirrors X position and X/Y rotations relative to the BB editor. */
    private static Vector3f vector(JsonElement e,boolean rotation){JsonArray v=e.getAsJsonArray();return new Vector3f(-v.get(0).getAsFloat(),(rotation?-1:1)*v.get(1).getAsFloat(),v.get(2).getAsFloat());}
    private static Vector3f sample(Frame f,String bone,String channel){
        List<Key> k=clips.get(f.clip).get(bone+"/"+channel);
        if(k==null||k.isEmpty())return new Vector3f();
        if(f.time<=k.getFirst().time)return new Vector3f(k.getFirst().value);
        if(f.time>=k.getLast().time)return new Vector3f(k.getLast().value);
        int i=0;while(i+1<k.size()&&k.get(i+1).time<f.time)i++;
        Key a=k.get(i),b=k.get(i+1);float u=(f.time-a.time)/(b.time-a.time);
        if(!a.smooth&&!b.smooth)return new Vector3f(a.value).lerp(b.value,u);
        Vector3f p0=k.get(Math.max(0,i-1)).value,p1=a.value,p2=b.value,p3=k.get(Math.min(k.size()-1,i+2)).value;
        return new Vector3f(p1).mul(2).add(new Vector3f(p2).sub(p0).mul(u))
            .add(new Vector3f(p0).mul(2).sub(new Vector3f(p1).mul(5)).add(new Vector3f(p2).mul(4)).sub(p3).mul(u*u))
            .add(new Vector3f(p0).negate().add(new Vector3f(p1).mul(3)).sub(new Vector3f(p2).mul(3)).add(p3).mul(u*u*u)).mul(.5f);
    }
    private static Quaternionf q(Vector3f d){return new Quaternionf().rotationZYX(d.z*Mth.DEG_TO_RAD,d.y*Mth.DEG_TO_RAD,d.x*Mth.DEG_TO_RAD);}
    private static Vector3f pos(Frame f,String bone){return sample(f,bone,"position");}
    private static Vector3f rot(Frame f,String bone){return sample(f,bone,"rotation");}
    private static void translate(PoseStack p,Vector3f v){p.translate(v.x*PX,v.y*PX,v.z*PX);}
    private static void renderPart(Player player,Item part,PoseStack pose,MultiBufferSource buffers,int light){
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(part),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,player.level(),player.getId());
    }
    private static void pivot(PoseStack pose,Vector3f v){pose.translate((v.x-8)*PX,(v.y-8)*PX,(v.z-8)*PX);}
    private static void unpivot(PoseStack pose,Vector3f v){pose.translate((8-v.x)*PX,(8-v.y)*PX,(8-v.z)*PX);}
    private static void string(Player p,PoseStack pose,MultiBufferSource buffers,int light,float pull,boolean left){
        float sign=left?1:-1;
        Vector3f start=new Vector3f(8.125f,12.125f,left?2:17);
        Vector3f end=new Vector3f(8.25f+pull,12.125f,left?9:10);
        Vector3f delta=end.sub(start);float length=delta.length();
        pose.pushPose();
        try{
            pivot(pose,start);
            pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0,0,sign),delta.normalize()));
            pose.scale(1,1,length/7f);unpivot(pose,start);
            renderPart(p,left?ModItems.CROSSBOW_PART_STRING_LEFT:ModItems.CROSSBOW_PART_STRING_RIGHT,pose,buffers,light);
        }finally{pose.popPose();}
    }
    private static void weapon(Player player,Frame f,String view,PoseStack pose,MultiBufferSource buffers,int light){
        renderPart(player,ModItems.CROSSBOW_PART_BODY,pose,buffers,light);
        pose.pushPose();try{pivot(pose,HINGE);pose.mulPose(q(rot(f,view+"_lever")));unpivot(pose,HINGE);renderPart(player,ModItems.CROSSBOW_PART_LEVER,pose,buffers,light);}finally{pose.popPose();}
        float pull=f.unloaded?0:pos(f,view+"_string_center").x/(view.equals("FP")?SCALE:1);
        string(player,pose,buffers,light,pull,true);string(player,pose,buffers,light,pull,false);
        pose.pushPose();try{pose.translate(pull*PX,0,0);renderPart(player,ModItems.CROSSBOW_PART_STRING_CENTER,pose,buffers,light);}finally{pose.popPose();}
        if(f.bolt){
            pose.pushPose();try{pose.translate(0,f.boltRaise*PX,0);renderPart(player,ModItems.INK_CROSSBOW_BOLT_VISUAL,pose,buffers,light);}finally{pose.popPose();}
        }
    }
    public static boolean renderFirst(AbstractClientPlayer player,ItemStack stack,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(!holds(player)||!load())return false;
        Frame f=frame(player,partial);
        Quaternionf arm=q(rot(f,"FP_right_arm"));Vector3f shoulder=new Vector3f(FP_R_ARM).add(pos(f,"FP_right_arm"));
        Vector3f grip=new Vector3f(FP_R_HAND).sub(FP_R_ARM).add(pos(f,"FP_right_sword"));arm.transform(grip);grip.add(shoulder);
        Quaternionf sword=new Quaternionf(arm).mul(q(new Vector3f(FP_SWORD).add(rot(f,"FP_right_sword"))));
        Quaternionf support=q(rot(f,"FP_left_arm"));Vector3f supportShoulder=new Vector3f(FP_L_ARM).add(pos(f,"FP_left_arm"));
        Vector3f supportHand=new Vector3f(FP_L_HAND).sub(FP_L_ARM);support.transform(supportHand);supportHand.add(supportShoulder);
        boolean left=player.getMainArm()==HumanoidArm.LEFT;
        pose.pushPose();
        try{
            if(left)pose.scale(-1,1,1);
            if(!player.isInvisible()){
                drawArm(player,left?HumanoidArm.LEFT:HumanoidArm.RIGHT,shoulder,grip,pose,buffers,light);
                drawArm(player,left?HumanoidArm.RIGHT:HumanoidArm.LEFT,supportShoulder,supportHand,pose,buffers,light);
            }
            pose.pushPose();
            try{translate(pose,grip);pose.mulPose(sword);pose.scale(SCALE,SCALE,SCALE);translate(pose,new Vector3f(8,8,8).sub(GRIP));weapon(player,f,"FP",pose,buffers,light);}finally{pose.popPose();}
        }finally{pose.popPose();}
        return true;
    }
    private static void drawArm(AbstractClientPlayer player,HumanoidArm side,Vector3f start,Vector3f end,PoseStack pose,MultiBufferSource buffers,int light){
        if(!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer))return;
        var model=renderer.getModel();boolean left=side==HumanoidArm.LEFT;
        ModelPart arm=left?model.leftArm:model.rightArm,sleeve=left?model.leftSleeve:model.rightSleeve;
        var armSave=arm.storePose();var sleeveSave=sleeve.storePose();
        Vector3f delta=new Vector3f(end).sub(start);
        if(delta.lengthSquared()<.01f)return;
        float len=delta.length();delta.normalize();
        pose.pushPose();
        try{
            translate(pose,start);pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),delta));pose.scale(1,len/8f,1);
            arm.setPos(0,0,0);arm.xRot=arm.yRot=arm.zRot=0;
            sleeve.setPos(0,0,0);sleeve.xRot=sleeve.yRot=sleeve.zRot=0;
            var skin=player.getSkin().texture();
            arm.render(pose,buffers.getBuffer(RenderType.entitySolid(skin)),light,OverlayTexture.NO_OVERLAY);
            if(player.isModelPartShown(left?PlayerModelPart.LEFT_SLEEVE:PlayerModelPart.RIGHT_SLEEVE))
                sleeve.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(skin)),light,OverlayTexture.NO_OVERLAY);
        }finally{arm.loadPose(armSave);sleeve.loadPose(sleeveSave);pose.popPose();}
    }
    public static void pose(HumanoidModel<?> model,Player p){
        if(!load())return;
        Frame f=frame(p,1);
        boolean left=p.getMainArm()==HumanoidArm.LEFT;
        applyArm(left?model.leftArm:model.rightArm,rot(f,"F5_right_arm"),F5_R_GUIDE,left,model,f);
        applyArm(left?model.rightArm:model.leftArm,rot(f,"F5_left_arm"),F5_L_GUIDE,left,model,f);
    }
    private static void applyArm(ModelPart part,Vector3f anim,Vector3f guide,boolean mirror,HumanoidModel<?> model,Frame f){
        Quaternionf convert=new Quaternionf().rotationZ(Mth.PI);
        Quaternionf orientation=new Quaternionf(convert).mul(q(anim)).mul(q(guide)).mul(new Quaternionf(convert).conjugate());
        Vector3f angles=orientation.getEulerAnglesZYX(new Vector3f());
        float aim=f.clip.equals("aim")?f.time/.25f:f.clip.equals("shoot")?1:0;
        part.xRot=angles.x+model.head.xRot*aim;part.yRot=(mirror?-angles.y:angles.y)+model.head.yRot*aim;part.zRot=mirror?-angles.z:angles.z;
    }
    public static boolean renderThird(Player player,ItemStack stack,ItemDisplayContext context,boolean left,PoseStack pose,MultiBufferSource buffers,int light){
        if(!holds(player)||!load())return false;
        Frame f=frame(player,1);
        var items=Minecraft.getInstance().getItemRenderer();
        var transform=items.getModel(stack,player.level(),player,player.getId()).getTransforms().getTransform(context);
        pose.pushPose();
        try{
            transform.apply(left,pose);
            Vector3f gr=new Vector3f(GRIP);
            if(left)pose.scale(-1,1,1);
            Quaternionf delta=q(F5_SWORD).conjugate().mul(q(new Vector3f(F5_SWORD).add(rot(f,"F5_right_sword"))));
            pivot(pose,gr);pose.mulPose(delta);unpivot(pose,gr);
            weapon(player,f,"F5",pose,buffers,light);
        }finally{pose.popPose();}
        return true;
    }
}
