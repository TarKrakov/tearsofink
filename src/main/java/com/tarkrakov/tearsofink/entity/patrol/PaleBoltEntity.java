package com.tarkrakov.tearsofink.entity.patrol;

import com.tarkrakov.tearsofink.entity.ModMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Арбалетный болт бледного патруля. Своих не задевает, подобрать нельзя. Вид — заглушка (свой болт нарисует автор). */
public class PaleBoltEntity extends AbstractArrow {
	public PaleBoltEntity(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	public PaleBoltEntity(Level level, LivingEntity owner) {
		super(ModMobs.PALE_BOLT, owner, level, new ItemStack(Items.ARROW), null);
		this.pickup = Pickup.DISALLOWED;
		// урон стрелы = скорость × базовый урон → подгоняем под нужный урон болта
		this.setBaseDamage(SquadTuning.BOLT_DAMAGE / SquadTuning.BOLT_SPEED);
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		if (this.getOwner() != null && com.tarkrakov.tearsofink.entity.faction.Factions.allied(this.getOwner(), target)) return false;
		return super.canHitEntity(target) && !(target instanceof PaleSoldier);
	}

	@Override
	protected void onHitEntity(net.minecraft.world.phys.EntityHitResult hit) {
		if (hit.getEntity() instanceof LivingEntity e && !(e instanceof net.minecraft.world.entity.player.Player)) {
			this.setBaseDamage(SquadTuning.BOLT_DAMAGE * SquadTuning.VS_MOBS_DAMAGE / SquadTuning.BOLT_SPEED); // по мобам сильнее
		}
		super.onHitEntity(hit);
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return new ItemStack(Items.ARROW);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.inGround && this.inGroundTime > 100) this.discard(); // воткнувшиеся болты не копятся
	}
}
