package com.tarkrakov.tearsofink.prologue;

import com.mojang.serialization.Codec;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.item.ModItems;
import com.tarkrakov.tearsofink.particle.ModParticles;
import com.tarkrakov.tearsofink.travel.FountainTravel;
import com.tarkrakov.tearsofink.vow.Vow;
import com.tarkrakov.tearsofink.vow.Vows;
import com.tarkrakov.tearsofink.world.InkLandsLayout;
import com.tarkrakov.tearsofink.world.ModDimensions;
import com.tarkrakov.tearsofink.world.ruins.Ruins;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** First slice of the personal storyline: a short, on-demand bearing to another temple. */
public final class StoryKeyItem extends Item {
    private static final int INK_ISSUED = 1, SOUL_ISSUED = 2, INK_USED = 4, SOUL_USED = 8;
    private static final AttachmentType<Integer> HISTORY = AttachmentRegistry.<Integer>builder()
            .persistent(Codec.INT).copyOnDeath().initializer(() -> 0)
            .buildAndRegister(TearsOfInk.id("story_key_history"));
    private static final int TRAIL_POINTS = 20, SPARKS_PER_POINT = 3;
    private static final double TRAIL_REACH = 9.0;
    private final Vow path;

    public StoryKeyItem(Vow path, Properties properties) {
        super(properties);
        this.path = path;
    }

    public static void init() {}

    /** One grant per vow, also used to migrate characters whose starter gift predates these items. */
    static void grant(ServerPlayer player, Vow path) {
        if (path == Vow.NONE || Vows.vow(player) != path) return;
        int bit = path == Vow.INK ? INK_ISSUED : SOUL_ISSUED;
        int history = player.getAttachedOrCreate(HISTORY);
        if ((history & bit) != 0) return;
        Item key = path == Vow.INK ? ModItems.INK_TEAR : ModItems.SHARD_OF_HOPE;
        if (!player.getInventory().contains(new ItemStack(key))) PrologueItems.give(player, key);
        player.setAttached(HISTORY, history | bit);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ModDimensions.isLands(level)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer server) {
            if (server.isSpectator() || Vows.vow(server) != path || FountainTravel.openedCount(server) >= 2)
                return InteractionResultHolder.pass(stack);
            InkLandsLayout layout = InkLandsLayout.of(server.serverLevel());
            if (layout == null) return InteractionResultHolder.pass(stack);
            int[] target = null;
            double best = Double.POSITIVE_INFINITY;
            double[] start = Ruins.start(layout);
            // Stable order and distance from the starting temple, not from where the player clicks.
            // The existing planner is read only; never request generation or load a distant chunk.
            for (int[] temple : Ruins.templePoints(layout)) {
                if (Ruins.isStartTemple(layout, temple[0], temple[1])
                        || FountainTravel.hasOpenedNear(server, temple[0], temple[1], 26)) continue;
                double dx = temple[0] - start[0], dz = temple[1] - start[1];
                double distance = dx * dx + dz * dz;
                if (distance < best) { best = distance; target = temple; }
            }
            if (target == null) return InteractionResultHolder.pass(stack);
            int bit = path == Vow.INK ? INK_USED : SOUL_USED;
            int history = server.getAttachedOrCreate(HISTORY);
            if ((history & bit) == 0) {
                server.sendSystemMessage(Component.translatable(path == Vow.INK
                        ? "message.tears-of-ink.story_key.ink" : "message.tears-of-ink.story_key.soul")
                        .withStyle(path == Vow.INK ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD, ChatFormatting.ITALIC));
                server.setAttached(HISTORY, history | bit);
            }
            double dx = target[0] + 0.5 - server.getX(), dz = target[1] + 0.5 - server.getZ();
            double length = Math.hypot(dx, dz);
            if (length > 0.05) {
                ParticleOptions spark = path == Vow.INK ? ModParticles.STORY_INK_SPARK : ModParticles.STORY_SOUL_SPARK;
                double reach = Math.min(TRAIL_REACH, length);
                double y = server.getY() + 1.25;
                for (int i = 1; i <= TRAIL_POINTS; i++) {
                    double offset = reach * i / TRAIL_POINTS;
                    server.serverLevel().sendParticles(server, spark, false,
                            server.getX() + dx / length * offset, y + 0.15 * Math.sin(i * 0.6),
                            server.getZ() + dz / length * offset, SPARKS_PER_POINT, 0.12, 0.12, 0.12, 0.008);
                }
            }
            server.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
