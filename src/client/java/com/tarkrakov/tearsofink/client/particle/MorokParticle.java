package com.tarkrakov.tearsofink.client.particle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
public final class MorokParticle extends TextureSheetParticle{
    private final SpriteSet sprites;private final boolean shard;private final float size;
    private MorokParticle(ClientLevel l,double x,double y,double z,double xd,double yd,double zd,SpriteSet sprites,boolean shard){
        super(l,x,y,z);this.sprites=sprites;this.shard=shard;this.xd=xd;this.yd=yd;this.zd=zd;
        lifetime=shard?16+random.nextInt(12):8+random.nextInt(7);size=shard?.045f+random.nextFloat()*.065f:.27f+random.nextFloat()*.18f;quadSize=size;
        friction=shard?.94f:.87f;gravity=shard?.015f:-.025f;hasPhysics=false;setColor(.42f+random.nextFloat()*.28f,.93f,1f);alpha=shard?.9f:.48f;setSpriteFromAge(sprites);
    }
    @Override public void tick(){super.tick();if(removed)return;setSpriteFromAge(sprites);float p=(float)age/lifetime;alpha=(shard?.95f:.52f)*(1-p);quadSize=size*(shard?1-p*.5f:1+p*.35f);if(shard){oRoll=roll;roll+=.12f;}}
    @Override protected int getLightColor(float tick){return LightTexture.FULL_BRIGHT;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public record Provider(SpriteSet sprites,boolean shard) implements ParticleProvider<SimpleParticleType>{
        @Override public Particle createParticle(SimpleParticleType t,ClientLevel l,double x,double y,double z,double xd,double yd,double zd){return new MorokParticle(l,x,y,z,xd,yd,zd,sprites,shard);}
    }
}
