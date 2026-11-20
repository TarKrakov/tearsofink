package com.tarkrakov.tearsofink.compat.jaber.mixin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tarkrakov.tearsofink.compat.jaber.JaberCompat;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.frog.Frog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "ru.chaos.duck.jaber.JABER", remap = false)
public abstract class JaberCommandMixin {
    // Exact lambda names from the supplied 1.0.0 JAR. Preserve /jaber x z in other dimensions.
    @Inject(method = "lambda$onInitialize$3", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$destination(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)
                || !JaberCompat.active(player.level()) || !(player.getVehicle() instanceof Frog)) return;
        int x = IntegerArgumentType.getInteger(context, "x"), z = IntegerArgumentType.getInteger(context, "z");
        if (!JaberCompat.inside(player.serverLevel(), x, z)) {
            context.getSource().sendFailure(Component.translatable("message.tears-of-ink.jaber.outside"));
            cir.setReturnValue(0);
        }
    }

    // The old world-tick steering still samples a surface 16 blocks ahead, although
    // FrogEntityMixin cancels the owned frog's vanilla brain and JaberDriver drives it.
    // Keep its dialogue/arrival/dismount logic. Only avoid the redundant distant height probe.
    @ModifyConstant(method = "lambda$onInitialize$8", constant = @Constant(doubleValue = 16.0), remap = false)
    private static double toi$legacyProbe(double reach, ServerLevel level) {
        return JaberCompat.active(level) ? 0.0 : reach;
    }
}
