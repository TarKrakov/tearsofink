package com.tarkrakov.tearsofink.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldInstallations;
import com.tarkrakov.tearsofink.world.battlefield.BattlefieldWorldPlan;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Operator diagnostics of the ACTUAL saved-seed world field, never manual spawning. */
public final class BattlefieldWorldCommands {
    private BattlefieldWorldCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> literal() {
        return Commands.literal("field")
                .then(Commands.literal("locate").executes(ctx -> locate(ctx.getSource())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("approach").executes(ctx -> approach(ctx.getSource())));
    }

    private static BattlefieldWorldPlan.Field first(CommandSourceStack source) {
        ServerLevel level = ModDimensions.lands(source.getServer());
        if (level == null) return null;
        InkLandsLayout layout = InkLandsLayout.of(level);
        BattlefieldWorldPlan plan = BattlefieldWorldPlan.of(layout);
        return plan == null || plan.fields().isEmpty() ? null : plan.fields().getFirst();
    }

    private static int locate(CommandSourceStack source) {
        var field = first(source);
        if (field == null) {
            source.sendFailure(Component.literal("Поле пока не запланировано: проверьте подготовку сети храмов."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Древнее поле боя: X=" + field.x() + ", Z=" + field.z()
                + ". Начало следов у магистрали: X=" + field.roadX() + ", Z=" + field.roadZ()
                + ". Эпизодов в плане: " + field.placements().size() + "."), false);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = ModDimensions.lands(source.getServer());
        var field = first(source);
        if (level == null || field == null) return locate(source);
        boolean loaded = level.getChunkSource().getChunkNow(field.x() >> 4, field.z() >> 4) != null;
        boolean generated = loaded && BattlefieldInstallations.fieldReady(level, field);
        int count = BattlefieldInstallations.placedCount(level, field);
        int pathsBuilt = BattlefieldInstallations.routeCount(level, field);
        source.sendSuccess(() -> Component.literal("Поле " + field.x() + "," + field.z()
                + ": центральный чанк " + (loaded ? "загружен" : "не загружен")
                + "; доказательство генерации: " + (generated ? "найдено" : loaded ? "отсутствует" : "неизвестно")
                + "; композиции: " + count + "/" + field.placements().size()
                + "; связующие участки: " + pathsBuilt + "/" + BattlefieldInstallations.routeTotal()
                + ". Остальные размещаются только в безопасных загруженных чанках."), false);
        return 1;
    }

    private static int approach(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!ModDimensions.isLands(player.level())) {
            source.sendFailure(Component.literal("Сначала войдите в Чернильные Земли."));
            return 0;
        }
        var field = first(source);
        if (field == null) return locate(source);
        // Explicit OP teleport, never background-force-load on regular players.
        var ground = ModDimensions.onGround(player.serverLevel(), field.roadX(), field.roadZ());
        player.teleportTo(ground.x, ground.y, ground.z);
        source.sendSuccess(() -> Component.literal(
                "Вы у подхода к полю. Ищите следы, уходящие с магистрали в лес."), false);
        return 1;
    }
}
