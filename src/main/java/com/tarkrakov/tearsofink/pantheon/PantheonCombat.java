package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.mixin.PantheonPlayerListAccessor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static com.tarkrakov.tearsofink.pantheon.PantheonArenaScenes.Scene.NEUTRAL;

/** One server-owned cooperative trial in the existing temple, with an abort-only restart policy. */
public final class PantheonCombat {
    private static final int ENTRY_WINDOW = 10 * 20;
    private static final int EMPTY_TIMEOUT = 120 * 20;
    private static final int TRANSITION_TIMEOUT = 60 * 20;
    private static final int PASSAGE_Z = 39;
    private static final int HUB_LIMIT_Z = 19;
    private static final String ENTITY_TAG = "toi.pantheon.session.";
    private static final String TARGET_TAG = "toi.pantheon.target";
    private static final String LANG = "message.tears-of-ink.pantheon.combat.";

    private enum Phase { PREPARING, OPEN, FIGHT, WON, FAILED, CLEANUP, ERROR }

    private static final class Session {
        final UUID id, initiator;
        final PantheonTrial trial;
        final UUID target;
        final Set<UUID> members = new LinkedHashSet<>();
        final Set<UUID> eliminated = new LinkedHashSet<>();
        Phase phase = Phase.PREPARING;
        long deadline;
        long entryEnds;
        long deathTick = Long.MIN_VALUE;
        boolean restoring;
        boolean cleared;
        String reason = "";

        Session(UUID id, UUID initiator, PantheonTrial trial, UUID target) {
            this.id = id;
            this.initiator = initiator;
            this.trial = trial;
            this.target = target;
        }
    }

    private static Session session;
    private static final Set<UUID> pendingHub = new LinkedHashSet<>();
    private static String recoveryError;
    private static String lastResult;
    private static boolean pendingDirty;

    private PantheonCombat() {}

    static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(Commands.literal("tearsofink").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("pantheon").then(Commands.literal("combat")
                                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                                .then(Commands.literal("abort").executes(ctx -> abort(ctx.getSource())))))));
        ServerLifecycleEvents.SERVER_STARTED.register(PantheonCombat::recover);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            session = null;
            pendingHub.clear();
            pendingDirty = false;
            recoveryError = null;
            lastResult = null;
        });
        ServerTickEvents.START_WORLD_TICK.register(level -> {
            if (active() && PantheonTemple.isTemple(level)) enforcePlayers(level);
        });
        ServerTickEvents.END_SERVER_TICK.register(PantheonCombat::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onArrival(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forfeit(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((previous, next, alive) -> onArrival(next));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (PantheonTemple.isTemple(origin)) forfeit(player);
            if (PantheonTemple.isTemple(destination)) onArrival(player);
        });
        ServerEntityEvents.ENTITY_LOAD.register(PantheonCombat::entityLoaded);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> allowDamage(victim, source));
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            if (isTrialTarget(victim) && session != null && session.target.equals(victim.getUUID())
                    && session.phase == Phase.FIGHT) session.deathTick = victim.level().getGameTime();
        });
    }

    static boolean active() { return session != null || recoveryError != null; }
    static boolean holdsGate() { return active() && (session == null || session.phase != Phase.OPEN); }

    static void interact(ServerPlayer player, ServerLevel level, boolean cancel) {
        if (cancel && session != null && session.initiator.equals(player.getUUID())
                && (session.phase == Phase.PREPARING || session.phase == Phase.OPEN)) {
            finish(level, false, "cancelled");
        } else {
            player.sendSystemMessage(Component.translatable(LANG + "busy"));
        }
    }

    static void start(ServerPlayer player, ServerLevel level, PantheonTrial trial) {
        if (trial == null || active() || !eligible(player) || !PantheonArena.neutralReady()) {
            player.sendSystemMessage(Component.translatable(LANG + "busy"));
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            player.sendSystemMessage(Component.translatable(LANG + "error", "Peaceful"));
            return;
        }
        session = new Session(UUID.randomUUID(), player.getUUID(), trial, UUID.randomUUID());
        phase(level, Phase.PREPARING, TRANSITION_TIMEOUT);
        if (!persist(level)) return;
        String problem = PantheonArena.changeScene(level, trial.scene());
        if (problem != null) {
            error(level, problem);
            return;
        }
        PantheonArena.retainCombatArea(level, true);
        announce(level, "preparing");
    }

    private static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(PantheonTemple.DIMENSION);
        if (level == null) return;
        if (session == null) {
            if (recoveryError != null && PantheonTemple.ready(level)) PantheonArena.closeCombatGate(level);
            if (pendingDirty && recoveryError == null) persist(level);
            return;
        }
        long now = level.getGameTime();
        switch (session.phase) {
            case PREPARING -> {
                if (PantheonArena.ready(session.trial.scene())) {
                    try {
                        open(level);
                    } catch (RuntimeException ex) {
                        TearsOfInk.LOGGER.error("Pantheon target creation failed", ex);
                        error(level, "target creation failed");
                    }
                }
                else if (PantheonArena.failure() != null) error(level, PantheonArena.failure());
                else if (now >= session.deadline) error(level, "preparing timeout");
            }
            case OPEN -> tickOpen(level, now);
            case FIGHT -> tickFight(level);
            case WON, FAILED -> beginCleanup(level);
            case CLEANUP -> tickCleanup(level, now);
            case ERROR -> { }
        }
    }

    private static void open(ServerLevel level) {
        Mob mob = session.trial.create().apply(level);
        var block = session.trial.spawn();
        Vec3 at = Vec3.atBottomCenterOf(block);
        if (mob == null || !inside(at.x, at.y, at.z) || !level.getFluidState(block).isEmpty()
                || !level.getBlockState(block.below()).isFaceSturdy(level, block.below(), Direction.UP)
                || !level.noCollision(mob, mob.getDimensions(mob.getPose()).makeBoundingBox(at))) {
            error(level, "unsafe target spawn " + block.toShortString());
            return;
        }
        mob.setUUID(session.target);
        mob.moveTo(at.x, at.y, at.z, 180, 0);
        mob.setNoAi(true);
        mob.setInvulnerable(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        mob.skipDropExperience();
        mob.addTag(TARGET_TAG);
        mob.addTag(ownershipTag());
        if (!level.addFreshEntity(mob)) {
            error(level, "target spawn rejected");
            return;
        }
        phase(level, Phase.OPEN, EMPTY_TIMEOUT);
        if (!persist(level)) return;
        if (!PantheonArena.openCombatGate(level)) {
            error(level, "could not open combat gate");
            return;
        }
        announce(level, "open");
    }

    private static void tickOpen(ServerLevel level, long now) {
        Mob mob = target(level);
        if (mob == null || !mob.isAlive() || !inside(mob)) {
            finish(level, false, "missing");
            return;
        }
        Set<UUID> entrants = new LinkedHashSet<>();
        for (ServerPlayer player : level.players()) {
            if (eligible(player) && inside(player)) entrants.add(player.getUUID());
        }
        if (!entrants.equals(session.members)) {
            session.members.clear();
            session.members.addAll(entrants);
            if (!persist(level)) return;
        }
        if (entrants.isEmpty()) {
            session.entryEnds = 0;
            if (now >= session.deadline) finish(level, false, "empty");
            return;
        }
        if (session.entryEnds == 0) {
            session.entryEnds = now + ENTRY_WINDOW;
            announce(level, "entry");
        }
        if (now < session.entryEnds) return;
        if (!PantheonArena.closeCombatGate(level)) {
            error(level, "could not close combat gate");
            return;
        }
        pendingHub.addAll(entrants);
        phase(level, Phase.FIGHT, 0);
        if (!persist(level)) return;
        enforcePlayers(level);
        mob.setInvulnerable(false);
        session.trial.activate().accept(mob);
        announce(level, "fight");
    }

    private static void tickFight(ServerLevel level) {
        enforcePlayers(level);
        if (session.phase != Phase.FIGHT) return;
        boolean alive = level.players().stream().anyMatch(PantheonCombat::fighter);
        if (!alive) finish(level, false, "party");
        else if (session.deathTick != Long.MIN_VALUE) {
            if (level.getGameTime() > session.deathTick) finish(level, true, "won");
        }
        else {
            Mob mob = target(level);
            if (mob == null || !mob.isAlive() || !inside(mob)) finish(level, false, "missing");
        }
    }

    private static void finish(ServerLevel level, boolean won, String reason) {
        session.reason = reason;
        phase(level, won ? Phase.WON : Phase.FAILED, 0);
        if (!persist(level)) return;
        lastResult = session.phase + " / " + reason;
        TearsOfInk.LOGGER.info("Pantheon combat {}: {}", session.id, lastResult);
        Mob mob = target(level);
        if (mob != null) {
            mob.setNoAi(true);
            mob.setInvulnerable(true);
            mob.setTarget(null);
        }
        if (won) session.trial.defeated().accept(session.id);
        announce(level, reason);
    }

    private static void beginCleanup(ServerLevel level) {
        phase(level, Phase.CLEANUP, TRANSITION_TIMEOUT);
        if (!persist(level)) return;
        tickCleanup(level, level.getGameTime());
    }

    private static void tickCleanup(ServerLevel level, long now) {
        enforcePlayers(level);
        if (!PantheonTemple.ready(level)) {
            if (now >= session.deadline) error(level, "temple readiness timeout");
            return;
        }
        if (!PantheonArena.closeCombatGate(level)) {
            error(level, "could not close combat gate");
            return;
        }
        if (!session.cleared) {
            // Unloaded owned entities are discarded by the load hook, even after the journal is cleared.
            var owned = StreamSupport.stream(level.getAllEntities().spliterator(), false)
                    .filter(entity -> entity.getTags().contains(ownershipTag())).toList();
            for (Entity entity : owned) {
                if (entity.getUUID().equals(session.target) && entity instanceof Mob mob && session.trial != null)
                    session.trial.cleanup().accept(mob);
                else entity.discard();
            }
            session.cleared = true;
        }
        if (PantheonArena.preparing()) {
            if (now >= session.deadline) error(level, "scene cleanup timeout");
            return;
        }
        if (PantheonArena.neutralReady()) {
            Session completed = session;
            session = null;
            if (!persist(level)) {
                session = completed;
                session.phase = Phase.ERROR;
                session.reason = recoveryError;
                return;
            }
            if (!PantheonArena.openCombatGate(level)) {
                session = completed;
                error(level, "could not reopen combat gate");
                return;
            }
            PantheonArena.retainCombatArea(level, false);
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (pendingHub.contains(player.getUUID())) onArrival(player);
            }
            announce(level, "restored");
            return;
        }
        if (session.restoring) {
            error(level, PantheonArena.failure() != null ? PantheonArena.failure() : "neutral scene not ready");
            return;
        }
        String problem = PantheonArena.changeScene(level, NEUTRAL);
        if (problem != null) error(level, problem);
        else session.restoring = true;
    }

    private static void enforcePlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) enforcePlayer(player);
    }

    private static void enforcePlayer(ServerPlayer player) {
        if (!active() || !player.isAlive()) return;
        if (registeredFighter(player)) {
            if (!eligible(player) || !inside(player)) {
                forfeit(player);
                PantheonArena.sendToHub(player);
            }
            return;
        }
        if (session != null && session.phase == Phase.OPEN && eligible(player)) return;
        if (player.getZ() >= (session != null && session.phase == Phase.FIGHT ? HUB_LIMIT_Z : PASSAGE_Z))
            PantheonArena.sendToHub(player);
    }

    private static void onArrival(ServerPlayer player) {
        if (!player.isAlive()) return;
        UUID id = player.getUUID();
        if (PantheonTemple.isTemple(player.level())) {
            boolean eliminated = session != null && session.eliminated.contains(id);
            if (pendingHub.contains(id) || eliminated) PantheonArena.sendToHub(player);
            if (eliminated && session.phase == Phase.FIGHT)
                player.sendSystemMessage(Component.translatable(LANG + "observer"));
            enforcePlayer(player);
        }
        if (session == null && recoveryError == null && pendingHub.remove(id)) {
            ((PantheonPlayerListAccessor) player.server.getPlayerList()).tearsofink$savePantheonPlayer(player);
            pendingDirty = true;
        }
    }

    public static void playerDied(ServerPlayer player) { forfeit(player); }

    private static void forfeit(ServerPlayer player) {
        if (!registeredFighter(player)) return;
        session.eliminated.add(player.getUUID());
        ServerLevel level = player.server.getLevel(PantheonTemple.DIMENSION);
        if (level != null && persist(level) && level.players().stream().noneMatch(PantheonCombat::fighter))
            finish(level, false, "party");
    }

    private static boolean registeredFighter(ServerPlayer player) {
        return session != null && session.phase == Phase.FIGHT && session.members.contains(player.getUUID())
                && !session.eliminated.contains(player.getUUID());
    }

    private static boolean fighter(ServerPlayer player) {
        return registeredFighter(player) && PantheonTemple.isTemple(player.level()) && eligible(player) && inside(player);
    }

    private static boolean eligible(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return player.isAlive() && !player.hasDisconnected() && (mode == GameType.SURVIVAL || mode == GameType.ADVENTURE);
    }

    private static boolean inside(Entity entity) { return inside(entity.getX(), entity.getY(), entity.getZ()); }

    private static boolean inside(double x, double y, double z) {
        return x >= -46 && x < 48 && y >= 80 && y < 102 && z >= PASSAGE_Z && z < 133;
    }

    private static String ownershipTag() { return ENTITY_TAG + session.id; }

    private static Mob target(ServerLevel level) {
        return level.getEntity(session.target) instanceof Mob mob && mob.getTags().contains(ownershipTag()) ? mob : null;
    }

    public static boolean isTrialTarget(Entity entity) {
        return PantheonTemple.isTemple(entity.level()) && entity.getTags().contains(TARGET_TAG);
    }

    private static Entity owner(Entity entity) {
        if (entity instanceof TraceableEntity traced) return traced.getOwner();
        if (entity instanceof OwnableEntity owned) return owned.getOwner();
        return null;
    }

    private static boolean combatant(Entity entity) {
        if (entity instanceof ServerPlayer player) return fighter(player);
        return entity != null && session != null && session.phase == Phase.FIGHT
                && session.target.equals(entity.getUUID()) && isTrialTarget(entity) && inside(entity);
    }

    private static void entityLoaded(Entity entity, ServerLevel level) {
        if (!PantheonTemple.isTemple(level) || entity instanceof ServerPlayer) return;
        if (entity.getTags().stream().anyMatch(tag -> tag.startsWith(ENTITY_TAG))) {
            if (session == null || !entity.getTags().contains(ownershipTag())
                    || session.phase == Phase.CLEANUP || session.phase == Phase.ERROR) entity.discard();
        } else if (session != null && combatant(owner(entity))) {
            entity.addTag(ownershipTag());
        }
    }

    public static boolean mayTick(Entity entity) {
        if (!active() || !PantheonTemple.isTemple(entity.level())) return true;
        if (isTrialTarget(entity)) return session != null
                && (session.phase == Phase.OPEN || session.phase == Phase.FIGHT);
        if (!(entity instanceof TraceableEntity) && !(entity instanceof OwnableEntity)) return true;
        Entity owner = owner(entity);
        if (entity.getTags().stream().anyMatch(tag -> tag.startsWith(ENTITY_TAG))
                && !combatant(owner)) {
            entity.discard();
            return false;
        }
        if (entity instanceof TraceableEntity && entity.getZ() >= PASSAGE_Z && !combatant(owner)
                && session != null && session.phase == Phase.FIGHT) {
            entity.discard();
            return false;
        }
        return true;
    }

    private static boolean allowDamage(LivingEntity recipient, DamageSource damage) {
        if (damage.is(DamageTypes.GENERIC_KILL))
            return !isTrialTarget(recipient) || session != null && session.phase == Phase.FIGHT;
        Entity source = damage.getEntity();
        if (source == null) source = damage.getDirectEntity();
        if (recipient instanceof ServerPlayer victim && source instanceof ServerPlayer attacker
                && victim != attacker && fighter(victim) && fighter(attacker)) return false;
        return permitsInteraction(recipient, source);
    }

    /** Guard damage, sourced status effects, projectiles, targeting and collisions at their endpoints. */
    public static boolean permitsInteraction(Entity recipient, Entity source) {
        if (!active() || !PantheonTemple.isTemple(recipient.level())) return true;
        if ((isTrialTarget(recipient) || source != null && isTrialTarget(source))
                && (session == null || session.phase != Phase.FIGHT)) return false;
        if (session == null || session.phase != Phase.FIGHT) return true;
        if (source == null) return !isTrialTarget(recipient);
        Entity actor = owner(source);
        if (actor == null) actor = source;
        return combatant(recipient) == combatant(actor);
    }

    private static void phase(ServerLevel level, Phase phase, int timeout) {
        session.phase = phase;
        session.deadline = level.getGameTime() + timeout;
    }

    private static void announce(ServerLevel level, String message) {
        for (ServerPlayer player : level.players()) player.sendSystemMessage(Component.translatable(LANG + message));
    }

    private static void error(ServerLevel level, String reason) {
        session.phase = Phase.ERROR;
        session.reason = reason;
        if (PantheonTemple.ready(level)) PantheonArena.closeCombatGate(level);
        PantheonArena.retainCombatArea(level, false);
        enforcePlayers(level);
        for (ServerPlayer player : level.players())
            player.sendSystemMessage(Component.translatable(LANG + "error", reason));
        TearsOfInk.LOGGER.error("Pantheon combat {} closed: {}", session.id, reason);
        persist(level);
    }

    private static int abort(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(PantheonTemple.DIMENSION);
        if (level == null || session == null || session.phase == Phase.CLEANUP
                || session.phase == Phase.WON || session.phase == Phase.FAILED) {
            source.sendFailure(Component.translatable(LANG + "nothing_to_abort"));
            return 0;
        }
        if (session.phase == Phase.ERROR) {
            session.restoring = false;
            session.cleared = false;
            beginCleanup(level);
        } else finish(level, false, "cancelled");
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String state = recoveryError != null ? "ERROR / " + recoveryError : "IDLE";
        if (session != null) state = session.phase + " / " + session.id + " / trial "
                + (session.trial == null ? "?" : session.trial.id()) + " / target " + session.target
                + " / members " + session.members.size() + " " + session.members
                + " / eliminated " + session.eliminated.size() + " " + session.eliminated
                + (session.reason.isEmpty() ? "" : " / " + session.reason);
        String text = state + " / scene " + PantheonArena.sceneStatus()
                + (lastResult == null ? "" : " / last " + lastResult);
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    private static void readIds(CompoundTag data, String key, Set<UUID> result) {
        for (Tag tag : data.getList(key, Tag.TAG_STRING)) result.add(UUID.fromString(tag.getAsString()));
    }

    private static boolean persist(ServerLevel level) {
        try {
            if (session == null && pendingHub.isEmpty()) PantheonCombatJournal.clear(level.getServer());
            else {
                CompoundTag data = new CompoundTag();
                data.put("ReturnToHub", writeIds(pendingHub));
                if (session != null) {
                    CompoundTag record = new CompoundTag();
                    record.putUUID("Id", session.id);
                    record.putUUID("Initiator", session.initiator);
                    record.putUUID("Target", session.target);
                    if (session.trial != null) record.putString("Trial", session.trial.id().toString());
                    record.putString("Phase", session.phase.name());
                    record.put("Members", writeIds(session.members));
                    record.put("Eliminated", writeIds(session.eliminated));
                    record.putString("Reason", session.reason);
                    data.put("Session", record);
                }
                PantheonCombatJournal.save(level.getServer(), data);
            }
            pendingDirty = false;
            recoveryError = null;
            return true;
        } catch (IOException ex) {
            recoveryError = ex.toString();
            if (session != null) {
                session.phase = Phase.ERROR;
                session.reason = recoveryError;
                if (PantheonTemple.ready(level)) PantheonArena.closeCombatGate(level);
            }
            TearsOfInk.LOGGER.error("Cannot persist Pantheon combat recovery", ex);
            announce(level, "save_failed");
            return false;
        }
    }

    private static void recover(MinecraftServer server) {
        try {
            CompoundTag saved = PantheonCombatJournal.read(server);
            if (saved == null) return;
            readIds(saved, "ReturnToHub", pendingHub);
            if (!saved.contains("Session", Tag.TAG_COMPOUND)) return;
            CompoundTag record = saved.getCompound("Session");
            ResourceLocation key = ResourceLocation.tryParse(record.getString("Trial"));
            session = new Session(record.getUUID("Id"), record.getUUID("Initiator"),
                    key == null ? null : PantheonTrial.lookup(key), record.getUUID("Target"));
            readIds(record, "Members", session.members);
            readIds(record, "Eliminated", session.eliminated);
            pendingHub.addAll(session.members);
            session.phase = Phase.CLEANUP;
            ServerLevel level = server.getLevel(PantheonTemple.DIMENSION);
            session.deadline = (level == null ? 0 : level.getGameTime()) + TRANSITION_TIMEOUT;
            session.reason = "restart";
            lastResult = "FAILED / restart";
        } catch (Exception ex) {
            session = null;
            recoveryError = ex.toString();
            TearsOfInk.LOGGER.error("Cannot read the Pantheon combat recovery journal", ex);
        }
    }
}
