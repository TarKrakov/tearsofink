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
        eq(EquipmentAffinity.multiplier(true,false),.6);
        eq(EquipmentAffinity.multiplier(false,true),.6);
        for(boolean lands : new boolean[]{true,false}) {
            double own=EquipmentAffinity.multiplier(lands,true), foreign=EquipmentAffinity.multiplier(lands,false);
            eq(8*own+12*foreign,lands?15.2:16.8);
            eq(10*own,lands?10:6);
            eq(10*foreign,lands?6:10);
        }
        eq(received(6,7,0),5.04);
        eq(received(6,11,2),3.936);
        eq(received(6,15,4),2.88);
        eq(received(8,15*.6,4*.6),8*(1-(9-8/2.6)/25));
        System.out.println("EquipmentAffinity: four directions, mixed armor, damage and current armor math OK");
    }
}
