import com.tarkrakov.tearsofink.world.EquipmentAffinity;

public class EquipmentAffinityTest {
    private static void eq(double a, double b) {
        if (Math.abs(a-b)>1e-8) throw new AssertionError(a+" != "+b);
    }
    private static double received(double damage, double armor, double toughness) {
        double effective = Math.min(20, Math.max(armor/5, armor-damage/(2+toughness/4)));
        return damage*(1-effective/25);
    }
    public static void main(String[] args) {
        eq(EquipmentAffinity.multiplier(true,true),1);
        eq(EquipmentAffinity.multiplier(false,false),1);
        eq(EquipmentAffinity.multiplier(true,false),.4);
        eq(EquipmentAffinity.multiplier(false,true),.6);
        for(boolean lands : new boolean[]{true,false}) {
            double own=EquipmentAffinity.multiplier(lands,true), foreign=EquipmentAffinity.multiplier(lands,false);
            eq(8*own+12*foreign,lands?12.8:16.8);
            eq(10*own,lands?10:6);
            eq(10*foreign,lands?4:10);
        }
        eq(received(6,9,1),4.48);
        eq(received(6,11,2),3.936);
        eq(received(6,13,3),3.4036363636363633);
        eq(received(8,13*.6,3*.6),8*(1-(7.8-8/2.45)/25));
        eq(received(6,20*.4,8*.4),6*(1-(8-6/2.8)/25));
        eq(received(12,20*.4,12*.4),12*(1-(8-12/3.2)/25));
        if (received(6,9,1) >= received(6,8,4.8)) throw new AssertionError("external netherite outprotects local light at 6 HP");
        System.out.println("EquipmentAffinity: four directions, mixed armor, damage and current armor math OK");
    }
}
