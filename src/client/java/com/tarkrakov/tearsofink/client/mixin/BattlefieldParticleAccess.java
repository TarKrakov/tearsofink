package com.tarkrakov.tearsofink.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Access to the already-existing falling white-ash sprite, never its provider. */
@Mixin(Particle.class)
public interface BattlefieldParticleAccess {
    @Accessor("x") double toi$getX();
    @Accessor("z") double toi$getZ();
    @Accessor("alpha") void toi$setAlpha(float alpha);
}
