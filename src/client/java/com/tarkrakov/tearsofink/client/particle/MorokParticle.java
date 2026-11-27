package com.tarkrakov.tearsofink.client.particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** Scattered, fullbright motes and fragments. No fire animation or rising flame. */
public final class MorokParticle extends TextureSheetParticle{
    private final boolean shard;
    private final float size,brightness;
    private final float spin;
    private MorokParticle(ClientLevel l,double x,double y,double z,double xd,double yd,double zd,SpriteSet sprites,boolean shard){
        super(l,x,y,z);
        this.shard=shard;this.xd=xd;this.yd=yd;this.zd=zd;
        lifetime=shard?16+random.nextInt(12):12+random.nextInt(13);
        size=shard?.065f+random.nextFloat()*.085f:.18f+random.nextFloat()*.17f;
        brightness=shard?.93f:.75f+random.nextFloat()*.22f;
        quadSize=size;friction=.94f;gravity=0;hasPhysics=false;
        float pale=random.nextFloat();
        setColor(.52f+.42f*pale,.92f+.08f*pale,1f);
        alpha=brightness;
        setSprite(sprites.get(random));
        roll=random.nextFloat()*(float)(2*Math.PI);oRoll=roll;
        spin=(random.nextFloat()-.5f)*.12f;
    }
    @Override public void tick(){
        super.tick();if(removed)return;
        float p=(float)age/lifetime;
        alpha=brightness*(1-p*p);
        quadSize=size*(1-.6f*p);
        oRoll=roll;roll+=spin;
    }
    @Override protected int getLightColor(float tick){return LightTexture.FULL_BRIGHT;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public record Provider(SpriteSet sprites,boolean shard) implements ParticleProvider<SimpleParticleType>{
        @Override public Particle createParticle(SimpleParticleType t,ClientLevel l,double x,double y,double z,double xd,double yd,double zd){return new MorokParticle(l,x,y,z,xd,yd,zd,sprites,shard);}
    }
}
