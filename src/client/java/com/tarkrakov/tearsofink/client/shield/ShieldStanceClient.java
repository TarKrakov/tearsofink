package com.tarkrakov.tearsofink.client.shield;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tarkrakov.tearsofink.TearsOfInk;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.shield.ShieldStance;
import com.tarkrakov.tearsofink.weapon.shield.ShieldTuning;
import com.tarkrakov.tearsofink.world.ModDimensions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Стойка двух щитов у игроков (клиент): поза рук от третьего лица (assets/tears-of-ink/shield_stance.json, F3+T),
 * оба щита — в позе блока, свечение щитов в темноте и свет в тумане (растут с зарядом).
 */
public final class ShieldStanceClient {
	/** id сущности → заряд щитов. */
	private static final Map<Integer, Integer> CHARGE = new HashMap<>();
	/** Поза рук в стойке (градусы, для правой руки; левая — зеркально): front — рука, что держит блок, side — вторая, сбоку. */
	private static Vector3f front = new Vector3f(-55, -25, 0), side = new Vector3f(-25, 30, 55);
	private static float look = 0.5f;

	private ShieldStanceClient() {}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(ShieldStance.Glow.TYPE, (payload, ctx) -> ctx.client().execute(() -> {
			if (payload.charge() <= 0) CHARGE.remove(payload.entity());
			else CHARGE.put(payload.entity(), payload.charge());
		}));
		ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> CHARGE.clear());
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public ResourceLocation getFabricId() {
				return TearsOfInk.id("shield_stance");
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				var res = manager.getResource(TearsOfInk.id("shield_stance.json"));
				if (res.isEmpty()) return;
				try (Reader r = res.get().openAsReader()) {
					JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
					if (o.has("front")) front = vec(o.getAsJsonArray("front"));
					if (o.has("side")) side = vec(o.getAsJsonArray("side"));
					if (o.has("look")) look = o.get("look").getAsFloat();
				} catch (Exception e) {
					TearsOfInk.LOGGER.error("Поза стойки щитов не прочиталась: {}", e.toString());
				}
			}
		});
	}

	private static Vector3f vec(JsonArray a) {
		return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
	}

	/** Щиты Души в обеих руках, в Землях. */
	public static boolean dual(LivingEntity e) {
		return e != null && ModDimensions.isLands(e.level()) && ShieldStance.dual(e);
	}

	/** В сомкнутой стойке (оба щита подняты). */
	public static boolean stance(LivingEntity e) {
		return dual(e) && e.isUsingItem() && e.getUseItem().getItem() instanceof SoulShieldItem
				&& !(e instanceof Player p && p.getCooldowns().isOnCooldown(e.getUseItem().getItem()));
	}

	public static int charge(LivingEntity e) {
		return CHARGE.getOrDefault(e.getId(), 0);
	}

	/** Модель «в блоке» для щита: этот щит используют — или стойка двух щитов (оба подняты). */
	public static boolean blocking(ItemStack stack, LivingEntity e) {
		if (e == null || !e.isUsingItem()) return false;
		if (e.getUseItem() == stack) return true;
		return stance(e) && (e.getMainHandItem() == stack || e.getOffhandItem() == stack);
	}

	/** Поза рук от третьего лица (после обычной позы). */
	public static void pose(HumanoidModel<?> model, LivingEntity e) {
		if (!stance(e)) return;
		HumanoidArm using = e.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? e.getMainArm() : e.getMainArm().getOpposite();
		set(model, using, front, model.head.xRot * look);
		set(model, using.getOpposite(), side, model.head.xRot * look * 0.5f);
	}

	private static void set(HumanoidModel<?> model, HumanoidArm arm, Vector3f deg, float pitch) {
		float m = arm == HumanoidArm.RIGHT ? 1 : -1;
		ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
		part.xRot = deg.x * Mth.DEG_TO_RAD + pitch;
		part.yRot = m * deg.y * Mth.DEG_TO_RAD;
		part.zRot = m * deg.z * Mth.DEG_TO_RAD;
	}

	/** Свечение щита в руке: щиты Души парой светятся в темноте, ярче с зарядом. */
	public static int light(LivingEntity e, ItemStack stack, int light) {
		if (!(stack.getItem() instanceof SoulShieldItem) || !dual(e)) return light;
		int glow = Math.min(15, ShieldTuning.GLOW + ShieldTuning.GLOW_PER_CHARGE * charge(e));
		int block = Math.max(LightTexture.block(light), glow);
		return LightTexture.pack(block, LightTexture.sky(light));
	}

	/** Видимость в тумане с двумя щитами (как ручной фонарь). 0 — щитов нет. */
	public static float clarity(Player p) {
		if (!dual(p)) return 0;
		return ShieldTuning.CLARITY + ShieldTuning.CLARITY_PER_CHARGE * charge(p);
	}
}
