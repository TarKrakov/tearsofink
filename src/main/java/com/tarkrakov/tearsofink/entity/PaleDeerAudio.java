package com.tarkrakov.tearsofink.entity;

import com.tarkrakov.tearsofink.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import java.util.Random;

/** D-076: server-only positional vocalizations and authored clip contact markers. */
final class PaleDeerAudio {
    private final Random random=new Random();
    private int voiceIn=180+random.nextInt(420), snortIn=120+random.nextInt(320), alertIn;
    private long stamp=Long.MIN_VALUE, previousMs;
    private boolean suspicious;

    void tick(PaleDeerEntity deer,int herdSize,boolean suspect,double movedSq) {
        long next=deer.phaseStamp();
        var phase=deer.phase();
        int age=deer.phaseAge();
        boolean changed=next!=stamp;
        // Same sampling formula as PaleDeerModel; long age avoids the phaseAge() clamp during long runs.
        long elapsed=Math.max(0L,deer.level().getGameTime()-PaleDeerEntity.startTick(next));
        long ms=(phase==PaleDeerEntity.Phase.RUN||phase==PaleDeerEntity.Phase.WALK)
                ? (long)(elapsed*50f*deer.individualTempo())+deer.runOffsetMs() : elapsed*50L;
        long old=changed?ms:previousMs;
        stamp=next; previousMs=ms;
        boolean newlySuspicious=suspect&&!suspicious; suspicious=suspect;
        if (voiceIn>0) --voiceIn;
        if (snortIn>0) --snortIn;
        if (alertIn>0) --alertIn;
        if (deer.isSilent()||!deer.isAlive()||deer.isNoAi()) return;
        float group=(float)Math.max(.55,Math.sqrt(3.0/Math.max(3,herdSize)));
        // Roars are one-shots; no repeat on an unchanged clip or restored mid-clip.
        if (phase==PaleDeerEntity.Phase.WATCH_ROAR||phase==PaleDeerEntity.Phase.ROAR) {
            if (changed&&age==0) {
                boolean watch=phase==PaleDeerEntity.Phase.WATCH_ROAR;
                play(deer,watch?ModSounds.PALE_DEER_WATCH_ROAR:ModSounds.PALE_DEER_ROAR,
                        watch?.82f: .62f*group,1.0f,true);
            }
            return;
        }
        boolean moving=deer.onGround()&&!deer.isInWater()&&!deer.isPassenger()&&movedSq>.0004&&movedSq<=1.0;
        if (phase==PaleDeerEntity.Phase.RUN) {
            // Forefeet reach the ground around .75, hindfeet at the cycle seam.
            if (!changed&&moving&&(crossed(old,ms,1000,750)||crossed(old,ms,1000,0)))
                play(deer,ModSounds.PALE_DEER_RUN_STEP,.48f*group,1.02f,false);
            return;
        }
        if (phase==PaleDeerEntity.Phase.WALK&&!changed&&moving
                &&(crossed(old,ms,2000,500)||crossed(old,ms,2000,1500)))
            play(deer,ModSounds.PALE_DEER_STEP,.36f*group,1.0f,false);
        if (suspect) {
            if (newlySuspicious&&alertIn==0) {
                play(deer,ModSounds.PALE_DEER_ALERT_SNORT,.54f*group,1f,true);
                alertIn=100;
            }
            return;
        }
        if (phase==PaleDeerEntity.Phase.WATCH_START||phase==PaleDeerEntity.Phase.WATCH_END) return;
        if (phase==PaleDeerEntity.Phase.EATING) {
            if (!changed&&(crossed(old,ms,7000,1350)||crossed(old,ms,7000,2350)
                    ||crossed(old,ms,7000,4000)||crossed(old,ms,7000,5000)))
                play(deer,ModSounds.PALE_DEER_EAT,.28f*group,1f,true);
            return;
        }
        if (voiceIn==0) {
            play(deer,ModSounds.PALE_DEER_VOICE,.54f*group,1f,true);
            voiceIn=360+random.nextInt(601);
            snortIn=Math.max(snortIn,40);
        } else if (snortIn==0) {
            play(deer,ModSounds.PALE_DEER_SNORT,.42f*group,1f,true);
            snortIn=220+random.nextInt(381);
        }
    }
    private static boolean crossed(long a,long b,long cycle,long marker) {
        // Discard gaps (e.g. tick resumed after unloading) rather than replaying missed steps.
        return b>a&&b-a<=100&&Math.floorDiv(a-marker,cycle)!=Math.floorDiv(b-marker,cycle);
    }
    private void play(PaleDeerEntity d,SoundEvent event,float volume,float pitch,boolean head) {
        float individual=.96f+Math.floorMod(d.getUUID().hashCode(),81)/1000f;
        // Fixed range affects packet delivery, sounds.json controls client attenuation.
        d.level().playSound(null,d.getX(),d.getY()+(head?d.getBbHeight()*.65:.12),d.getZ(),
                event,SoundSource.NEUTRAL,volume,pitch*individual);
    }
}
