package com.tarkrakov.tearsofink.entity;

/** Shared clock and equally spaced slots, independent of entity tick order. */
public final class MorokDanceOrbit {
    private MorokDanceOrbit() {}
    public static final int PERIOD=200;
    public static final double RADIUS=3.0, BOB=.18;
    public record Offset(double x,double y,double z){}
    public static Offset at(int slot,int size,double angle,int direction,long elapsed){
        if(size<3||size>4||slot<0||slot>=size||(direction!=1&&direction!=-1))
            throw new IllegalArgumentException("Invalid dance slot");
        double a=angle+2*Math.PI*(slot/(double)size+direction*Math.floorMod(elapsed,PERIOD)/(double)PERIOD);
        return new Offset(Math.cos(a)*RADIUS,Math.sin(a*2)*BOB,Math.sin(a)*RADIUS);
    }
}
