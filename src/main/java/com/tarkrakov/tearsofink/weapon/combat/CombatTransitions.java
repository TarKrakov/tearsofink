package com.tarkrakov.tearsofink.weapon.combat;

import com.tarkrakov.tearsofink.skill.ModSkills;
import com.tarkrakov.tearsofink.weapon.SoulShieldItem;
import com.tarkrakov.tearsofink.weapon.WeaponAbilities;
import com.tarkrakov.tearsofink.weapon.blood.Parry;
import com.tarkrakov.tearsofink.weapon.shield.Resonance;
import net.minecraft.server.level.ServerPlayer;

/** A new allowed technique replaces unfinished physical actions, not their paid costs or cooldowns.
 * Already emitted projectiles and persistent support effects remain independent. */
public final class CombatTransitions {
    private CombatTransitions() {}
    public static void begin(ServerPlayer p, boolean keepShield) {
        PaleGreatswordMoves.cancelForAction(p);
        PaleLongswordMoves.cancelForAction(p);
        Parry.cancelForAction(p);
        Combat.cancelForAction(p);
        HiltStrike.cancelForAction(p);
        boolean moving = AxeMoves.cancelWindup(p);
        moving |= DaggerMoves.cancelWindup(p);
        moving |= WeaponAbilities.cancelWindup(p);
        moving |= Resonance.cancelWindup(p);
        moving |= ModSkills.DASH.cancelForAction(p);
        moving |= ModSkills.STRIDE.cancelForAction(p);
        if (p.isUsingItem() && !(keepShield && p.getUseItem().getItem() instanceof SoulShieldItem))
            p.stopUsingItem();
        if (moving) {
            p.setDeltaMovement(0,p.getDeltaMovement().y,0);
            p.hurtMarked = true;
        }
    }
}
