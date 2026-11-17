package com.tarkrakov.tearsofink.weapon.forge;

import com.tarkrakov.tearsofink.block.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Окно Проклятого алтаря: оружие + реагент (сгустки крови / чернила) + усилитель (Кристалл крови / Чернильный отголосок).
 * Кнопки: 0–2 — выбрать слот, 3 — пробудить (и перебросить), 4 — оставить выпавшее, 5 — усилить свойство слота на уровень.
 * Выпавшее не записано, пока не нажато «Оставить»; ушёл — старое остаётся, ресурсы потрачены.
 */
public class CursedAltarMenu extends AbstractContainerMenu {
	public static final int BTN_AWAKEN = 3, BTN_KEEP = 4, BTN_EMPOWER = 5;
	/** Выпавшее: -2 — ничего не бросали, -1 — «ничего не пробудилось», 0+ — номер свойства. */
	public static final int D_SLOT = 0, D_PENDING = 1, D_LEVEL = 2, D_COUNT = 3;
	private static final int WEAPON = 0, REAGENT = 1, CATALYST = 2, INV_START = 3, INV_END = 39;

	private final SimpleContainer items = new SimpleContainer(3);
	private final ContainerData data;
	private final ContainerLevelAccess access;
	private ItemStack pendingFor = ItemStack.EMPTY;

	public CursedAltarMenu(int id, Inventory inventory) {
		this(id, inventory, ContainerLevelAccess.NULL);
	}

	public CursedAltarMenu(int id, Inventory inventory, ContainerLevelAccess access) {
		super(ModForge.ALTAR_MENU, id);
		this.access = access;
		this.data = new SimpleContainerData(D_COUNT);
		data.set(D_PENDING, -2);
		addDataSlots(data);
		addSlot(new Slot(items, WEAPON, 12, 20) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Gear.ours(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});
		addSlot(new Slot(items, REAGENT, 12, 44) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Awakening.reagentIsBlood(stack) != null;
			}
		});
		addSlot(new Slot(items, CATALYST, 12, 68) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return Awakening.catalystIsBlood(stack) != null;
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 162));
		}
	}

	// ===== Для окна =====
	public ItemStack weapon() {
		return items.getItem(WEAPON);
	}

	public int selected() {
		return data.get(D_SLOT);
	}

	public int pending() {
		return data.get(D_PENDING);
	}

	public int pendingLevel() {
		return data.get(D_LEVEL);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer sp)) return false;
		ItemStack weapon = items.getItem(WEAPON);
		if (pendingFor != weapon) clearPending(); // оружие сменили — выпавшее сгорело
		if (id >= 0 && id < 3) {
			if (weapon.isEmpty() || id >= Awakening.slots(weapon)) return false;
			if (id != data.get(D_SLOT)) clearPending();
			data.set(D_SLOT, id);
			return true;
		}
		if (weapon.isEmpty() || data.get(D_SLOT) >= Awakening.slots(weapon)) return false;
		switch (id) {
			case BTN_AWAKEN -> {
				ItemStack reagent = items.getItem(REAGENT);
				Boolean blood = Awakening.reagentIsBlood(reagent);
				int cost = Awakening.cost(weapon);
				if (blood == null || reagent.getCount() < cost) return false;
				reagent.shrink(cost);
				Awakening.addRoll(weapon);
				var r = sp.getRandom();
				if (r.nextFloat() < Awakening.NOTHING_CHANCE) {
					data.set(D_PENDING, -1);
					data.set(D_LEVEL, 0);
				} else {
					data.set(D_PENDING, Awakening.random(blood, weapon, r).ordinal());
					data.set(D_LEVEL, r.nextFloat() < Awakening.LEVEL_TWO_CHANCE ? 2 : 1);
				}
				pendingFor = weapon;
				sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f);
			}
			case BTN_KEEP -> {
				if (data.get(D_PENDING) < 0) return false;
				Awakening candidate = Awakening.values()[data.get(D_PENDING)];
				if (!Awakening.allowedOn(weapon, candidate)) return false;
				Awakening.set(weapon, data.get(D_SLOT),
						new Awakening.Slot(candidate, data.get(D_LEVEL)));
				clearPending();
				pendingFor = weapon;
				sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f);
			}
			case BTN_EMPOWER -> {
				Awakening.Slot s = Awakening.get(weapon, data.get(D_SLOT));
				ItemStack cat = items.getItem(CATALYST);
				Boolean blood = Awakening.catalystIsBlood(cat);
				if (s == null || !Awakening.allowedOn(weapon, s.awakening())
						|| blood == null || blood != s.awakening().blood || s.level() >= Awakening.MAX_LEVEL) return false;
				cat.shrink(1);
				Awakening.set(weapon, data.get(D_SLOT), new Awakening.Slot(s.awakening(), s.level() + 1));
				sound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.3f);
			}
			default -> {
				return false;
			}
		}
		items.setChanged();
		broadcastChanges();
		return true;
	}

	private void clearPending() {
		data.set(D_PENDING, -2);
		data.set(D_LEVEL, 0);
	}

	private void sound(net.minecraft.sounds.SoundEvent e, float pitch) {
		access.execute((level, pos) -> level.playSound(null, pos, e, SoundSource.BLOCKS, 0.8f, pitch));
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		access.execute((level, pos) -> clearContainer(player, items));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.CURSED_ALTAR);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < INV_START) {
			if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
		} else if (Gear.ours(stack)) {
			if (!moveItemStackTo(stack, WEAPON, WEAPON + 1, false)) return ItemStack.EMPTY;
		} else if (Awakening.reagentIsBlood(stack) != null) {
			if (!moveItemStackTo(stack, REAGENT, REAGENT + 1, false)) return ItemStack.EMPTY;
		} else if (Awakening.catalystIsBlood(stack) != null) {
			if (!moveItemStackTo(stack, CATALYST, CATALYST + 1, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}
}
