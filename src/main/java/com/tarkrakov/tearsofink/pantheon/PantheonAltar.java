package com.tarkrakov.tearsofink.pantheon;

import com.tarkrakov.tearsofink.block.ModBlocks;
import com.tarkrakov.tearsofink.TearsOfInk;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import com.tarkrakov.tearsofink.mixin.PantheonCompoundContainerAccessor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.storage.LevelResource;
import com.tarkrakov.tearsofink.mixin.PantheonPlayerListAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Consumer;

/** The existing altar and filtered reliquary share the registered trial relics. */
public final class PantheonAltar {
    static final BlockPos POS = PantheonTemple.world(new BlockPos(62, 1, 11));
    private static final BlockPos CHEST_A = PantheonTemple.world(new BlockPos(62, 1, 13));
    private static final BlockPos CHEST_B = PantheonTemple.world(new BlockPos(62, 1, 14));
    private static final AttachmentType<ResourceLocation> RELIC_ID = AttachmentRegistry.<ResourceLocation>builder()
            .persistent(ResourceLocation.CODEC).buildAndRegister(TearsOfInk.id("pantheon_altar_relic"));

    public static final MenuType<PantheonAltarMenu> MENU = Registry.register(BuiltInRegistries.MENU,
            TearsOfInk.id("pantheon_altar"), new MenuType<>(PantheonAltarMenu::new, FeatureFlags.VANILLA_SET));

    private static final String RECEIPT = "TearsOfInkAltarReceipt";
    private static Transfer transfer;
    private static boolean writingReceipt;

    private record Transfer(UUID id, UUID player, UUID session, boolean insert, boolean host, PantheonTrial relic) {
        CompoundTag encode() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            tag.putUUID("Player", player);
            if (session != null) tag.putUUID("Session", session);
            tag.putBoolean("Insert", insert);
            tag.putBoolean("Host", host);
            tag.putString("Relic", relic.id().toString());
            return tag;
        }
    }

    private PantheonAltar() {}

    public static boolean isRelic(ItemStack stack) {
        return PantheonTrial.forRelic(stack) != null;
    }
    public static boolean isRelicChest(Container container) {
        if (container instanceof ChestBlockEntity chest)
            return chest.getLevel() != null && PantheonTemple.isTemple(chest.getLevel())
                    && (CHEST_A.equals(chest.getBlockPos()) || CHEST_B.equals(chest.getBlockPos()));
        if (container instanceof CompoundContainer doubleChest) {
            PantheonCompoundContainerAccessor parts = (PantheonCompoundContainerAccessor) doubleChest;
            return isRelicChest(parts.tearsofink$first()) || isRelicChest(parts.tearsofink$second());
        }
        return false;
    }

    static boolean occupied(ServerLevel level) {
        var state = level.getBlockState(POS);
        return state.is(ModBlocks.PANTHEON_ALTAR) && state.getValue(PantheonAltarBlock.RELIC);
    }

    private static ResourceLocation attached(ServerLevel level) {
        return level.getChunkAt(POS).getAttached(RELIC_ID);
    }

    /** A legacy occupied altar with no attachment still owns the starter relic. */
    static PantheonTrial placed(ServerLevel level) {
        if (!occupied(level)) return null;
        var id = attached(level);
        return id == null || PantheonTrial.legacyId(id) ? PantheonTrial.starter() : PantheonTrial.lookup(id);
    }

    static boolean matches(ServerLevel level, PantheonTrial trial) {
        PantheonTrial current = placed(level);
        return current != null && current.relic().get() == trial.relic().get();
    }

    private static boolean available(ServerLevel level) {
        return transfer == null && !PantheonEditor.active() && PantheonTemple.ready(level)
                && level.getBlockState(POS).is(ModBlocks.PANTHEON_ALTAR);
    }

    static boolean canInsert(ServerLevel level) {
        return available(level) && !PantheonCombat.sceneOwned() && PantheonArena.neutralReady()
                && !occupied(level) && attached(level) == null;
    }

    static boolean canExtract(ServerLevel level) {
        return available(level) && placed(level) != null && PantheonCombat.canExtract();
    }

    /** Transfer one item between the canonical chunk slot and the player's durable inventory/cursor. */
    static void transfer(ServerPlayer player, ServerLevel level, ItemStack stack, Consumer<ItemStack> set) {
        boolean insert = !stack.isEmpty();
        PantheonTrial relic = insert ? PantheonTrial.forRelic(stack) : placed(level);
        if (relic == null || (insert ? !canInsert(level) : !canExtract(level))) return;
        var intent = new Transfer(UUID.randomUUID(), player.getUUID(), PantheonCombat.sceneOwner(), insert,
                player.server.isSingleplayerOwner(player.getGameProfile()), relic);
        try {
            PantheonCombatJournal.write(path(level.getServer()), intent.encode());
        } catch (IOException ex) {
            TearsOfInk.LOGGER.error("Cannot prepare altar transfer", ex);
            return;
        }
        transfer = intent;
        ItemStack before = stack.copy();
        set.accept(insert ? stack.copyWithCount(stack.getCount() - 1) : relic.relicStack());
        writingReceipt = true;
        try {
            ((PantheonPlayerListAccessor) player.server.getPlayerList()).tearsofink$savePantheonPlayer(player);
            // IntegratedServer loads its host from level.dat ahead of the playerdata file.
            if (intent.host()) player.server.saveAllChunks(true, true, true);
            if (!receiptSaved(player.server, intent)) {
                writingReceipt = false;
                set.accept(before);
                clearTransfer(player.server);
                return;
            }
            if (!commit(level, intent)) PantheonCombat.altarConflict(level);
        } catch (Exception ex) {
            TearsOfInk.LOGGER.error("Altar transfer requires recovery", ex);
            PantheonCombat.altarConflict(level);
        }
    }

    public static void writeReceipt(ServerPlayer player, CompoundTag tag) {
        if (writingReceipt && transfer != null && transfer.player().equals(player.getUUID()))
            tag.putUUID(RECEIPT, transfer.id());
    }

    private static boolean receiptSaved(MinecraftServer server, Transfer intent) throws IOException {
        Path player = intent.host() ? server.getWorldPath(LevelResource.LEVEL_DATA_FILE)
                : server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(intent.player() + ".dat");
        if (Files.notExists(player)) {
            if (!intent.host()) return false;
            player = server.getWorldPath(LevelResource.OLD_LEVEL_DATA_FILE);
            if (Files.notExists(player)) return false;
        }
        var tag = NbtIo.readCompressed(player, NbtAccounter.unlimitedHeap());
        if (intent.host()) tag = tag.getCompound("Data").getCompound("Player");
        return tag.hasUUID(RECEIPT) && intent.id().equals(tag.getUUID(RECEIPT));
    }

    private static boolean commit(ServerLevel level, Transfer intent) throws IOException {
        if (!java.util.Objects.equals(intent.session(), PantheonCombat.sceneOwner())) return false;
        var state = level.getBlockState(POS);
        if (!state.is(ModBlocks.PANTHEON_ALTAR)) return false;
        boolean wasOccupied = state.getValue(PantheonAltarBlock.RELIC);
        ResourceLocation id = attached(level);
        if (wasOccupied && !matches(level, intent.relic()) || !wasOccupied && id != null) return false;
        if (wasOccupied != intent.insert() && !PantheonTemple.altarRelic(level, state, intent.insert())) return false;
        level.getChunkAt(POS).setAttached(RELIC_ID, intent.insert() ? intent.relic().id() : null);
        level.getChunkSource().save(true);
        if (!intent.insert() && !PantheonCombat.relicExtracted(level, intent.session())) return false;
        clearTransfer(level.getServer());
        return true;
    }

    static boolean hasTransfer() { return transfer != null; }

    static void loadTransfer(MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.read(path(server));
        if (tag == null) return;
        PantheonTrial relic = PantheonTrial.lookup(ResourceLocation.parse(tag.getString("Relic")));
        if (relic == null) throw new IOException("Unknown relic in pending altar transfer");
        transfer = new Transfer(tag.getUUID("Id"), tag.getUUID("Player"),
                tag.hasUUID("Session") ? tag.getUUID("Session") : null, tag.getBoolean("Insert"), tag.getBoolean("Host"), relic);
    }

    static boolean recoverTransfer(ServerLevel level) {
        if (transfer == null) return true;
        try {
            if (receiptSaved(level.getServer(), transfer)) return commit(level, transfer);
            if (occupied(level) == transfer.insert()) return false;
            clearTransfer(level.getServer());
            return true;
        } catch (IOException ex) {
            TearsOfInk.LOGGER.error("Cannot reconcile altar transfer", ex);
            return false;
        }
    }

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/tearsofink_pantheon_altar_v1.dat");
    }

    private static void clearTransfer(MinecraftServer server) throws IOException {
        Files.deleteIfExists(path(server));
        transfer = null;
        writingReceipt = false;
    }

    static void init() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            transfer = null;
            writingReceipt = false;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!PantheonTemple.isTemple(level) || !POS.equals(hit.getBlockPos())
                    || !level.getBlockState(POS).is(ModBlocks.PANTHEON_ALTAR)
                    || player.isSpectator()) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel temple)
                    || !sp.isAlive()) return InteractionResult.FAIL;
            sp.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                    new PantheonAltarMenu(id, inventory, temple),
                    Component.translatable("block.tears-of-ink.pantheon_altar")));
            return InteractionResult.CONSUME;
        });
    }
}
