package com.tarkrakov.tearsofink.mixin;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CompoundContainer.class)
public interface PantheonCompoundContainerAccessor {
    @Accessor("container1") Container tearsofink$first();
    @Accessor("container2") Container tearsofink$second();
}
