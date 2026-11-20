package com.tarkrakov.tearsofink.compat.jaber.mixin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tarkrakov.tearsofink.compat.jaber.JaberCompat;
import com.tarkrakov.tearsofink.compat.jaber.JaberRouter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(targets = "ru.chaos.duck.jaber.JABER", remap = false)
public abstract class JaberCommandMixin {
    @Shadow @Final public static Map<UUID, Vec3> TARGETS;

    @Inject(method = "lambda$onInitialize$3", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$destination(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)
                || !JaberCompat.active(player.level()) || !(player.getVehicle() instanceof Frog frog)) return;
        if (!JaberRouter.owned(frog, player)) {
            context.getSource().sendFailure(Component.translatable("message.tears-of-ink.jaber.not_owner"));
            cir.setReturnValue(0); return;
        }
        int x = IntegerArgumentType.getInteger(context, "x"), z = IntegerArgumentType.getInteger(context, "z");
        if (!JaberCompat.inside(player.serverLevel(), x, z)) {
            context.getSource().sendFailure(Component.translatable("message.tears-of-ink.jaber.outside"));
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "lambda$onInitialize$3", at = @At("RETURN"), remap = false)
    private static void toi$start(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValue() != 1 || !(context.getSource().getEntity() instanceof ServerPlayer player)
                || !JaberCompat.active(player.level()) || !(player.getVehicle() instanceof Frog frog)
                || !JaberRouter.owned(frog, player)) return;
        JaberRouter.command(frog, player, IntegerArgumentType.getInteger(context, "x"),
                IntegerArgumentType.getInteger(context, "z"), TARGETS);
    }

    /** The original second driver intentionally makes wrong turns and ejects riders.
     * It must not compete with the Ink Lands router. Outside the Lands it is untouched. */
    @Inject(method = "lambda$onInitialize$8", at = @At("HEAD"), cancellable = true, remap = false)
    private static void toi$oneDriver(ServerLevel level, CallbackInfo ci) {
        if (!JaberCompat.active(level)) return;
        JaberRouter.bind(TARGETS);
        ci.cancel();
    }
}
