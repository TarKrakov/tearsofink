package com.tarkrakov.tearsofink.compat.jaber;

import com.tarkrakov.tearsofink.TearsOfInk;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/** Server-side replacement for the fixed X+4 call item, only in the Ink Lands.
 * Reuse the public ownership/target maps of the reviewed 1.0.0 JAR, not its classes at compile time. */
final class JaberSummon {
    private static Map<UUID, UUID> owned;
    private static Map<UUID, Vec3> targets;
    private static Method clearExtras;
    private static boolean failed;
    private JaberSummon() {}

    @SuppressWarnings("unchecked")
    private static boolean connect() {
        if (owned != null && targets != null) return true;
        if (failed) return false;
        try {
            Class<?> type = Class.forName("ru.chaos.duck.jaber.JABER");
            Map<UUID, UUID> owners = (Map<UUID, UUID>) type.getField("OWNED").get(null);
            Map<UUID, Vec3> goals = (Map<UUID, Vec3>) type.getField("TARGETS").get(null);
            Method clear = type.getDeclaredMethod("clearExtras", UUID.class);
            if (!clear.trySetAccessible()) throw new IllegalAccessException("clearExtras");
            owned = owners; targets = goals; clearExtras = clear;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            failed = true;
            TearsOfInk.LOGGER.error("Cannot connect JABER 1.0.0 summon bridge", e);
            return false;
        }
    }

    private static Frog ownedFrog(ServerPlayer player) {
        UUID id = owned.get(player.getUUID());
        // Same live cross-level lookup as the original; recover a loaded tagged frog
        // after losing the in-memory owner map, without loading missing chunks.
        if (id != null) for (ServerLevel level : player.server.getAllLevels()) {
            if (level.getEntity(id) instanceof Frog frog && frog.isAlive() && JaberRouter.owned(frog, player)) return frog;
        }
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Frog frog && frog.isAlive() && JaberRouter.owned(frog, player)) return frog;
            }
        }
        return null;
    }

    static boolean call(ServerPlayer player) {
        if (!connect()) {
            player.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.unavailable"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        Frog old = ownedFrog(player);
        if (old != null && old.getPassengers().stream().anyMatch(p -> p != player)) {
            player.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.occupied"), true);
            return false;
        }
        boolean create = old == null || old.level() != level;
        Frog frog = create ? EntityType.FROG.create(level) : old;
        if (frog == null) return false;
        // Use the same scale as the active router, not the original transient scale 3.
        var scale = frog.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(2.0);
        Vec3 at = JaberRouter.summonPosition(player, frog);
        if (at == null) {
            player.displayClientMessage(Component.translatable("message.tears-of-ink.jaber.unsafe_call"), true);
            return false;
        }
        if (create) {
            frog.setCustomName(Component.literal("Жабер"));
            frog.setCustomNameVisible(true);
            frog.setPersistenceRequired();
            frog.addTag("jaber_owner_" + player.getUUID());
            frog.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
            if (!level.addFreshEntity(frog)) return false;
        }
        JaberRouter.bind(targets);
        JaberRouter.cancelOwner(player);
        // Do not discard the old frog until a replacement has actually spawned safely.
        if (create && old != null) {
            JaberRouter.forget(old);
            old.discard();
        }
        player.stopRiding();
        frog.getNavigation().stop();
        frog.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
        frog.setDeltaMovement(Vec3.ZERO);
        frog.resetFallDistance();
        JaberRouter.forget(frog);
        owned.put(player.getUUID(), frog.getUUID());
        try {
            clearExtras.invoke(null, frog.getUUID());
        } catch (ReflectiveOperationException e) {
            TearsOfInk.LOGGER.warn("Could not reset original JABER auxiliary state", e);
        }
        player.startRiding(frog, true);
        // No random destination; the router gives its /jaber prompt on the next AI tick.
        return true;
    }
}
